// import java.util.*;

// public class Test {

//     /**
//      * 对于HashSet的构造函数输入一个Set是浅拷贝, 即在此例中copy和myset同变动(指向同一内存空间)
//      */
//     public static void main(String... args) {
//         Set<Toy> myset = new HashSet<>();
//         for (int i = 0; i < 10; i++) {
//             myset.add(new Toy());
//         }
//         for (Toy t : myset) {
//             t.getId();
//         }
//         System.out.println("");
//         Set<Toy> copy = new HashSet<>(myset);
//         for (Toy t : myset) {
//             t.addId();
//         }
//         for (Toy t : copy) {
//             t.getId();
//         }
//         System.out.println("");

//         for (Toy t : myset) {
//             t.getId();
//         }
//         System.out.println("");

//     }
// }

// class Toy {
//     private int id = 0;
//     private static int total = 0;

//     public Toy() {
//         this.id = total;
//         total += 1;
//     }

//     public void getId() {
//         System.out.print(this.id + " ");
//     }

//     public void addId() {
//         this.id += 1;
//     }
// }
import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.locks.*;

// public class Test{

//     public static void main(String[] args)throws Exception {
//         final Object obj = new Object();
//         Thread A = new Thread(new Runnable() {
//             @Override
//             public void run() {
//                 int sum = 0;
//                 for(int i=0;i<10;i++){
//                     sum+=i;
//                 }
//                 try {
//                     synchronized (obj){
//                         obj.wait();
//                     }
//                 }catch (Exception e){
//                     e.printStackTrace();
//                 }
//                 System.out.println(sum);
//             }
//         });
//         A.start();
//         // 睡眠一秒钟，保证线程A已经计算完成，阻塞在wait方法
//         // 若去掉sleep() 发生阻塞 因为主线程先运行到notify() A线程再wait()
//         Thread.sleep(1000);
//         synchronized (obj){
//             obj.notify();
//         }
//     }
// }

public class Test {
    public static void main(String... args) throws InterruptedException {
        Lock lock = new ReentrantLock();

        final class ChildThread extends Thread {
            Thread father;

            ChildThread(Thread father) {
                this.father = father;
            }

            public void run() {
                try {
                    // Thread.currentThread().sleep(1000);
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    System.out.println("Child Thread has been interrupt");
                }
                father.interrupt();
            }
        }

        lock.lockInterruptibly();
        try{
            Thread t = new ChildThread(Thread.currentThread());
            t.start();
            Thread.currentThread().sleep(1000);
            // while(true){}
            t.interrupt();
        } finally{
            lock.unlock();
        }
    }
}