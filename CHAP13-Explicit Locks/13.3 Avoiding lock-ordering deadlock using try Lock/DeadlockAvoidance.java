import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import static java.util.concurrent.TimeUnit.*;

/**
 * 通过tryLock来避免死锁
 */
public class DeadlockAvoidance {
    private static Random rnd = new Random();
    private static final int NUM_THREADS = 20;
    private static final int NUM_ACCOUNTS = 5;
    private static final int NUM_ITERATIONS = 100000;

    public static boolean transferMoney(
        Account fromAccount, 
        Account toAccount, 
        DollarAmount amount,
        long timeout,
        TimeUnit unit) throws InsufficientFundsException, InterruptedException {
        // 易发生动态死锁的版本
        // synchronized (fromAccount) {
        //     synchronized (toAccount) {
        //         // 看似所有的线程都是按照相同的顺序来获得锁，但事实上锁的顺序取决于传递给tansferMoney的参数顺序
        //         // 所以仍然容易发生锁顺序死锁 (持有X锁请求Y，持有Y锁请求A)
        //         if (fromAccount.getBalance().compareTo(amount) < 0)
        //             throw new InsufficientFundsException();
        //         else {
        //             fromAccount.debit(amount);
        //             toAccount.credit(amount);
        //         }
        //     }
        // }
        long fixedDelay = getFixedDelayComponentNanos(timeout, unit);
        long randMod    = getRandomDelayModulusNanos(timeout, unit);
        long stopTime   = System.nanoTime() + unit.toNanos(timeout);    //  指定超时失败时间
        
        while(true){                                    // 轮询
            if(fromAccount.lock.tryLock()){             // 如果成功获得fromAcct的锁
                try{
                    if(toAccount.lock.tryLock()){       // 到这里成功获得两个锁
                        try{
                            fromAccount.debit(amount);
                            toAccount.credit(amount);
                            return true;                // 成功转账
                        } finally{
                            toAccount.lock.unlock();    // 注意一定要在finally块中释放锁
                        }
                    }
                } finally{                              
                    fromAccount.lock.unlock();          // 注意一定要在finally块中释放锁
                }
            }
            if(System.nanoTime() > stopTime){           // 超时返回失败
                return false;
            }
            NANOSECONDS.sleep(fixedDelay + rnd.nextLong()%randMod); // 不能同时获得锁，休眠一段时间后重新尝试
        }
    }

    private static final int DELAY_FIXED = 1;
    private static final int DELAY_RANDOM = 2;

    static long getFixedDelayComponentNanos(long timeout, TimeUnit unit){
        return DELAY_FIXED;
    }

    static long getRandomDelayModulusNanos(long timeout, TimeUnit unit){
        return DELAY_RANDOM;
    }

    static class DollarAmount implements Comparable<DollarAmount> {
        private int mydollarAmount;

        public DollarAmount(int amount) {
            mydollarAmount = amount;
        }

        public DollarAmount add(DollarAmount d) {
            return new DollarAmount(mydollarAmount + d.mydollarAmount);
        }

        public DollarAmount subtract(DollarAmount d) {
            return new DollarAmount(mydollarAmount - d.mydollarAmount);
        }

        public int compareTo(DollarAmount dollarAmount) {
            return mydollarAmount - dollarAmount.mydollarAmount;
        }

        public int getAmount() {
            return mydollarAmount;
        }
    }

    static class Account {
        private DollarAmount balance;
        private final int acctNo;
        private static final AtomicInteger sequence = new AtomicInteger();
        public Lock lock;   // 增加account的显式锁

        public Account() {
            acctNo  = sequence.incrementAndGet();
            balance = new DollarAmount(1000);
            lock    = new ReentrantLock(); 
        }

        void debit(DollarAmount d) {
            balance = balance.subtract(d);
        }

        void credit(DollarAmount d) {
            balance = balance.add(d);
        }

        DollarAmount getBalance() {
            return balance;
        }

        int getAcctNo() {
            return acctNo;
        }
    }

    static class InsufficientFundsException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

    }

    public static void main(String[] args) throws InsufficientFundsException, InterruptedException {
        final Random rnd2 = new Random();
        final Account[] accounts = new Account[NUM_ACCOUNTS];

        for (int i = 0; i < accounts.length; i++)
            accounts[i] = new Account();

        class TransferThread extends Thread {
            public void run() {
                for (int i = 0; i < NUM_ITERATIONS; i++) {
                    int fromAcct = rnd2.nextInt(NUM_ACCOUNTS);
                    int toAcct = rnd2.nextInt(NUM_ACCOUNTS);
                    DollarAmount amount = new DollarAmount(rnd2.nextInt(1000));
                    try {
                        transferMoney(accounts[fromAcct], accounts[toAcct], amount, 100, NANOSECONDS);
                    } catch (InsufficientFundsException ignored) {
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }
        for (int i = 0; i < NUM_THREADS; i++)
            new TransferThread().start();   //  不发生死锁,程序运行完顺利退出
        
        Thread.sleep(2000);
        
        int total = 0;
        for(int i = 0; i< NUM_ACCOUNTS; i++){
            System.out.println(accounts[i].getBalance().getAmount());
            total += accounts[i].getBalance().getAmount();
        }
        System.out.println("total number is:"+total);
    }
}