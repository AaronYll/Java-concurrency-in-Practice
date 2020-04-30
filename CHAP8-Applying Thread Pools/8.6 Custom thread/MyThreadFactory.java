/**
 * 自定义的线程工厂
 */
public class MyThreadFactory implements ThreadFactory{
    private final String poolName;

    public MyThreadFactory(String poolName){
        this.poolName = poolName;
    }

    public Thread newThread(Runnable r){
        return new MyAppThread(r, poolName);
    }

    public static void main(String... args) throws InterruptedException {
        MyThreadFactory myThreadFactory = new MyThreadFactory("mypool");
        Thread myThread = myThreadFactory.newThread(
            ()->{
                System.out.println("aa");
            }
        );
        myThread.start();
        Thread.sleep(100);
        if(myThread instanceof MyAppThread){
            System.out.println(((MyAppThread)myThread).getThreadsCreated());
        }
    }
}