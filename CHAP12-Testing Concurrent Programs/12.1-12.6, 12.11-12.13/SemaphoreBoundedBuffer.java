import java.util.concurrent.*;

/**
 * BounderBuffer 基于信号量的有界缓存
 */
public class SemaphoreBoundedBuffer<E> {
    private final Semaphore availableItems, availableSpace; // 创建两个信号量，一个表示可取用的资源数量，一个表示可放入资源的空间
    private E[] buffer;
    private int putPosition = 0, takePosition = 0;

    public SemaphoreBoundedBuffer(int capacity) {
        buffer = (E[]) new Object[capacity];
        availableItems = new Semaphore(0);
        availableSpace = new Semaphore(capacity);
    }

    public boolean isEmpty() {
        return availableItems.availablePermits() == 0;
    }

    public boolean isFull() {
        return availableSpace.availablePermits() == 0;
    }

    public void put(E x) throws InterruptedException {
        availableSpace.acquire();
        doInsert(x);
        availableItems.release();
    }

    public synchronized void doInsert(E x) {
        int i = putPosition;
        buffer[i] = x;
        putPosition = (++i == buffer.length) ? 0 : i;
    }

    public E take() throws InterruptedException {
        availableItems.acquire();
        E item = doExtract();
        availableSpace.release();
        return item;
    }

    public synchronized E doExtract() {
        int i = takePosition;
        E item = buffer[i];
        buffer[i] = null; // 防止内存泄漏
        takePosition = (++i == buffer.length) ? 0 : i;
        return item;
    }
}



