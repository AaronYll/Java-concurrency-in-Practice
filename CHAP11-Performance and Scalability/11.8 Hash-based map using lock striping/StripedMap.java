import java.util.*;
import java.util.concurrent.*;

public class StripedMap<K, V>{
    private static final int N_LOCKS = 16;
    private final Node<K, V>[] buckets;
    private final K[] locks;    // 泛型数组可以声名但不能通过正常方式构造

    private static class Node<K, V>{
        public Node<K, V> next;
        public K key;
        public V value;

        public Node(K key, V value){
            this.key = key;
            this.value = value;
            this.next = null;
        }
    }

    @SuppressWarnings("unchecked")
    public StripedMap(int numBuckets){
        // buckets = (Node[])new Object[numBuckets];
        Node<?, ?>[] head = new Node<?, ?>[numBuckets];
        buckets = (Node<K, V>[])head;
        locks   = (K[])new Object[N_LOCKS];
        for(int i=0; i<N_LOCKS; i++){
            locks[i] = (K)new Object();
        }
    }

    private final int hash(Object key){
        return Math.abs(key.hashCode()%buckets.length);
    }

    public V get(K key){
        int hash = hash(key);
        synchronized(locks[hash%N_LOCKS]){
            for(Node<K, V> m=buckets[hash]; m!=null; m=m.next)
                if(m.key.equals(key))
                    return (V)m.value;
        }
        return null;
    }

    public void insert(K key, V value){
        int hash = hash(key);
        synchronized(locks[hash%N_LOCKS]){
            if(buckets[hash]==null){
                buckets[hash] = new Node<K, V>(key, value);
            }
            else{
                Node<K, V> m = buckets[hash];
                for(; m.next!=null; m=m.next){
                    if(m.key.equals(key)){
                        m.value = value;
                        return;
                    }
                }
                if(m.key.equals(key)){
                    m.value = value;
                    return;
                }
                m.next = new Node<K, V>(key, value);
            }
        }
    }

    public void clear() {
        for (int i = 0; i < buckets.length; i++) {
            synchronized (locks[i%N_LOCKS]) {
                buckets[i] = null;
            }
        }
    }

    public static void main(String... args) throws InterruptedException {
        StripedMap<String, Integer> map = new StripedMap<>(16);
        ExecutorService exec = Executors.newFixedThreadPool(6);

        class addTask implements Runnable{
            private String key;
            private Integer value;

            public addTask(String key, Integer value){
                this.key = key;
                this.value = value;
            }
            public void run(){
                map.insert(key, value);
            }
        }     

        class getTask implements Runnable{
            private String key;

            public getTask(String key){
                this.key = key;
            }

            public void run(){
                System.out.println(map.get(key));
            }
        }

        exec.execute(new addTask("abc", 1));
        exec.execute(new addTask("def", 2));
        // Thread.sleep(10);
        exec.execute(new getTask("abc"));
        exec.execute(new getTask("def"));
    }
}