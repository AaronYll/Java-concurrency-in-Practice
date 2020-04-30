import java.util.concurrent.locks.*;

/**
 * OneShotLatch
 * Binary Latch using AQS
 * 功能描述：起初闭锁关闭，任何调用await的线程都将阻塞并直到闭锁被打开.
 * 通过调用signal打开闭锁时，所有等待中的线程都将被释放，并且随后到达闭锁的线程也被允许执行。
 */
public class OneShotLatch{
    private final Sync sync = new Sync();

    public void signal(){
        sync.releaseShared(0);
    }

    public void await() throws InterruptedException{
        sync.acquireInterruptibly(0);   // 内部实现中有一块调用了tryAcquireShared
    }

    private class Sync extends AbstractQueuedSynchronizer{
        protected int tryAcquireShared(int ignore){
            // 重写
            // 非独占方式获取
            // 当闭锁开放时(state == 1)成功，否则失败
            return (getState() == 1)? 1: -1;
        }

        protected boolean tryReleaseShared(int ignored){
            setState(1);    // 将闭锁设置为打开 (功能逻辑上自定义了state == 1为闭锁开放的逻辑)
            return true;
        }
    }
}