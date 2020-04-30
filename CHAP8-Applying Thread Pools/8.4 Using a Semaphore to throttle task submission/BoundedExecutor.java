import java.util.concurrent.*;

/**
 * BoundedExecutor
 * <p/>
 * Using a Semaphore to throttle task submission
 *
 * @author Brian Goetz and Tim Peierls
 */
public class BoundedExecutor {
    private final Executor exec;
    private final Semaphore semaphore;

    public BoundedExecutor(Executor exec, int bound) {
        this.exec = exec;
        this.semaphore = new Semaphore(bound);
    }

    public void submitTask(final Runnable command) throws InterruptedException {
        semaphore.acquire();
        try {
            exec.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        command.run();
                    } finally {
                        semaphore.release();
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            System.out.println("rejection occure");
            semaphore.release();
        }
    }

    public static void main(String... args) {
        // 创建一个固定大小为1的线程池，有界队列大小为1，放入BoundedExecutor的构造函数中，信号量为5（2）
        BoundedExecutor boundedExecutor = new BoundedExecutor(
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1)), 5
            );

        // 创建5个线程，分别测试信号量为5和2, 当信号量为5时，由于阻塞当线程超过有界队列大小，所以触发饱和策略
        // 默认的饱和策略是“Abort”，该策略将抛出未检查的RejectedExecutionException
        // 示例中将丢失三个线程的执行
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                try {
                    boundedExecutor.submitTask(() -> {
                        System.out.println("this is t1");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
        });
        Thread t2 = new Thread(new Runnable() {
            public void run() {
                try {
                    boundedExecutor.submitTask(() -> {
                        System.out.println("this is t2");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
        });
        Thread t3 = new Thread(new Runnable() {
            public void run() {
                try {
                    boundedExecutor.submitTask(() -> {
                        System.out.println("this is t3");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
        });
        Thread t4 = new Thread(new Runnable() {
            public void run() {
                try {
                    boundedExecutor.submitTask(() -> {
                        System.out.println("this is t4");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
        });
        Thread t5 = new Thread(new Runnable() {
            public void run() {
                try {
                    boundedExecutor.submitTask(() -> {
                        System.out.println("this is t5");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
        });
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        System.out.println("主线程运行完毕");
    }
}