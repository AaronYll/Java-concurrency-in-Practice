import java.util.concurrent.locks.*;

public class SemaphoreOnLock{
    private final Lock lock = new ReentrantLock();
    // 信号量的等待队列 (与lock对应)
    // CONDITION PREDICATE: permitsAvailable (permits > 0)
    private final Condition permitesAvailable = lock.newCondition();
    private int permits;            // 许可数

    public SemaphoreOnLock(int permits){
        lock.lock();
        try{
            this.permits = permits;
        } finally{
            lock.unlock();
        }
    }

    // 阻塞直到permits>0
    public void acquire(){
        lock.lock();
        try{
            while(permits<=0){
                permitesAvailable.await();
            }
        } finally{
            permits--;
            lock.unlock();
        }
    }

    public void release(){
        lock.lock();
        try{
            ++permits;          // 注意这个实现并不完美 若多次release会导致permits超出初始限定
            permitesAvailable.signal();
        } finally{
            lock.unlock();
        }
    }
}