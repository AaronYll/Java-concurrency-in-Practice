import junit.framework.TestCase;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.ThreadFactory;

// 若继承TestCase则exec.shutdownNow()无法正常执行
public class TestThreadPool extends TestCase {
    private final TestingThreadFactory threadFactory = new TestingThreadFactory();

    public void testPoolExpansion() throws InterruptedException {
        int MAX_SIZE = 10;
        ExecutorService exec = Executors.newFixedThreadPool(MAX_SIZE);
        final AtomicInteger t = new AtomicInteger();

        for (int i = 0; i < 10 * MAX_SIZE; i++) {
            exec.execute(threadFactory.newThread(new Runnable() {
                public void run() {
                    try {
                        System.out.println(t.getAndIncrement());
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }));
        }
        for (int i = 0; i < 20 && threadFactory.numCreated.get() < MAX_SIZE; i++) {
            Thread.sleep(100);
        }
        assertEquals(threadFactory.numCreated.get(), MAX_SIZE);
        exec.shutdownNow();
        exec.awaitTermination(1000, TimeUnit.MILLISECONDS);
    }

    public static void main(String... args) throws InterruptedException {
        TestThreadPool testThreadPool = new TestThreadPool();
        testThreadPool.testPoolExpansion();
        // 执行结果为：
        // Exception in thread "main" junit.framework.AssertionFailedError:
        // expected:<100> but was:<10>
        // 主线程先创建100个test线程提交给exec后线程池由于线程池资源为10，一次执行10个线程
        // System.out.println(t.getAndIncrement())不是原子的 所以输出1-10不按序
    }
}

/**
 * 通过自定义线程工厂，可以对线程的创建过程进行控制
 */
class TestingThreadFactory implements ThreadFactory {
    // Implements Interface ThreadFactory must implement the inherited abstract
    // method ThreadFactory.newThread(Runnable)
    public final AtomicInteger numCreated = new AtomicInteger(); // 记录创建的线程数
    private final ThreadFactory factory = Executors.defaultThreadFactory(); // 委托defaultThreadFactory

    public Thread newThread(Runnable r) {
        numCreated.getAndIncrement();
        return factory.newThread(r);
    }
}