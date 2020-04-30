import java.util.concurrent.*;

public class ThreadDeadlock implements Callable<String>{
    // ExecutorService exec = Executors.newSingleThreadExecutor(); // 单线程，发生死锁
    ExecutorService exec = Executors.newFixedThreadPool(10);    // 正常启动

    public class LoadFileTask implements Callable<String>{
        private final String fileName;

        public LoadFileTask(String filename){
            this.fileName = filename;
        }
        @Override
        public String call() throws Exception {
            // TODO Auto-generated method stub
            return fileName;
        }
    }

    public class RenderPageTask implements Callable<String>{
        @Override
        public String call() throws Exception{
            Future<String> header, footer;
            header = exec.submit(new LoadFileTask("header.html"));
            footer = exec.submit(new LoadFileTask("footer.html"));
            String page = renderBody();
            return header.get()+page+footer.get();  // 此处存在依赖
        }

        public String renderBody(){
            return " body.html ";
        }
    }

    public String call(){
        Future<String> f = exec.submit(new RenderPageTask());
        try{
            return f.get();
        } catch(Exception e){
            System.out.println(e.getMessage());
        }
        return null;
    }


    public static void main(String... args){
        ThreadDeadlock t = new ThreadDeadlock();
        System.out.println(t.call());
    }
}