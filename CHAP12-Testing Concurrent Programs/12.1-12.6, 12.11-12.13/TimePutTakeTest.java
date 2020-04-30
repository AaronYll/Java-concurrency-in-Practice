import java.util.concurrent.*;

/**
 * 基于栅栏的定时器，在PutTakeTest中增加计时功能
 */
public class TimePutTakeTest extends PutTakeTest {
    private BarrierTimer timer = new BarrierTimer();

    public TimePutTakeTest(int capacity, int npairs, int ntrials) {
        super(capacity, npairs, ntrials);
        barrier = new CyclicBarrier(nPairs * 2 + 1, timer); // barrier的第二个线程参数用于放开栅栏后执行这个BarrierAction(计时器开/关)
    }

    /**
     * 基于栅栏的定时器进行测试
     */
    public void test() {
        try {
            timer.clear();
            for (int i = 0; i < nPairs; i++) {
                pool.execute(new Producer());
                pool.execute(new Consumer());
            }
            barrier.await();
            barrier.await();
            long nsPerItem = timer.getTime() / (nPairs * (long) nTrials);
            System.out.print("Througput: " + nsPerItem + "ns/item");
            assertEquals(putSum.get(), takeSum.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String... args) {
        // TimePutTakeTest t = new TimePutTakeTest(10, 15, 20);
        // t.test();
        int tpt = 100000; // 每个线程中的测试次数(put/take次数)
        /**
         * 设置不同的有界缓存容量大小
         */
        for (int cap = 10; cap <= 1000; cap *= 10) {
            System.out.println("Capacity: " + cap);
            for (int pair = 1; pair <= 128; pair *= 2) // 设置不同的生产者/消费者对数
            {
                TimePutTakeTest test = new TimePutTakeTest(cap, pair, tpt);
                System.out.println("Pairs: " + pair + "\t");
                test.test();
                System.out.println();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                test.test();
                System.out.println();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        pool.shutdown();
    }
}

/**
 * 栅栏计时器
 */
class BarrierTimer implements Runnable{
    private boolean started;
    private long startTime, endTime;

    public synchronized void run(){
        long t = System.nanoTime();
        if(!started){
            started = true;
            startTime = t;
        }
        else{
            endTime = t;
        }
    }

    public synchronized void clear(){
        started = false;
    }

    public synchronized long getTime(){
        return endTime-startTime;
    }
}