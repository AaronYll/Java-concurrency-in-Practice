import java.util.*;
import java.util.concurrent.*;


public class TrackingExcutor extends AbstractExecutorService {
    private final ExecutorService exec; //委托
    private final Set<Runnable> tasksCancelledAtShutdown = Collections.synchronizedSet(new HashSet<Runnable>());

    public TrackingExcutor(ExecutorService exec){
        this.exec = exec;
        // Executors.newCachedThreadPool();
        }

    public void shutdown(){
        exec.shutdown();
    }

    public List<Runnable> shutdownNow(){
        return exec.shutdownNow();
    }

    public boolean isShutdown(){
        return exec.isShutdown();
    }

    public boolean isTerminated(){
        return exec.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException{
        return exec.awaitTermination(timeout, unit);
    }

    public List<Runnable> getCancelledTasks(){
        if(!exec.isTerminated()){
            throw new IllegalStateException("tasks havnt completed");
        }
        return new ArrayList<>(tasksCancelledAtShutdown);
    }

    public void execute(final Runnable runnable){
        exec.execute(()->{
            try{
                runnable.run();
            } finally{
                if(isShutdown() && Thread.currentThread().isInterrupted()){ // 如果被委托的exec已经被shut down并且当前线程已经被中断. 注意这段代码在finally块中，所以要使这个策略发挥作用，任务在返回时必须维持线程的中断状态
                    tasksCancelledAtShutdown.add(runnable);                 // 将当前执行的任务加入tasksCancelledAtShutdown集合
                }
            }
        });
    }

  
}