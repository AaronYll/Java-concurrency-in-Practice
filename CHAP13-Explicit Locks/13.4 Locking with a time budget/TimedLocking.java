import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * 带有时间限制的加锁
 * 在Lock保护的共享通信线路上发一条消息，如果不能在指定时间内完成，代码就会失败
 */
public class TimedLocking{
    private Lock lock = new ReentrantLock();

    public boolean trySendOnSharedLine(String message, long timeout, TimeUnit unit)
    throws InterruptedException{
        long nanosToLock = unit.toNanos(timeout)-estimatedNanosToSend(message); // 由于传送消息也需时间，故最多等待锁timeout-estimateNanosTosend时间
        if(!lock.tryLock(nanosToLock, NANOSECONDS)){
            return false;
        }
        // 这里已经获得锁，进入临界区
        try{
            sendOnSharedLine(message);
        } finally{
            lock.unlock();      // 必须在finally中释放锁
        }
        return false;
    }

    private boolean sendOnSharedLine(String message){
        /* send something */
        return true;
    }

    private long estimatedNanosToSend(String message){
        return message.length();
    }

}