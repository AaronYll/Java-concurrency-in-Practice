import junit.framework.TestCase;

/**
 * 基本的测试单元， 类似于在串行上下文中执行的测试 首先创建一个有界缓存，然后调用它的各个方法，并验证它的后验条件和不变性条件 不变性条件： 1.
 * 新建立的缓存是空的 2. 将N个元素插入到容量为N的缓存中，然后测试缓存是否已经被填满。
 */
class BoundedBufferTest extends TestCase{
    private static final long LOCKUP_DETECT_TIMEOUT = 1000;


    void testIsEmptyWhenConstructed(){
        SemaphoreBoundedBuffer<Integer> bb = new SemaphoreBoundedBuffer<>(10);
        assertTrue(bb.isEmpty());
        assertFalse(bb.isFull());
    }

    void testIsFullAfterPuts() throws InterruptedException {
        SemaphoreBoundedBuffer<Integer> bb = new SemaphoreBoundedBuffer<>(10);
        for(int i=0; i<10; i++){
            bb.put(i);
        }
        assertTrue(bb.isFull());    // 顺利运行
        assertFalse(bb.isEmpty());  // 顺利运行
    }

    void testTakeBlocksWhenEmpty(){
        final SemaphoreBoundedBuffer<Integer> bb = new SemaphoreBoundedBuffer<>(10);
        Thread taker = new Thread(){
            public void run(){
                try{
                    // 测试了阻塞属性
                    int unused = bb.take();
                    fail();     // 对空的BoundedBuffer进行take, 若未阻塞运行到这里说明fail
                } catch (InterruptedException success){
                    // 测试了take在中断后能抛出InterruptedException
                    System.out.println("success");
                }
            }
        };
        try{
            taker.start();
            Thread.sleep(LOCKUP_DETECT_TIMEOUT);
            // 如果不进行interrupt taker.isAlive==True， 报junit.framework.AssertionFailedError
            // taker.interrupt();  
            // 如果take操作由于某种意外停滞了，那么支持限时的join方法能确保测试最终完成
            taker.join(LOCKUP_DETECT_TIMEOUT);  // 经过LOOKUP_DETECT_TIMEOUT时间后join过期
            assertFalse(taker.isAlive());
        } catch(Exception unexpected){
            fail();
        }
    }


    
    public static void main(String... args) throws InterruptedException {
        BoundedBufferTest t = new BoundedBufferTest();
        t.testIsEmptyWhenConstructed();
        t.testIsFullAfterPuts();
        t.testTakeBlocksWhenEmpty();
    }
}