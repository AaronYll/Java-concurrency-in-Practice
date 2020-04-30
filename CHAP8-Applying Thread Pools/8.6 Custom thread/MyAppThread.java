import java.util.concurrent.atomic.*;
import java.util.logging.*;

/**
 * 定制Thread基类
 */
public class MyAppThread extends Thread{
    public static final String DEFAULT_NAME = "MyAppThread";    // 为线程指定名字
    private static volatile boolean debugLifecycle = false;     // debug标签用于选择是否log
    private static final AtomicInteger created = new AtomicInteger();   // 用来统计共开过多少个这样的线程, 每个线程新建时编号用
    private static final AtomicInteger alive   = new AtomicInteger();   // 用来统计有多少个alive的线程
    private static final Logger log = Logger.getAnonymousLogger();      // log

    public MyAppThread(Runnable r){
        this(r, DEFAULT_NAME);
    }

    public MyAppThread(Runnable r, String name){
        super(r, name+"-"+created.incrementAndGet());   // 加入线程自身的编号
        
        setUncaughtExceptionHandler(
            new Thread.UncaughtExceptionHandler(){
            
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    log.log(Level.SEVERE, "UNCAUGHT in thread "+t.getName(), e);
                }
            }
        );
    }

    public void run(){
        // 必须要复制debug标志以确保一致的值(实际上是一种线程封闭)
        boolean debug = debugLifecycle;
        // super.run();
        try{
            alive.incrementAndGet();
            super.run();
        } finally{
            alive.decrementAndGet();
            if(debug){
                log.log(Level.FINE, "Exiting ", this.getName());
            }
        }
    }

    public static int getThreadsCreated(){
        return created.get();
    }

    public static int getThreadAlive(){
        return alive.get();
    }

    public static boolean getDebug(){
        return debugLifecycle;
    }

    public static void setDebug(boolean b){
        debugLifecycle = b;
    }

    public static void main(String... args) throws InterruptedException {
        // 创建10个定制的线程并运行
        for(int i = 0; i < 10; i++){
            new MyAppThread(
                ()->{
                    System.out.println("aaa");
                }
            ).start();
        }
        Thread.sleep(100);
        System.out.println(MyAppThread.getThreadAlive());   // 得到还存活的线程数
        System.out.println(MyAppThread.getThreadsCreated());    // 得到创建过的线程数
    }
}