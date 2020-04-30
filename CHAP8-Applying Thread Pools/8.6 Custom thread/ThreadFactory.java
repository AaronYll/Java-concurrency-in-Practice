/**
 * ThreadFactory接口
 */
public interface ThreadFactory{
    Thread newThread(Runnable r);
}