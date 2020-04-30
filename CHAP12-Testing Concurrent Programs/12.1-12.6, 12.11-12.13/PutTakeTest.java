import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import junit.framework.TestCase;

/**
 * 测试SemaphoreBoundedBuffer的生产者-消费者程序
 */
public class PutTakeTest extends TestCase{
    protected static final ExecutorService pool = Executors.newCachedThreadPool();
    protected final AtomicInteger putSum  = new AtomicInteger(0);
    protected final AtomicInteger takeSum = new AtomicInteger(0);
    protected CyclicBarrier barrier;
    // private final SemaphoreBoundedBuffer<Integer> bb;
    private final LinkedBlockingQueue<Integer> bb;
    /**
     * nTrials: 每个生产者/消费者取出/放入数字的个数
     * nPairs: 生产者/消费者对数
     */
    protected final int nTrials, nPairs;  


    PutTakeTest(int capacity, int npairs, int ntrials){
        nTrials = ntrials;
        nPairs  = npairs;
        // bb      = new SemaphoreBoundedBuffer<>(capacity);
        bb      = new LinkedBlockingQueue<>(capacity);
        /**
         * barrier: 循环栅栏，阻塞一组线程直到线程数达到2*npairs+1(主线程+2*npairs个消费者/生产者线程)
         */
        barrier = new CyclicBarrier(2*npairs+1);   // 注意等待栅栏为barrier.await()而不是wait()(执行wait()需要获取监视器，否则报监视器错)
    }

    static  int xorshift(int y){
        y ^= (y << 6);
        y ^= (y >>> 8);
        y ^= (y << 7);
        return y;
    } 

    class Producer implements Runnable{
        public void run(){
            try{
                int seed = this.hashCode()^(int)System.nanoTime();  // 每个Producer初始化随机生成一个随机数种子
                int sum  = 0;
                barrier.await();    // 前期准备完成，等待栅栏释放(凑够2*npairs+1)线程数
                for(int i=0; i<nTrials; i++){
                    bb.put(seed);
                    sum += seed;
                    seed = xorshift(seed);
                }
                putSum.addAndGet(sum);
                barrier.await();    // 计算完成，等待栅栏释放
            } catch(Exception e){
                throw new RuntimeException(e);
            } 
        }
    }

    class Consumer implements Runnable{
        public void run(){
            try{
                int sum = 0;
                barrier.await();    // 前期准备完成，等待栅栏释放(凑够2*npairs+1)线程数
                for(int i=0; i<nTrials; i++){
                    sum += bb.take();
                }
                takeSum.addAndGet(sum);
                barrier.await();    // 计算完成，等待栅栏释放
            } catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    public void test(){
        try{
            for(int i=0; i<nPairs; i++){
                pool.execute(new Producer());
                pool.execute(new Consumer());
            }
            barrier.await();    // 等待所有线程就绪
            barrier.await();    // 等待所有线程执行完成
            assertEquals(putSum.get()+1, takeSum.get());
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }
    public static void main(String... args){
        PutTakeTest t = new PutTakeTest(15, 10, 100);
        t.test();   // 正常执行
        pool.shutdown();
        // t.test();   // java.util.concurrent.RejectedExecutionException
    }
}