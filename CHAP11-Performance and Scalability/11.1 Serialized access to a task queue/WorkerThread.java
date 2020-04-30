import java.util.concurrent.BlockingQueue;

/**
 * WorkerThread
 * <p/>
 * 这个程序看起来可以完全并行：各个任务之间不会相互等待，因此处理器越多，能够并发处理的任务也就越多
 * 然而从队列中获取任务是串行的：所以工作者任务共享同一个工作队列，因此在进行并发访问时使用了某种并发机制
 * BlockingQueue是在类外发布的，在创建WorkerThread时传入
 */

public class WorkerThread extends Thread {
    private final BlockingQueue<Runnable> queue;

    public WorkerThread(BlockingQueue<Runnable> queue) {
        this.queue = queue;
    }

    public void run() {
        while (true) {
            try {
                Runnable task = queue.take();
                task.run();
            } catch (InterruptedException e) {
                break; /* Allow thread to exit */
            }
        }
    }
}