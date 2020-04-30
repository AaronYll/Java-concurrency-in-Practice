import java.util.concurrent.atomic.*;
import java.util.*;

public class DemonstrateDeadlock {
    private static final int NUM_THREADS = 20;
    private static final int NUM_ACCOUNTS = 5;
    private static final int NUM_ITERATIONS = 100000;

    public static class DynamicOrderDeadlock {
        // Warning: deadlock-prone!
        public static void transferMoney(Account fromAccount, Account toAccount, DollarAmount amount)
                throws InsufficientFundsException {
            synchronized (fromAccount) {
                synchronized (toAccount) {
                    // 看似所有的线程都是按照相同的顺序来获得锁，但事实上锁的顺序取决于传递给tansferMoney的参数顺序
                    // 所以仍然容易发生锁顺序死锁 (持有X锁请求Y，持有Y锁请求A)
                    if (fromAccount.getBalance().compareTo(amount) < 0)
                        throw new InsufficientFundsException();
                    else {
                        fromAccount.debit(amount);
                        toAccount.credit(amount);
                    }
                }
            }
        }

        public static class DollarAmount implements Comparable<DollarAmount> {
            // Needs implementation
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

        public static class Account {
            private DollarAmount balance;
            private final int acctNo;
            private static final AtomicInteger sequence = new AtomicInteger();

            public Account() {
                acctNo = sequence.incrementAndGet();
                balance = new DollarAmount(1000);
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
    }

    public static void main(String[] args) throws DynamicOrderDeadlock.InsufficientFundsException {
        final Random rnd = new Random();
        final DynamicOrderDeadlock.Account[] accounts = new DynamicOrderDeadlock.Account[NUM_ACCOUNTS];

        for (int i = 0; i < accounts.length; i++)
            accounts[i] = new DynamicOrderDeadlock.Account();

        class TransferThread extends Thread {
            public void run() {
                for (int i = 0; i < NUM_ITERATIONS; i++) {
                    int fromAcct = rnd.nextInt(NUM_ACCOUNTS);
                    int toAcct = rnd.nextInt(NUM_ACCOUNTS);
                    DynamicOrderDeadlock.DollarAmount amount = new DynamicOrderDeadlock.DollarAmount(rnd.nextInt(1000));
                    try {
                        DynamicOrderDeadlock.transferMoney(accounts[fromAcct], accounts[toAcct], amount);
                    } catch (DynamicOrderDeadlock.InsufficientFundsException ignored) {
                    }
                }
            }
        }
        for (int i = 0; i < NUM_THREADS; i++)
            new TransferThread().start();   //  发生死锁
    }
}