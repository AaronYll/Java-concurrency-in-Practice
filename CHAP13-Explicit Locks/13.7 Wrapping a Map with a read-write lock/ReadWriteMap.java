import java.util.*;
import java.util.concurrent.locks.*;

public class ReadWriteMap<K, V>{
    private final Map<K, V> map;                    // 委托
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock r = lock.readLock();
    private final Lock w = lock.writeLock();

    public ReadWriteMap(Map<K, V> map){             // map的具体构造实现由外部传入
        this.map = map;
    }

    // put k-v pair
    // write
    public V put(K key, V value){
        w.lock();
        try{
            return map.put(key, value);
        } finally{
            w.unlock();
        }
    }

    // remove 
    // write
    public V remove(K key){
        w.lock();
        try{
            return map.remove(key);
        } finally{
            w.unlock();
        }
    }

    // put all
    // write
    public void putAll(Map<? extends K, ? extends V> m){
        w.lock();
        try{
            map.putAll(m);
        } finally{
            w.unlock();
        }
    }

    // clear
    // write
    public void clear(){
        w.lock();
        try{
            map.clear();
        } finally{
            w.unlock();
        }
    }

    // get
    // read
    public V get(K key){
        r.lock();
        try{
            return map.get(key);
        } finally{
            r.unlock();
        }
    }

    // size
    // read
    public int size(){
        r.lock();
        try{
            return map.size();
        } finally{
            r.unlock();
        }
    }

    // imEmpty
    // read
    public boolean isEmpty(){
        r.lock();
        try{
            return map.isEmpty();
        } finally{
            r.unlock();
        }
    }

    // containsKey
    // read
    public boolean containsKey(K key){
        r.lock();
        try{
            return map.containsKey(key);
        } finally{
            r.unlock();
        }
    }

    // containsValue
    // read
    public boolean containsValue(V value){
        r.lock();
        try{
            return map.containsValue(value);
        } finally{
            r.unlock();
        }
    }
}