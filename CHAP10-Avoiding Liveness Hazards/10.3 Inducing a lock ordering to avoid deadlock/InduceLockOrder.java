import java.util.concurrent.atomic.*;

/**
 * 通过锁顺序来避免死锁
 * 由于动态顺序容易发生死锁，所以需要一种机制来固定锁顺序
 * 这里使用identityHashCode() 通过哈希值的映射来给线程规定一个按“哈希值大小”意义上的顺序来获得锁，从而来获得一致性的锁（实际上去防止出现锁依赖的环）
 * 当哈希值相同时(概率较小)，此时若按照参数次序执行还是可能发生死锁，所以需要额外增加一个锁，执行时请求该锁确保只有一个线程执行，从而不会发生因锁顺序不同而发生的饱死问题
 */
public class InduceLockOrder{
    private static final Object tieLock = new Object();     // "加时赛"锁
    
    public void transferMoney(final Account fromAcct, final Account toAcct, final DollarAmount amount) throws InsufficientFundsException{
        /**
         * 内部类Helper
         * 闭包 操作的外部变量为fromAcct和toAcct
         * 为了一致性要求这个操作开始前要请求锁
         * 封装transfer减少代码量
         */
        class Helper{
            public void transfer() throws InsufficientFundsException{
                if(fromAcct.getBalance().compareTo(amount) < 0){
                    throw new InsufficientFundsException();
                }
                else{
                    fromAcct.debit(amount);
                    toAcct.credit(amount);
                }
            }
        }
        int fromHash = System.identityHashCode(fromAcct);
        int toHash   = System.identityHashCode(toAcct);
        if(fromHash > toHash){
            synchronized(fromAcct){
                synchronized(toAcct){
                    new Helper().transfer();
                }
            }
        }
        else if(fromHash < toHash){
            synchronized(toAcct){
                synchronized(fromAcct){
                    new Helper().transfer();
                }
            }
        }
        else{
            // 这种情况下hashcode相同，无法以hash值为标准提供请求锁的先后顺序，使用“加时锁”
            synchronized(tieLock){
                synchronized(fromAcct){
                    synchronized(toAcct){
                        new Helper().transfer();
                    }
                }
            }
        }
    }

    static class DollarAmount implements Comparable<DollarAmount> {
        // Needs implementation

        public DollarAmount(int amount) {
        }

        public DollarAmount add(DollarAmount d) {
            return null;
        }

        public DollarAmount subtract(DollarAmount d) {
            return null;
        }

        public int compareTo(DollarAmount dollarAmount) {
            return 0;
        }
    }

    static class Account {
        private DollarAmount balance;
        private final int acctNo;
        private static final AtomicInteger sequence = new AtomicInteger();

        public Account() {
            acctNo = sequence.incrementAndGet();
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
    public static void main(String... args){
        System.out.println("a");
    }
}