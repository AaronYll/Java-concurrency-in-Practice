import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

/**
 * 增加了日志和计时等功能的线程池
 */
public class TimingThreadPool extends ThreadPoolExecutor{

    public TimingThreadPool(){
        super(1, 1, 0L, TimeUnit.SECONDS, null);
    }

    private final ThreadLocal<Long> startTime = new ThreadLocal<>();    // ThreadLocal线程封闭, 因为ThreadPool中的多个线程都会用，为了防止发生竞态条件
    private final Logger log = Logger.getLogger("TimingThreadPool");
    private final AtomicLong numTasks = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();

    protected void beforeExecute(Thread t, Runnable r){
        super.beforeExecute(t, r);      // 先调用继承的默认的beforeExecute
        log.fine(String.format("Thread %s: start %s", t, r));   // 扩展的自定义部分，记录日志
        startTime.set(System.nanoTime());   // 扩展的自定义部分， 得到当前线程的系统时间，为计算后设置线程运行消耗的总时间做准备
    }

    protected void afterExecute(Runnable r, Throwable t){
        try{
            long endTime = System.nanoTime();
            long taskTime = endTime - startTime.get();
            numTasks.incrementAndGet();     // 自定义，增加统计信息
            totalTime.addAndGet(taskTime);      // 自定义， 设置线程运行消耗的总时间
            log.fine(String.format("Terminated: avg time=%dns", totalTime.get()/numTasks.get()));
        }   finally{
            super.terminated();     // 自定义后处理后调用继承的默认后处理
        }
    }
}