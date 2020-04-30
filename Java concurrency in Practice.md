
---
## CHAP 11 性能与可伸缩性

- 线程的最主要目的是提高程序的运行性能
- 提升性能的前提是要把安全性放在第一位，首先要保持程序的正确性，然后仅当程序的性能需求和测试结果要求程序执行地更快时，才应该设法提高它的运行速度。

### 11.1 对性能的思考

- 当操作性能由于某种特定的资源而受到限制时，我们通常将该操作成为资源密集型的操作。
> 资源例如 CPU时钟周期、内存、网络带宽、I/O带宽、数据库请求、磁盘空间以及其他资源。

- 引入多个线程造成的额外开销包括：<p>线程之间的协调(加锁、触发信号、内存同步等)、增加的上下文切换、线程的创建和销毁、线程的调度等

- 应用程序的性能指标：<p>衡量程序的“运行速度”：服务时间、延迟时间等<p>衡量程序的“处理能力”：生产量、吞吐量等<p>另外还有一些指标：吞吐率、效率、可伸缩性、容量等
> 可伸缩性指的是：当增加计算资源时 (如CPU、内存、存储容量或I/O带宽)，程序的吞吐量或者处理能力能相应地增加。

因此`在并发应用程序中针对可伸缩性进行设计和调整时所采用的方法与传统的性能调优方法截然不同`。

- 性能调优：用更小的代价完成相同的工作 (时间、空间等)。
- 可伸缩性调优： 设法将问题的计算并行化，从而能利用更多的计算资源来完成更多的工作。

> 避免不成熟的优化。首先使程序正确，然后再提高运行速度——如果它还运行地不够快。
`大多数优化措施不成熟的原因之一：通常无法获得一组明确的需求。(以快排和冒泡排序在不同数据规模上的执行效率为例)`

### 11.2 Amdahl定律
- Amdahl定律描述的是：<p>在增加计算资源的情况下，程序在理论上能实现的最高加速比，这个值取决于程序中可并行组件与串行组件所占的比重。假定F是必须被串行执行执行的部分，则在包含N个处理器的机器中，最高加速比为： Speedup <= 1/(F+(1-F)/N)
> 当N趋近无穷大时，Speedup趋近于1/F，即当程序有50%的计算需要串行执行，那么最高加速比为2(而不管有多少线程可用)。 故`要预测应用程序在某个多处理器系统中将实现多大的加速比，还需要找出任务中的串行部分`。

- 另一种常见的串行操作是对结果进行处理。`所有有用的计算都会生成某种结果或者产生某种效应`。示例中的Runnable没有提供明确的结果处理过程，因此这些任务一定会产生某种效果，例如将它们的结果写入到日志或者保存到某个数据结构。`通常，日志文件和结果容器都会由多个工作者线程共享，并且这也是一个串行部分`。

- 对于BlockingQueue，使用LinkedBlockingQueue作为工作队列，那么出列操作被阻塞的可能性将小于使用ConcurrentLinkedList时发生阻塞的可能性。(`然而无论访问哪种共享数据结构，基本上都会在程序中引入一个串行部分`)

- 还有一种容易忽略的常见串行操作：对结果进行处理。`所有有用的计算都会生成某种结果或者产生某种效应`——如果不会，那么可以将它们作为“Dead Code”删除掉。通常日志文件和结果容器都会由多个工作者线程共享，并且这也是一个串行部分。
>由于Runnable没有提供明确的结果处理过程，因此这些任务一定会产生某种效果，例如将它们的结果写入到日志或者保存到某个数据结构。(否则就是“Dead Code”)
>>`在所有的并发程序中都包含一些串行部分`。

示例：
```java
/**
 * BlockingQueue是在类外发布的，在创建WorkerThread时传入
 * 这个程序看起来可以完全并行：各个任务之间不会相互等待，因此处理器越多，能够并发处理的任务也就越多
 * 然而从队列中获取任务是串行的：所以工作者任务共享同一个工作队列，因此在进行并发访问时使用了某种并发机制
 */
public class WorkerThread extends Thread{
    private final BlockingQueue<Runnable> queue;

    public WorkerThread(BlockingQueue<Runnable> queue){
        this.queue = queue;
    }

    public void run(){
        while(true){
            try{
                Runnable task = queue.take();
                task.run();
            } catch(InterruptedException e){
                break; /* Allow thread to exit*/
            }
        }
    }
}
```
如上所示，这个程序看起来可以完全并行：各个任务之间不会相互等待，因此处理器越多，能够并发处理的任务也就越多，`然而从队列中获取任务是串行的`：所有工作者任务共享同一个工作队列，因此在进行并发访问时使用了某种同步机制来维持队列的完整性。
> 单个任务的处理时间不仅包括执行任务Runnable的时间，也包括从共享队列中取出任务的时间。<p>再次强调，`所有并发程序中都包含一些串行部分`。

### 11.3 线程引入的开销
- 单线程程序既不存在线程调度，也不存在同步开销，而且不需要使用锁来保证数据结构的一致性。

- 在多个线程的调度和协调过程中都需要一定的性能开销：`对于为了提升性能而引入的线程来说，并行带来的性能提升必须超过并发导致的开销`。

#### 11.3.1 上下文切换
- 如果`可运行的线程数大于CPU的数量，那么操作系统最终会将某个正在运行的线程调度出来`，从而使其它线程能够使用CPU，这将导致一次上下文切换。

- 上下文切换的过程中将保存当前运行线程的执行上下文，并将新调度进来的线程的执行上下文设置为当前上下文。

- 切换上下文需要一定的开销，而在`线程调度过程中需要访问由操作系统和JVM共享的数据结构`。(如被阻塞的线程在其执行时间片还未用之前被交换出去，而在随后当要获取的锁或者其它资源可用时，又再次被切换回来，都需要操作系统介入。)

- 应用程序、操作系统和JVM都使用一组相同的CPU。在JVM和操作系统的代码中消耗越多的CPU时钟周期，应用程序的可用CPU就越少。

- 上下文切换开销除操作系统和JVM外，`还包括缓存缺失的开销`。当一个新的线程被切换进来时，它所需要的数据可能不在当前处理器的本地缓存中，因而线程在首次调度运行时会更加缓慢。

- 当线程由于等待某个发生竞争的锁而被阻塞时，JVM通常会将这个线程挂起，并允许它被交换出去。

- 如果线程频繁地发生阻塞，那么它们将无法使用完整的调度时间片，在程序中发生越多的阻塞 (包括阻塞I/O，等待获取发生竞争的锁，或者在条件变量上等待)，程序就会发生越多上下文切换，从而增加调度开销，并因此降低吞吐量。

- 无阻塞算法有助于减小上下文开销。

#### 11.3.2 内存同步
- 在synchronized和volatile提供的可见性保证中会使用一些特殊指令，即`内存栅栏`。

- 内存栅栏可以刷新缓存，使缓存无效，刷新硬件的写缓冲以及停止管道执行。内存栅栏将抑制一些编译器优化操作，在内存栅栏中，大多数操作是不能被重排序的。

- 现代JVM能通过优化来去掉一些不会发生竞争的锁 (`非竞争同步`)，从而减小不必要的同步开销。
1. 如果一个锁对象只能由当前线程访问，那么JVM就可以通过优化来去掉这个锁。下面是一个示例：
```java
synchronized(new Object()){
    // 执行一些操作
}
```
2. 一些更完备的JVM能通过溢出分析来找出不会发布到堆的本地对象引用 (这个引用是线程本地的)，在getStoogeNames的执行过程中，由于Vector是线程安全的，至少会将Vector上的锁获取/释放4次，然而编译器通常会分析这些调用，从而使stooges及其内部状态不会溢出，从而去掉这四次锁操作：
```java
import java.util.*;
/**
 * ThreeStooges
 * Immutable class built out of mutable underlying objects,
 * demonstration of candidate for lock elision
 */
 public final class ThreeStooges {
    private final Set<String> stooges = new HashSet<String>();

    public ThreeStooges() {
        stooges.add("Moe");
        stooges.add("Larry");
        stooges.add("Curly");
    }

    public boolean isStooge(String name) {
        return stooges.contains(name);
    }

    /**
     * 溢出分析，stooges内部状态不溢出，去掉四次锁操作
     */
    public String getStoogeNames() {
        List<String> stooges = new Vector<String>();
        stooges.add("Moe");
        stooges.add("Larry");
        stooges.add("Curly");
        return stooges.toString();
    }
}
```

- 锁粒度粗化 (Lock Coarsening):`编译器可以将邻近的同步代码块用同一个锁合并起来` (在getStoogeNames中，可能会把3add和1toString调用合并为单个锁获取/释放操作)。不仅减少了同步开销，还能使优化器处理更大的代码块，从而可能实现进一步优化。
>不用过度担心非竞争同步的开销，这个基本的机制已经非常快了，并且JVM还进行了一系列优化。`应该将优化重点放在发生锁竞争的地方`。

- 某个线程中的同步可能会影响其它线程的性能，因为`同步会增加共享内存总线上的通信量，而总线的带宽是有限的，所有处理器都将共享这条总线`。

### 11.4 减少锁的竞争

- 串行操作会降低可伸缩性，并且上下文切换也会降低性能。在锁上发生竞争时将同时导致这两种问题，因此减少锁的竞争能够提高性能和可伸缩性。

> 在并发程序中，对可伸缩性的最主要威胁就是独占方式的资源锁。

- 有两个因素将影响在锁上发生竞争的可能性：<p>1. 锁的请求频率<p>2. 每次持有锁的时间<p>如果二者的乘积很小，那么大多数获取锁的操作都不会发生竞争，因此在该锁上的竞争不会对可伸缩性造成严重影响。
> 这是排队理论的一个推论， “在一个稳定的系统中，顾客的平均数量等于它们的平均到达率乘以在系统中的平均停留时间”。

- 有3种方式可以降低锁的竞争程度：<p>减少锁的持有时间。——11.4.1 缩小锁的范围 <p>降低锁的请求频率。——11.4.2 锁分解 11.4.3 锁分段<p>使用带有协调机制的独占锁，这些机制允许更高的并发性。——11.4.5 一些代替独占锁的方法

#### 11.4.1 缩小锁的范围(“快进快出”)
- 将一些与锁无关的代码移出同步代码块，尤其是开销较大的操作以及可能被阻塞的操作，例如I/O操作。如下例:
```java
/**
 * 将一个锁不必要地持有过长时间
 * 在一个Map对象中查找用户位置，并使用正则表达式进行匹配以判断结果值是否匹配所提供的模式。
 * 整个userLocationMatches方法都用了synchronized来修饰，但只有Map.get这个方法才真正需要锁，应该将其它代码移出同步代码块
 */
public class AttributeStore{
    private final Map<String, String> attributes = new HashMap<>();

    public synchronized boolean userLocationMatches (String name, String regexp){
        String key = "users."+name+".location";
        String location = attribute.get(key);
        if(location == null)
            return false;
        else
        return Pattern.matches(regexp, location);
    }
}
改为——>
public class BetterAttributeStore {
    private final Map<String, String> attributes = new HashMap<String, String>();

    public boolean userLocationMatches(String name, String regexp) {
        String key = "users." + name + ".location";
        String location;
        synchronized (this) {
            location = attributes.get(key);
        }
        if (location == null)
            return false;
        else
            return Pattern.matches(regexp, location);
    }
}
```
>通过缩小缩小userLocationMatches方法中锁的作用范围，能极大地减少在持有锁时需要的执行指令数量，这样串行代码的指令就减少了。

- 尽管缩小同步代码块能提高可伸缩性，但同步代码块也不应过小：<p>首先，一个原因是至少需要把那些需要采用原子方式执行的操作 (如对某个不变性条件中的多个变量进行更新) 必须包含在一个同步块中。<p>此外，同步也需要一定的开销，`当把一个同步代码块分解为多个同步代码块时 (在确保正确性的情况下)，反而会对性能提升产生负面影响`。(因此会有11.3.2中 编译器的锁粒度粗化)

#### 11.4.2 减小锁的粒度
- 另一种减小锁的持有时间的方式是降低线程请求锁的频率 (从而减小发生竞争的可能性)。可以通过`锁分解`和`锁分段`技术来实现。

- 锁分解：如果`一个锁要保护多个互相独立的状态变量`，那么可以将这个锁分解为多个锁，并且每个锁都保护一个变量，从而提高可伸缩性，并最终降低每个锁被请求的频率。如下例：
```java
import java.util.*;

/**
 * 给出了某个数据库服务器的部分监视接口，该数据库维护了当前已登陆的用户以及正在执行的请求
 * users和queries信息是完全独立的
 * 这里对于独立的信息共用一个内置对象锁
 */
 public class ServerStatus{
     public final Set<String> users;
     public final Set<String> queries;
     ...
     public synchronized void addUser(String u) {
         users.add(u);
     }

    public synchronized void addQuery(String q) {
        queries.add(q);
    }

    public synchronized void removeUser(String u) {
        users.remove(u);
    }

    public synchronized void removeQuery(String q) {
        queries.remove(q);
    }
 }
 ——>
 /**
 * 采用锁分解
 * ServerStatus refactored to use split locks
 * 在代码中不是用ServeStatus锁来保护用户状态和查询状态，而是每个状态都通过一个锁来保护
 * 新的细粒度锁上的访问量将比最初的访问量少
 */
public class ServerStatusAfterSplit {
    public final Set<String> users;
    public final Set<String> queries;

    public ServerStatusAfterSplit() {
        users = new HashSet<String>();
        queries = new HashSet<String>();
    }

    // 用户信息使用users上的锁
    public void addUser(String u) {
        synchronized (users) {
            users.add(u);
        }
    }

    // 请求信息使用queries上的锁
    public void addQuery(String q) {
        synchronized (queries) {
            queries.add(q);
        }
    }

    public void removeUser(String u) {
        synchronized (users) {
            users.remove(u);
        }
    }

    public void removeQuery(String q) {
        synchronized (users) {
            queries.remove(q);
        }
    }
}
```
>在上例锁分解中，通过将用户状态和查询状态委托给两个不同的线程安全的Set而不是用显示的同步也隐含着对锁进行分解。

- 锁分段：在某些情况下，可以将锁分解技术进一步扩展为对一组独立对象上的锁进行分解。
>例如在ConcurrentHashMap中使用了一个包含16个锁的数组，每个锁保护所有散列桶的1/16，其中第N个散列桶由第 (N mod 16) 个锁来保护。假设散列函数具有合理的分布性，并且关键字能够实现均匀分布，那么这大约能把每个锁的请求频率降到原来的1/16。`正是这项技术使ConcurrentHashMap能够支持多达16个并发的写入器`。<p>如下例：

```java
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
```
- 锁分段的一个劣势在于：与采用单个锁来来实现独占访问相比，要获取多个锁来实现独占访问将更加困难并且开销更高。
>通常在执行一个操作时最多只需获取一个锁，但在某些情况下需要加锁整个容器，`例如当ConcurrentHashMap需要扩展映射范围，以及重新计算键值的散列值要分布到更大的桶集合中时`，就需要获取分段锁集合中所有的锁。`但并不要求同时获得，如上例的clear`

>要获取内置锁的一个集合，能采用的唯一方式是递归。

- 如果`程序采用锁分段技术，那么一定要表现出在锁上的竞争频率高于在锁保护的数据上发生竞争的频率`。(例如一个锁保护两个独立变量X和Y，线程A想要访问X，线程B想要访问Y，那么这两个线程不会在任何数据上发生竞争，但会在同一个锁上发生竞争)。

#### 11.4.4 避免热点域

- 在单线程或者采用完全同步的实现中，使用一个独立的计数能很好地提高类似size和isEmpty这些方法的执行速度，但却更难以提升实现的可伸缩性,`因为每个修改map的操作都需要更新这个共享的计数器`，在这种情况下，计数器也被成为热点域。

- 一些常见的优化措施，例如将一些反复计算的结果缓存起来，都会引入一些“热点域(Hot Field)”，而这些热点域往往会限制可伸缩性。

>在ConcurrentHashMap中计算size时，为了避免引入热点域，将size的计算分到每个分段锁中，每个分段锁维护一个值，而不是维护一个全局的size。

#### 11.4.5 一些代替独占锁的方法
第三种降低竞争锁的影响的技术是放弃使用独占锁，使用一种友好并发的方式来管理共享状态。
- 使用`并发容器`
- 使用`ReadWriteLock`：对于读取操作占多数的数据结构，RW锁能提供比独占锁更高的并发性。
- `原子变量`提供了一种方式来降低更新“热点域”的开销。`如果在类中只包含少量的热点域，并且这些域不会与其它变量参与到不变性条件中`，那么使用原子变量来代替它们能提高可伸缩性。

#### 11.4.7 不要使用对象池
- 在对象池中，对象能被循环使用，而不是由垃圾回收器自动分配。

- 在单线程程序中，使用对象池技术能降低垃圾收集操作的开销，但对于搞开销对象意外的其它对象来说仍然存在性能缺失。(不展开)

- 在并发程序中，对象池的表现更加差。因为`当线程分配新对象时，基本不需要在线程之间进行协调，因为对象分配器通常会使用线程本地的内存块，不需要在堆数据结构上进行同步`，但如果从对象池请求一个对象就`需要通过某种同步来协调对对象池数据结构的访问`。
> 通常，对象分配操作的开销比同步的开销更低。
---
## CHAP 12 并发程序的测试

- 在测试并发程序时，所面临的主要挑战在于：潜在错误的发生并不具有确定性，而是随机的。要在测试中将这些故障暴露出来，就需要比普通的串行程序测试覆盖更广的范围并且执行更长时间。

并发测试大致分为两类：
- 安全性测试：“不发生任何错误的行为”
- 活跃性测试：”某个良好的行为终究会发生“
> 性能测试与活跃性测试相关，包括：<p> 吞吐量：指一组并发任务中已完成任务所占的比例。<p>响应性：指请求从发出到完成之间的时间(也称为延迟)。<p>可伸缩性：在增加更多资源的情况下(通常指CPU)，吞吐量的提升情况。

### 12.1 正确性测试

- 在为某个并发类设计单元测试时，`首先需要执行与测试串行类时相同的分析———找出需要检查的不变性条件和后验条件`。

下面是一个测试基于信号量的有界队列的测试：
```java
import java.util.concurrent.*;
import junit.framework.TestCase;
/**
 * BounderBuffer 基于信号量的有界缓存
 * BounderBuffer实现了一个固定长度的队列，其中定义了可阻塞的put和take方法(通过两个计数信号量进行控制)。，
 */
public class SemaphoreBoundedBuffer<E> {
    private final Semaphore availableItems, availableSpace; // 创建两个信号量，一个表示可取用的资源数量，一个表示可放入资源的空间
    private E[] buffer;
    private int putPosition = 0, takePosition = 0;

    public SemaphoreBoundedBuffer(int capacity) {
        buffer = (E[]) new Object[capacity];
        availableItems = new Semaphore(0);
        availableSpace = new Semaphore(capacity);
    }

    public boolean isEmpty() {
        return availableItems.availablePermits() == 0;
    }

    public boolean isFull() {
        return availableSpace.availablePermits() == 0;
    }

    public void put(E x) throws InterruptedException {
        availableSpace.acquire();
        doInsert(x);
        availableItems.release();
    }

    public synchronized void doInsert(E x) {
        int i = putPosition;
        buffer[i] = x;
        putPosition = (++i == buffer.length) ? 0 : i;
    }

    public E take() throws InterruptedException {
        availableItems.acquire();
        E item = doExtract();
        availableSpace.release();
        return item;
    }

    public synchronized E doExtract() {
        int i = takePosition;
        E item = buffer[i];
        buffer[i] = null; // 防止内存泄漏
        takePosition = (++i == buffer.length) ? 0 : i;
        return item;
    }
}

/**
 * 基本的测试单元， 类似于在串行上下文中执行的测试 首先创建一个有界缓存，然后调用它的各个方法，并验证它的后验条件和不变性条件 
 * 不变性条件： 
 * 1. 新建立的缓存是空的 
 * 2. 将N个元素插入到容量为N的缓存中，然后测试缓存是否已经被填满。
 * 3. 对阻塞操作的测试
 */
class BoundedBufferTest extends TestCase{
    private static final long LOCKUP_DETECT_TIMEOUT = 1000;

    /**
    * 基本单元测试
    */
    void testIsEmptyWhenConstructed(){
        SemaphoreBoundedBuffer<Integer> bb = new SemaphoreBoundedBuffer<>(10);
        assertTrue(bb.isEmpty());
        assertFalse(bb.isFull());
    }

    /**
    * 基本单元测试
    */
    void testIsFullAfterPuts() throws InterruptedException {
        SemaphoreBoundedBuffer<Integer> bb = new SemaphoreBoundedBuffer<>(10);
        for(int i=0; i<10; i++){
            bb.put(i);
        }
        assertTrue(bb.isFull());    // 顺利运行
        assertFalse(bb.isEmpty());  // 顺利运行
    }

    /**
    * 对阻塞操作的测试
    */
    void testTakeBlocksWhenEmpty(){
        final SemaphoreBoundedBuffer<Integer> bb = new SemaphoreBoundedBuffer<>(10);
        Thread taker = new Thread(){
            public void run(){
                try{
                    // 测试了阻塞属性
                    int unused = bb.take();
                    fail();     // 对空的BoundedBuffer进行take, 若未阻塞运行到这里说明fail
                } catch (InterruptedException success){
                    // 测试了take在中断后能抛出InterruptedException
                    System.out.println("success");
                }
            }
        };
        try{
            taker.start();
            Thread.sleep(LOCKUP_DETECT_TIMEOUT);
            // 如果不进行interrupt taker.isAlive==True， 报junit.framework.AssertionFailedError
            // taker.interrupt();  
            // 如果take操作由于某种意外停滞了，那么支持限时的join方法能确保测试最终完成
            taker.join(LOCKUP_DETECT_TIMEOUT);  // 经过LOOKUP_DETECT_TIMEOUT时间后join过期
            assertFalse(taker.isAlive());
        } catch(Exception unexpected){
            fail();
        }
    }


    
    public static void main(String... args) throws InterruptedException {
        BoundedBufferTest t = new BoundedBufferTest();
        t.testIsEmptyWhenConstructed();
        t.testIsFullAfterPuts();
        t.testTakeBlocksWhenEmpty();
    }
}
```
> 注：join()方法的作用，是当前调用线程需等待调用join方法的线程对象结束。<p>例中taker.join()方法`阻塞调用此方法的线程(calling thread)进入TIMED_WAITING状态`，这里为main()主线程，直到线程taker完成，此线程再继续。<p>join(long millis)还支持定时，在millis时间后join方法过期，若原线程仍活跃，则返回并行执行状态。

#### 12.1.3 安全性测试 
上例的测试无法发现由于数据竞争而引发的错误。`要想测试一个并发类在不可预测的并发访问情况下能否正确执行，需要创建多个线程`来分别执行put和take操作，并在执行一段时间后判断在测试中是否会出现问题。

要测试在生产者-消费者模式中使用的类，一种有效的方法就是检查被放入队列中和从队列中取出的各个元素。

- 一个较好的方法是通过一个`对顺序敏感的校验和计算函数`来计算所有入列元素以及出列元素的校验和，如果两者相等，那么测试就是成功的。
- 将这种方法扩展到多生产者-多消费者的情况，就需要一个`对元素入列/出列顺序不敏感的校验和函数` (因为共用一个缓存队列，不能保证put/take的顺序，只能保证总体take和put数相同) ，从而在测试程序运行完成后，可以将多个校验和以不同的顺序组合起来。
> 在构建并发类的安全测试中，有个需要解决的关键问题是，要找出哪些容易出错的属性(在发生错误的情况下极可能失败)，`同时又不会使得错误检查代码人为地限值并发性`。如上面对多生产者-多消费者情况中需要选择顺序不敏感的校验和函数，否则需要访问一个共享的校验和变量按序累加，需要同步，限制了并发性。

下面是多生产者-多消费者安全性测试的例子：
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import junit.framework.TestCase;

/**
 * 测试SemaphoreBoundedBuffer的生产者-消费者程序
 */
public class PutTakeTest extends TestCase{
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private final AtomicInteger putSum  = new AtomicInteger(0);
    private final AtomicInteger takeSum = new AtomicInteger(0);
    private final CyclicBarrier barrier;
    private final SemaphoreBoundedBuffer<Integer> bb;
    /**
     * nTrials: 每个生产者/消费者取出/放入数字的个数
     * nPairs: 生产者/消费者对数
     */
    private final int nTrials, nPairs;  


    PutTakeTest(int capacity, int npairs, int ntrials){
        nTrials = ntrials;
        nPairs  = npairs;
        bb      = new SemaphoreBoundedBuffer<>(capacity);
        /**
         * barrier: 循环栅栏，阻塞一组线程直到线程数达到2*npairs+1(主线程+2*npairs个消费者/生产者线程)
         */
        barrier = new CyclicBarrier(2*npairs+1);   // 注意等待栅栏为barrier.await()而不是wait()(执行wait()需要获取监视器，否则报监视器错)
    }

    static  int xorshift(int y){
        y ^= (y << 6);
        y ^= (y >>> 8);
        y ^= (y << 7);
        return y;
    } 

    class Producer implements Runnable{
        public void run(){
            try{
                int seed = this.hashCode()^(int)System.nanoTime();  // 每个Producer初始化随机生成一个随机数种子
                int sum  = 0;
                barrier.await();    // 前期准备完成，等待栅栏释放(凑够2*npairs+1)线程数
                for(int i=0; i<nTrials; i++){
                    bb.put(seed);
                    sum += seed;
                    seed = xorshift(seed);
                }
                putSum.addAndGet(sum);
                barrier.await();    // 计算完成，等待栅栏释放
            } catch(Exception e){
                throw new RuntimeException(e);
            } 
        }
    }

    /**
    * 当元素进出队列时，每个线程都会更新对这些元素计算得到的校验和
    * 每个线程都拥有自己的校验和，在执行完成后在AtomicInteger上进行"汇总"
    */
    class Consumer implements Runnable{
        public void run(){
            try{
                int sum = 0;
                barrier.await();    // 前期准备完成，等待栅栏释放(凑够2*npairs+1)线程数
                for(int i=0; i<nTrials; i++){
                    sum += bb.take();
                }
                takeSum.addAndGet(sum);
                barrier.await();    // 计算完成，等待栅栏释放
            } catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    public void test(){
        try{
            for(int i=0; i<nPairs; i++){
                pool.execute(new Producer());
                pool.execute(new Consumer());
            }
            barrier.await();    // 等待所有线程就绪
            barrier.await();    // 等待所有线程执行完成
            assertEquals(putSum.get(), takeSum.get());
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }
    public static void main(String... args){
        PutTakeTest t = new PutTakeTest(15, 10, 100);
        t.test();   // 正常执行
        pool.shutdown();
        t.test();   // java.util.concurrent.RejectedExecutionException
    }
}
```
> 上例代码中，要确保测试程序能正确地测试所有要点，就一定不能让编译器可以预先猜测到校验和的值，故初始化时引入了随机初始化，并且使用了简单的伪随机数函数xorShift()。
>> 在执行过程中不始终使用随机数生成器(RNG，Random Number Generator)的原因是，由于大多数随机数生成器类都是线程安全的，并且会带来额外的`同步开销`，因此可能会在这些类与线程执行之间产生耦合，影响并行性。<p>根据系统平台不同，创建线程与启动线程等操作可能需要较大开销，先创建的线程可能拥有较大的“领先优势”。`引入CyclicBarrier是为了让线程之间的竞争更公平`，防止“领先优势”。

>要最大限度地检测出一些对执行时序敏感的数据竞争，那么`测试中的线程数量应该多于CPU数量`，这样在任意时间都会有一些线程在运行，一些线程被交换出去。

#### 12.1.5 使用回调

- 回调函数的执行通常是在对象生命周期的一些已知位置上，并且在这些位置上非常适合判断不变性条件是否被破坏。

- 通过使用自定义的线程工厂， 可以对线程的创建过程进行控制。如下例的线程工厂中可以记录已创建的线程数量。
```java
/**
 * 通过自定义线程工厂，可以对线程的创建过程进行控制
 */
class TestingThreadFactory implements ThreadFactory {
    // Implements Interface ThreadFactory must implement the inherited abstract
    // method ThreadFactory.newThread(Runnable)
    public final AtomicInteger numCreated = new AtomicInteger(); // 记录创建的线程数
    private final ThreadFactory factory = Executors.defaultThreadFactory(); // 委托defaultThreadFactory

    public Thread newThread(Runnable r) {
        numCreated.getAndIncrement();
        return factory.newThread(r);
    }
}
```

### 12.1.6 产生更多的交替操作

- 在访问共享状态的操作中，当代码在访问状态时没有使用足够的同步，将存在一些对执行时许敏感的错误，`通过在某个操作的执行过程中调用yield方法`，可以将这些错误暴露出来。
例：
```java
public synchronized void transferCredits(Account from, Account to, int amount){
    from.setBalance(from.getBalance() - amount);
    if(random.nextInt(1000) > THRESHOLD)    // random引入随机性
        Thread.yield();     
}
```

### 12.2 性能测试

- 事实上，在性能测试中应该包含一些基本的功能测试，从而确保不会对错误的代码进行性能测试。

- 理想情况下，在测试中应该反映出被测试对象在应用程序中的实际用法。(尽管通常很难)

- 性能测试的第二个目标是根据经验值来调整各种不同的限值，例如线程数量、缓存容量等。
> 这些限值可能依赖于具体平台的特性 (例如，处理器的类型、处理器的Stepping Level)、CPU的数量或内存大小等，因此需要动态地进行配置，因此需要合理地选择这些值，从而使程序能够在更多的系统上良好运行。

#### 12.2.1 在PutTakeTest中增加计时功能
```java
import java.util.concurrent.*;

/**
 * 基于栅栏的定时器，在PutTakeTest中增加计时功能
 * 这里实现一种更精确的测量方式：记录整个运行过程的时间，然后除以总操作的数量，从而得到每次操作的运行时间。
 */
public class TimePutTakeTest extends PutTakeTest {
    private BarrierTimer timer = new BarrierTimer();

    public TimePutTakeTest(int capacity, int npairs, int ntrials) {
        super(capacity, npairs, ntrials);
        barrier = new CyclicBarrier(nPairs * 2 + 1, timer); // barrier的第二个线程参数用于放开栅栏后执行这个BarrierAction(计时器开/关)
    }

    /**
     * 基于栅栏的定时器进行测试
     */
    public void test() {
        try {
            timer.clear();
            // 消费者与生产者线程各nPairs个进行ntrials次数的put/take
            for (int i = 0; i < nPairs; i++) {
                pool.execute(new Producer());
                pool.execute(new Consumer());
            }
            barrier.await();
            barrier.await();
            long nsPerItem = timer.getTime() / (nPairs * (long) nTrials);
            System.out.print("Througput: " + nsPerItem + "ns/item");
            assertEquals(putSum.get(), takeSum.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String... args) {
        // TimePutTakeTest t = new TimePutTakeTest(10, 15, 20);
        // t.test();
        int tpt = 100000; // 每个线程中的测试次数(put/take次数)
        /**
         * 设置不同的有界缓存容量大小
         */
        for (int cap = 1; cap <= 1000; cap *= 10) {
            System.out.println("Capacity: " + cap);
            for (int pair = 1; pair <= 128; pair *= 2) // 设置不同的生产者/消费者对数
            {
                TimePutTakeTest test = new TimePutTakeTest(cap, pair, tpt);
                System.out.println("Pairs: " + pair + "\t");
                test.test();
                System.out.println();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                test.test();
                System.out.println();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        pool.shutdown();
    }
}

/**
 * 栅栏计时器
 */
class BarrierTimer implements Runnable{
    private boolean started;
    private long startTime, endTime;

    public synchronized void run(){
        long t = System.nanoTime();
        if(!started){
            started = true;
            startTime = t;
        }
        else{
            endTime = t;
        }
    }

    public synchronized void clear(){
        started = false;
    }

    public synchronized long getTime(){
        return endTime-startTime;
    }
}

运行结果:
Capacity: 1
Pairs: 1
Througput: 7381ns/item
Througput: 7260ns/item
Pairs: 2
Througput: 10112ns/item
Througput: 10080ns/item
Pairs: 4
Througput: 9935ns/item
Througput: 9997ns/item
Pairs: 8
Througput: 9817ns/item
Througput: 9836ns/item
Pairs: 16
Througput: 9941ns/item
Througput: 10115ns/item
Pairs: 32
Througput: 10504ns/item
Througput: 10412ns/item
Pairs: 64
Througput: 10605ns/item
Througput: 10629ns/item
Pairs: 128
Througput: 12136ns/item
Througput: 12095ns/item
Capacity: 10
Pairs: 1
Througput: 863ns/item
Througput: 860ns/item
Pairs: 2
Througput: 1043ns/item
Througput: 1107ns/item
Pairs: 4
Througput: 1059ns/item
Througput: 1062ns/item
Pairs: 8
Througput: 1080ns/item
Througput: 1057ns/item
Pairs: 16
Througput: 1048ns/item
Througput: 1072ns/item
Pairs: 32
Througput: 1076ns/item
Througput: 1114ns/item
Pairs: 64
Througput: 1104ns/item
Througput: 1117ns/item
Pairs: 128
Througput: 1179ns/item
Througput: 1168ns/item
Capacity: 100
Pairs: 1
Througput: 254ns/item
Througput: 243ns/item
Pairs: 2
Througput: 318ns/item
Througput: 330ns/item
Pairs: 4
Througput: 372ns/item
Througput: 396ns/item
Pairs: 8
Througput: 381ns/item
Througput: 375ns/item
Pairs: 16
Througput: 368ns/item
Througput: 374ns/item
Pairs: 32
Througput: 374ns/item
Througput: 372ns/item
Pairs: 64
Througput: 397ns/item
Througput: 399ns/item
Pairs: 128
Througput: 388ns/item
Througput: 386ns/item
Capacity: 1000
Pairs: 1
Througput: 254ns/item
Througput: 249ns/item
Pairs: 2
Througput: 333ns/item
Througput: 325ns/item
Pairs: 4
Througput: 387ns/item
Througput: 405ns/item
Pairs: 8
Througput: 380ns/item
Througput: 388ns/item
Pairs: 16
Througput: 377ns/item
Througput: 371ns/item
Pairs: 32
Througput: 369ns/item
Througput: 366ns/item
Pairs: 64
Througput: 343ns/item
Througput: 353ns/item
Pairs: 128
Througput: 315ns/item
Througput: 321ns/item
```
> CyclicBarrier的另一个构造函数CyclicBarrier(int parties, Runnable barrierAction)，用于`线程到达屏障时，优先执行barrierAction`，方便处理更复杂的业务场景

我们可以从TimePutTask的运行结果中学到一些东西：
- 生产者-消费者模式在不同参数组合下的吞吐率。
- 有界缓存在不同线程数量下的可伸缩性。
- 如何选择缓存的大小。

可以看到， 当缓存大小为1时，吞吐率非常糟糕，这是因为`每个线程在阻塞并等待另一个线程之前所取得的进展是非常有限的`并且会`导致非常多的上下文切换次数`。当把缓存大小提高到10时，吞吐率得到了极大的提升：但在超过10后，所得到的收益又开始降低。

当增加更多线程时，性能却略有下降。猜测这可能跟缓存队列的实现有关，上例的缓存队列SemaphoreBoundedBuffer运行效率不高:在put和take方法中都含有多个可能发生竞争的操作，如获取一个信号量，获取一个锁以及释放信号量等。
当`缓存使用LinkedBlockingQueue`来实现，部分运行结果如下：
```java
(In PutTakeTest.java:)
private final SemaphoreBoundedBuffer<Integer> bb;
——>
private final LinkedBlockingQueue<Integer> bb;

运行结果:
Capacity: 100
Pairs: 1
Througput: 271ns/item
Througput: 274ns/item
Pairs: 2
Througput: 223ns/item
Througput: 225ns/item
Pairs: 4
Througput: 175ns/item
Througput: 182ns/item
Pairs: 8
Througput: 162ns/item
Througput: 153ns/item
Pairs: 16
Througput: 150ns/item
Througput: 149ns/item
Pairs: 32
Througput: 147ns/item
Througput: 148ns/item
Pairs: 64
Througput: 147ns/item
Througput: 145ns/item
Pairs: 128
Througput: 150ns/item
Througput: 150ns/item
```

> 这个测试在模拟应用程序时忽略了许多实际的因素，生产者/消费者无需太多工作就能生成/获取一个元素，即虽然有许多的线程，但是没有足够多的计算量，`大部分的时间都消耗在线程的阻塞和解除阻塞等操作上`,因此当增加更多线程时，也不会过多地降低性能。<p>在真实的生产者-消费者应用程序中，通常工作者线程需要通过复杂的计算来生产和获取各个元素条目，那么CPU的空闲状态将消失，并且由于线程过多而导致的影响将变得非常明显。

#### 12.2.3 响应性衡量
- 到目前为止的重点是吞吐量的测量，`这通常是并发程序最重要的性能指标`。

- 有时候还需要知道`某个动作(线程)经过多长时间才能执行完成`。这时就要测量服务时间的变化情况。(这个值是有意义的，所以有时候以更长的服务时间换更小的服务时间变动性是值得的)。

- 公平性开销主要是由于线程阻塞而造成的 (若每次操作都伴随着线程阻塞，即将缓存大小设置为1，则非公平信号量和公平信号量的执行性能基本相当)。
> 除非线程由于密集的同步需求而被持续地阻塞，否则`非公平的信号量通常能实现更好的吞吐量，而公平的信号量则实现更低的变动性`。

### 12.3 避免性能测试的陷阱
在实际情况中,开发性能测试程序必须提防多种编码陷阱，否则会使性能测试变得毫无意义。

#### 12.3.1 垃圾回收

- 垃圾回收的执行时序是无法预测的，因此在执行测试时，垃圾回收器可能在任何时刻运行。因此即使测试程序运行次数相差不大，在引入垃圾回收后(N次迭代时没有触发垃圾回收,N+1时触发)，在最终测试的每次迭代时间上会带来很大的 (`但却虚假的`)影响。

有两种策略可以防止垃圾回收操作对测试结果产生偏差。
- 确保垃圾回收操作在测试运行的整个期间都不会执行 (调用JVM时指定-verbose: gc来判断是否执行了垃圾回收)。
- 确保垃圾回收操作在测试期间执行多次，这样测试程序就能充分反映出运行期间的内存分配与垃圾回收等开销。
> 通常第二种更好，要求更长的测试时间并且更有可能反映实际环境下的性能。

#### 12.3.2 动态编译
编写动态编译语言的性能基准测试要困难得多。
- 静态编译语言: C，C++等。
- 动态编译语言: Java等。
`现代的JVM中将字节码的解释与动态编译结合起来使用`。
> 当某个类第一次被加载时，JVM会通过解释字节码的方式来执行它。`若某个时刻一个方法运行的次数足够多，那么动态编译器会将它编译为机器代码，当编译完成后，代码的执行方式将从解释执行变成直接执行`。

- 测量采用解释执行的代码速度是没有意义的，因为大多数程序在运行足够长的时间后，所有频繁执行的代码路径都会被编译。

- 如果编译器可以在测试期间运行，那么将在两方面对测试结果产生偏差:<p>1. 编译过程将消耗CPU资源。<p>2. 若测量的代码即包含解释执行的代码，又包含编译执行的代码，而编译执行时机又无法预测，那么这种混合代码得到的性能指标没有太大意义。

基于各种原因，代码还可能被:
- 反编译:退回到解释执行
- 发生重新编译。可能原因:<p>1. 加载了一个会使编译假设无效的类。<p>2. 收集了足够的分析信息后，决定采用不同的优化措施来重新编译某条代码路径等。

- 防止动态编译对测试结果产生偏差的方式：<p>1. 使程序运行足够长的时间(至少数分钟)，这样编译过程以及解释执行都只是总运行时间的很小一部分。<p>2. 使代码预先运行一段时间并且不测试这段时间内的代码性能，这样在开始计时前代码已被编译 (同理，在同一个JVM中将相同的测试运行多次，第一组结果作为“预先执行”的结果丢弃也可以验证测试方法的有效性)。
> HotSpot中，使用命令行选项`-xx: +PrintCompilation`,那么当动态编译运行时将输出一条信息，可以通过这条信息来验证动态编译是否在测试运行前。

#### 12.3.3 对代码路径的不真实采样
- 运行时编译器根据收集到的信息对已编译的代码进行优化。`JVM可以与执行过程特定的信息来生成更优的代码`。因此在编译某个程序的方法M时生成的代码可能与编译另一个不同程序中的方法M时生成的代码不同。

- JVM可能会基于一些只是临时有效的假设进行优化，并在这些假设失效时抛弃已编译的代码。

- 故测试程序需要尽量覆盖在该应用程序中将执行的代码路径集合。
> 体现在并发测试中，即使想测试单线程的性能，也应该将单线程的性能测试与多线程的性能测试结合在一起，否则动态编译器可能会针对一个单线程测试程序进行一些专门优化，但只要在真实的应用程序中包含一些并行这些优化就不复存在。

#### 12.3.4 不真实的竞争程度
- 并发应用程序可以交替执行两种不同类型的工作：<p>1. 访问共享数据 (例如从共享工作队列中取出下一个任务)。<p>2. 执行线程本地的计算。<p>根据两种不同类型工作的相关程度，在应用程序中将出现不同的竞争，并表现出不同的性能和可伸缩性。

- 要获得有实际意义的结果，`在并发程序测试中应该尽量模拟典型应用程序中的线程本地计算量以及并发协调开销`。

- 如果应用程序在每次访问共享数据结构时执行大量的线程本地计算，那么可以极大地降低竞争程度并提供更好的性能。
> 从这个角度来看，TimePutTakeTest对于某些应用程序来说可能是一种不好的模式：工作者线程没有执行太多的工作，因此吞吐量将主要受限于线程之间的协调开销，但对实际的应用程序来说并不都是如此。

#### 12.3.5 无用代码的消除
- 优化编译器能找出并消除那些不会对输出结果产生任何影响的无用代码(Dead Code)。

- 大多数情况下编译器从程序中删除无用代码都是一种优化措施，但对于基准测试程序是一个大问题，`由于基准测试程序通常不会执行任何计算，因此它们很容易在编译器的优化过程中被消除`。这将使得测试的内容变少。

- 对于`静态编译`语言中的基准测试，编译器在消除无用代码时也会存在问题，但要检测出编译器是否消除了测试基准是很容易的，可以`通过查看机器代码来发现是否缺失了部分程序，但动态编译语言要获得这种信息较困难`。
> 要编写有效的性能测试程序，就需要告诉优化器不要将基准测试当作无用代码而优化掉。这就`要求在程序中对每个计算结果都要通过某种方式来使用，这种方式不需要同步或者大量的计算`。<p>例如在PutTakeTest中，我们计算了在队列中被添加与删除的所有元素的校验和，但如果在程序中没有用到这个校验和，那么计算校验和的操作仍可能被优化。

- 有一个简单的计算技巧可以避免运算被优化掉而不会引入过高的开销:<p>`计算某个派生对象中域的散列值并将它与一个任意值进行比较，例如System。nanoTime的当前值，如果二者碰巧一样就输出一个无用并且可被忽略的消息`：
```java
/**
 * 这个比较操作
 * 1. 很少会成功
 * 2. 不会真正地执行I/O操作 (print方法中将输出结果缓存起来，当调用println时才真正输出)
 * 3. 应该是不可预测的 (System.nanoTime())，否则一个只能的动态优化编译器将用预先计算的结果来代替计算过程。(如果PutTakeTest测试程序的输入参数为静态数据，那么会受到这种优化措施影响)
 */
if(foo.x.hashCode() == System.nanoTime())
    System.out.print(" ");
```

### 12.4 其他的测试方法
- 测试的目标不是更多地发现错误，而是提高代码能按照预期方式工作的可信度。(找出所有错误是不现实的)

#### 12.4.1 代码审查

- 多人参与的代码审查通常是不可替代的。

#### 12.4.2 静态分析工具
使用静态分析工具进行正式测试与代码审查的有效补充。`这里我只列出一些在自己进行编码时应该注意的不要犯的错误`。

- `不一致的同步`<p>
- `调用Thread.run`<p>
- `未被释放的锁`<p>
- `双重检查加锁`<p>
- `在构造函数中启动一个线程`<p>
- `notify错误`<p>
- `条件等待中的错误`<p>
- `在sleep或者wait的同时持有一个锁`<p>
- `自旋循环`<p>
---
## CHAP 13 显式锁
ReentrantLock(可重入锁)并不是一种替代内置加锁的方法，而是当内置加锁机制不适用时，作为一种可选择的高级功能。

### 13.1 Lock 与 ReentrantLock
- Lock接口定义了一组抽象的加锁操作。与内置加锁机制不同，它提供了一种`无条件的、可轮询的、定时的、可中断的`锁获取操作。

- Lock的所有加锁与解锁方法都是`显式的`。
```java
public interface Lock{
    void lock();                // 无条件的
    void lockInterruptibly();   // 可中断的
    boolean tryLock();          // 可轮询的
    boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException; // 定时的
    void unlock();
    Condition newCondition();
}
```
- Lock的实现中提供了与内部锁相同的内存可见性，但在加锁语义、调度算法、顺序保证以及性能特性等方面可以有所不同。

- ReentrantLock实现了Lock接口，并提供了与synchronized相同的互斥性和内存可见性。

- 必须在finally块中释放锁，否则如果在被保护的代码中抛出了异常，那么这个锁永远都无法释放。如下例：
```java
Lock lock = new ReentrantLock();
...
lock.lock();
try{
    // 更新对象状态
    // 捕获异常，并在必要时恢复不变性条件
} finally{
    lock.unlock();
}
```
> 如果没有使用finally来释放Lock，那么相当于启动来一个定时炸弹。当发生错误时将很难追踪到最初发生错误的位置，因为没有记录应该释放锁的位置和时间。

- 在已有内置锁的情况下还要创建一种与其相似的Lock机制的原因在于内置锁存在的一些局限性：<p>1. 无法中断一个正在等待获取锁的线程。<p>2. 无法在请求获取锁时一直等待下去。<p>3. 内置锁必须在获取该锁的代码块中释放，好处是提高了安全性，简化编码，但`无法实现非阻塞结构的加锁规则` (可以通过tryLock来实现非阻塞加锁，若获取锁失败不加入阻塞队列直接跳过，可通过在外套一层循环实现轮询)。  <p>4. 某些情况下一种更灵活的加锁机制通常能提供更好的活跃性或性能。

#### 13.1.1 轮询锁与定时锁
- 可定时的与可轮询的锁获取模式是由tryLock方法实现的，相比内置锁具有更完善的错误恢复机制。

- 在内置锁中，防止死锁的唯一方法是在构造程序时出现不一致的锁顺序。`可定时的与可轮询的锁提供了另一种选择：避免死锁的发生`，如下例：
```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import static java.util.concurrent.TimeUnit.*;

/**
 * 通过tryLock来避免死锁
 * 使用tryLock来获取两个锁，如果不能同时获得，那么就回退并重新尝试
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
            NANOSECONDS.sleep(fixedDelay + rnd.nextLong()%randMod); // 休眠时间包含固定部分和随机部分，避免活锁
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
        
        Thread.sleep(2000); // 若不进行适当的等待使转账线程都运行完毕，由于读取account使没有对所有account加锁，会发生数据的不一致

        int total = 0;
        for(int i = 0; i< NUM_ACCOUNTS; i++){
            System.out.println(accounts[i].getBalance().getAmount());
            total += accounts[i].getBalance().getAmount();
        }
        System.out.println("total number is:"+total);
    }
}
```

- 在实现具有时间限制的操作时，定时锁同样非常有用(6.3.7节)。使用带有时间限制的tryLock，如果不能在指定的时间得到锁，那么程序就会提前结束。

- 定时的tryLock能够在带有时间限制的操作中实现独占加锁行为。如下例：
```java
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * 带有时间限制的加锁
 * 在Lock保护的共享通信线路上发一条消息，如果不能在指定时间内完成，代码就会失败
 */
public class TimedLocking{
    private Lock lock = new ReentrantLock();

    public boolean trySendOnSharedLine(String message, long timeout, TimeUnit unit)
    throws InterruptedException{
        long nanosToLock = unit.toNanos(timeout)-estimatedNanosToSend(message); // 由于传送消息也需时间，故最多等待锁timeout-estimateNanosTosend时间
        if(!lock.tryLock(nanosToLock, NANOSECONDS)){
            return false;
        }
        // 这里已经获得锁，进入临界区
        try{
            sendOnSharedLine(message);
        } finally{
            lock.unlock();      // 必须在finally中释放锁
        }
        return false;
    }

    private boolean sendOnSharedLine(String message){
        /* send something */
        return true;
    }

    private long estimatedNanosToSend(String message){
        return message.length();
    }
    
}
```

#### 13.1.2 可中断的锁获取操作
- 并非所有的可阻塞方法或者阻塞机制都能相应中断，如：一个线程由于执行同步的`Socket I/O`或者`等待获得内置锁`而阻塞，那么中断请求只能设置线程的中断状态，除此之外没有任何其它作用。

- Thread类内置的interrupt方法仅当下面三种情况时才会响应中断：<p>1. 当线程在调用 object.wait()、join()、sleep()等方法阻塞时受到中断，则其中断状态将被清除并收到InterruptedException<p>2. 如果线程在一个java.nio.channels.InterruptibleChannel上的I/O操作中阻塞并受到中断，将抛出java.nio.ClosedByInterryptException然后通道将被关闭，线程的中断状态将被设置<p>3. 线程在一个java.nio.channels.Selector中阻塞并受到中断，这个线程的中断状态将被设置并且从选择操作中立即返回并可能返回一个非零值，就如同调用了wakeup方法。

- 由上可见，线程在运行中收到中断的话，若想停止只能显式地在运行逻辑中加入对本线程中断状态的判断

> 若线程在执行java.io包中的同步Socket I/O时阻塞并想对其实现中断 (不属于上面任何一种)，这时就要对原始的interrupt进行封装，虽然InputStream和OutputStream中的read和write方法都不会响应中断(在此阻塞)，但通过封装一层interrupt并加入`关闭底层套接字`，可以使得由于执行read或write而被阻塞的线程抛出一个SocketException(属于IOException),代码示例见7.1.6

> 也可以采用newTaskfor将非标准的取消操作封装在一个任务中。(7.1.7节)

- 不可取消的阻塞机制将使得实现可取消的任务变得复杂。`lockInterruptibly方法能够在获取锁的同时保持对中断的响应(阻塞时仍响应中断)`，并且由于它包含在Lock中，因此无须创建其它类型的不可中断阻塞机制。
```java
/**
 *使用了lockInterruptibly来实现上例程序中的sendOnSharedLine
 * 便于在一个可取消的任务中调用它
 */
 public boolean sendOnSharedLine(String message) throws InterruptedException{
     lock.lockInterruptibly();      // 获取锁，若阻塞可响应中断
     try{
         return cancellableSendOnSharedLine(message);
     } finally{
         lock.unlock();
     }
 }

 private boolean cancellableSendOnSharedLine(String message) throws InterruptedException{
     /* send something */
     return true;
 }
```
- 需要强调的一点是，`可中断的锁获取操作只在获取锁阻塞时响应中断`。如下例：
```java
/**
 * 1. 在这个示例中主线程(父线程)先通过请求可中断获得锁并且不退出运行(无限循环)
 * 2. 父线程中构造子线程并启动，构造函数中传入父线程并由子线程中断父线程
 * 3. 结果看主线程仍然持续运行
 * 说明可中断的锁获取后在运行中不响应中断
 */
public class Test1 {
    public static void main(String... args) throws InterruptedException {
        Lock lock = new ReentrantLock();

        final class ChildThread extends Thread {
            Thread father;

            ChildThread(Thread father) {
                this.father = father;
            }

            public void run() {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                }
                father.interrupt();
            }
        }

        lock.lockInterruptibly();
        try{
            Thread t = new ChildThread(Thread.currentThread());
            t.start();
            while(true){}
        } finally{
            lock.unlock();
        }
    }
}

/**
 * 1. 在这个示例中主线程(父线程)先通过请求可中断获得锁并且不退出运行(无限循环)
 * 2. 父线程中构造子线程并启动,子线程执行可中断的锁获取操作(阻塞)，若被中断输出信息“子线程被中断”
 * 3. 在父线程中中断子线程
 * 4. 控制台输出“子线程被中断”
 * 说明可中断的锁获取后在阻塞时响应中断
 */
public class Test2 {
    public static void main(String... args) throws InterruptedException {
        Lock lock = new ReentrantLock();

        final class ChildThread extends Thread {
            Thread father;

            ChildThread(Thread father) {
                this.father = father;
            }

            public void run() {
                try {
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
            t.interrupt();
        } finally{
            lock.unlock();
        }
    }
}
```
#### 13.1.3 非块结构的加锁

- 在内置锁中，锁的获取和释放等操作都是基于代码块的——释放锁的操作总是与获取锁的操作处于同一个代码块，而不考虑控制权如何退出该代码块。

- 通过与锁分段类似的原则来降低链表中锁的粒度，为每个链表节点使用一个独立的锁，使不同的线程能独立地对链表的不同部分进行操作。具体的实现就要用到非块结构的加锁。 (不展开)

### 13.2 性能考虑因素

- Java 5.0时ReentrantLock具有比内置锁更好的性能，`Java 6后使用了改进后的算法来管理内置锁，性能已与ReentrantLock相差不大`。
> 性能是个不断变化中的指标，如果在昨天的测试基准中发现X比Y更快，那么在今天就可能已经过时了。

### 13.3 公平性

- 在ReentrantLock的构造函数中提供了两种公平性选择：<p>1. 默认创建一个非公平的锁<p>2. 创建一个公平的锁

- 公平锁：在公平锁上，线程将按照它们发出请求的顺序来获得锁

- `非公平锁`：在非公平锁上允许“插队”：当一个线程请求非公平的锁时，如果在`该线程发出请求的同时该锁状态变为可用，那么将跳过队列中所有的等待线程并获得这个锁`。(Semaphore中同样可以选择采用公平或非公平的获取顺序)
> ReentrantLock和Semaphore的实现中都继承了AQS类(AbstractQueueSynchronizer)

- 当执行加锁操作时，公平性将由于在挂起线程和恢复线程时存在的开销而极大地降低性能。
>即，在恢复一个被挂起的线程与该线程真正开始运行之间存在着严重的延迟，`若采用公平锁，那么在这一段时间内，直到在该锁的第一个排队线程恢复完成为止(一段不短的时间)，没有任何一个线程能做任何事`。而使用非公平锁，此时到达的线程可以"趁机插入"，填补无意义的CPU空置。

- 在大多数情况下，非公平锁的性能要高于公平锁。

- `当持有锁的相对时间较长，或者请求锁的平均时间间隔较长，那么应该使用公平锁` (因为恢复进程的时间开销占比就变得较小，即使利用上这一段空白也对整体吞吐量影响不大)。

- 内置加锁也并不会提供确定的公平性保证，大多数情况下，在锁实现上实现统计上的公平性保证已经足够了。

### 13.4 在synchronized和ReentrantLock之间进行选择

- ReentrantLock在加锁`和内存`上提供的语义与内置锁相同，此外它还提供了一些其它功能，包括定时的锁等待、可中断的锁等待、公平性，以及实现非块结构的加锁。

- 与显式锁相比，内置锁仍具有很大的优势。<p>1. 更多开发者熟悉，简洁紧凑，现有许多程序中都已使用。<p>2. ReentrantLock的危险性比同步机制要高，如果`忘记在finally块中调用unlock,会造成严重后果`。<p>3. 况且ReetrantLock在性能上相比synchronized已无优势。

- 仅当内置锁不能满足需求时，ReentrantLock可以作为一种高级工具。

### 13.5 读-写锁

- ReentrantLock实现了一种标准的互斥锁：每次最多只有一个线程能持有ReentrantLock。但`互斥是一种保守的加锁策略，虽然可以避免“写/写”冲突和“写/读”冲突，但同样也避免了“读/读”共享`。

- 读/写锁：一个资源可以被多个读操作访问，或者被一个写操作访问，但两者不能同时进行。

- 读-写锁是一种性能优化措施，在一些特定的情况下能实现更高的并发性。<p>1. 对于在多处理器系统上被频繁读取的数据结构，读-写锁能够提高性能。<p>2. 在其它情况下，读-写锁的性能要差一些，因为复杂性更高。

```java
/**
 * ReadWriteLock接口
 */
 public interface ReadWriteLock{
     Lock readLock();
     Lock writeLock()
 }
```

- ReentrantReadWriteLock为这两种锁都提供了可重入的加锁语义， 且<p>1. 可选是非公平的锁还是公平的锁。<p>2.公平锁中，等待时间最长的线程将优先获得锁，如果有线程持有了读锁，而另一个线程请求写入锁，那么其它线程都不能获得读取锁，直到写线程使用完并释放了写入锁。<p>非公平锁中，线程获得访问许可的顺序是不确定的。

- 当锁的持有时间较长并且大部分操作都不会修改被守护的资源时，那么读写锁能提高并发性。

下面给一个用读写锁来包装Map的示例：
```java
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
```
> 类似的也可以封装ArrayList等容器。
---
## CHAP 14 构建自定义的同步工具

### 14.4 Synchronizer剖析

- 在ReentryLock和Semaphore这两个接口之间存在许多共同点。<p>1. 这两个类都可以用作“阀门”，即每次允许一定数量的线程通过，并当线程到达阀门时，可以`通过(在调用lock或acquire时成功返回)，也可以等待(在调用lock或acquire时阻塞)，还可以取消(在调用tryLock或tryAcquire时返回“假”，表示在指定的时间内锁不可用或无法取得许可)`。<p>2. 两个接口都支持可中断的、不可中断的以及限时的获取操作，并且也都支持等待线程执行公平或非公平的队列操作。

- 实际上锁和信号量可以相互实现 (虽然在java的实现中实际上都是基于AQS)。下面是一个使用Lock来实现信号量的例子：
```java
import java.util.concurrent.locks.*;

public class SemaphoreOnLock{
    private final Lock lock = new ReentryLock();
    // 信号量的等待队列 (与lock对应)
    // CONDITION PREDICATE: permitsAvailable (permits > 0)
    private final Condition permitesAvailable = lock.newCondition();
    private int permits;            // 许可数

    public SemaphoreOnLock(int permits){
        lock.lock();
        try{
            this.permits = permits;
        } finally{
            lock.unlock();
        }
    }

    // 阻塞直到permits>0
    public void acquire(){
        lock.lock();
        try{
            while(permits<=0){
                permitesAvailable.await();
            }
        } finally{
            permits--;
            lock.unlock();
        }
    }

    public void release(){
        lock.lock();
        try{
            ++permits;          // 注意这个实现并不完美 若多次release会导致permits超出初始限定
            permitesAvailable.signal();
        } finally{
            lock.unlock();
        }
    }
}
```

- ReentryLock和Semaphore实现时都使用了共同都基类 AbstractQueuedSynchronizer(AQS)。

- AQS是一个用于构建锁和同步器的框架，许多同步器都可以通过AQS很容易并且高效地构造出来。

- 基于AQS构造都还有：<p>1. CountDownLatch<p>2. ReentrantReadWriteLock<p>3. FutureTask

> 在java 6后将基于AQS实现的SynchronousQueue替换为一个可伸缩性更高的`非阻塞`版本。

- 基于AQS来构建同步器能带来许多好处。<p>1. 极大地减少实现工作 <p>2. 不必处理在多个位置上发生的竞争问题。 (`在上例用锁实现的Semaphore中，获取许可的操作可能在两个时刻阻塞——当锁保护信号量状态时以及当许可不可用时`，而基于AQS构建的同步器中仅在许可不可用时阻塞)。<p>3. 在设计AQS时充分考虑来可伸缩性，因此JUC中所有基于AQS构建的同步器都能获得这个优势。

### 14.5 AbstractQueuedSynchronizer

- 在`基于AQS构建的同步器类`中，`最基本的操作`包括各种形式的`获取操作`和`释放操作`。获取操作是一种依赖状态的操作，并且通常会阻塞。释放并不是可阻塞的操作，当执行“释放”操作时，所有在请求时被阻塞的线程都会开始执行。

- “获取”操作在不同同步器中的含义：<p>1. 当使用锁或信号量时，指获取锁或者许可。  <p>2. 当使用CountDownLatch时，指“等待并直到闭锁到达结束状态”。 <p>3. 当使用FutureTask时，指“等待并直到任务已完成”。

- `AQS负责管理同步器类中的状态，它管理了一个整数状态信息`，可以通过getState，setState以及compareAndSetState等protected类型方法来进行操作。

- AQS管理的整数信息可表示任意状态。<p>1.ReentrantLock用它来表示所有者线程已经重复获取该锁的次数。 <p>2.Semaphore用它来表示剩余的许可数量。 <p>3.FutureTask用它来表示任务的状态(尚未开始、正在运行、已完成、已取消)。<p>4. 同步器类中还可以自行管理一些额外的状态变量，如ReentrantLock保存了锁的当前所有者信息，来区分某个获取操作是重入的还是竞争的。

- 根据同步器的不同，获取操作可以是一种`独占操作 (如ReentrantLock)`，也可以是一个`非独占操作 (如Semaphore和CountDownLatch)`。

- 以下是`AQS中获取操作和释放操作的标准形式`：
```java
boolean acquire() throws InterruptedException{
    while(当前状态不允许获取操作){
        if(需要阻塞获取请求){
            如果当前线程不在队列中，则将其插入队列
            阻塞当前线程
        }
        else
            返回失败
    }
    可能更新同步器的状态    // 获取同步器的某个线程可能会对其他线程能否也获取该同步器造成影响。例如当获取一个锁后，锁的状态将从"未被持有"变成“已被持有”，从Semaphore获取一个许可后，把剩余许可数量减一。获取闭锁的操作不会改变闭锁的状态。
    如果线程位于队列中，则将其移出队列
    返回成功
}

void release(){
    更新同步器的状态
    if(新的状态允许某个被阻塞的线程获取成功)
        解除队列中一个或多个线程的阻塞状态
}
```
- 如果某个同步器支持独占的获取操作，那么需要实现一些保护方法，包括tryAcquire、tryRelease和isHeldExclusively等。<p> 而对于支持共享获取的同步器，则应该实现tryAcquireShared和tryReleaseShared等方法。

- AQS中的acquire、acquireShared、release、releaseShared等方法都将调用这些方法在`子类`中带有前缀try的版本(`说明带有前缀try的版本需要在子类中重载实现多态`)来判断某个操作是否能执行。

- 在同步器的`子类`中，可以根据其获取操作和释放操作的语义，使用`getState、setState、compareAndSetState`来检查和更新状态，并通过返回的状态值来告知基类"获取"或“释放”同步器的操作是否成功。(例如tryAcquire返回一个负值表示获取操作失败，返回零值表示同步器通过独占方式被获取，返回正值表示同步器通过非独占方式被获取，对于tryRelease和tryReleaseShared方法来说，如果释放操作使得所有在获取同步器时被阻塞的线程恢复执行，那么这两个方法应该返回true)。

- 为了使支持条件队列的锁(如ReentrantLock)实现起来更简单，AQS还提供了一些机制来构造与同步器相关联的条件变量。

下面是`一个使用AQS实现的二元闭锁`：
```java
/**
 * 包含两个公有方法：await、signal分别对应获取、释放操作
 * 功能描述：起初闭锁关闭，任何调用await的线程都将阻塞并直到闭锁被打开，通过调用signal打开闭锁时，所有等待中的线程都将被释放，并且随后到达闭锁的线程也被允许执行。
 */
 public class OneShotLatch{
    private final Sync sync = new Sync();

    public void signal(){
        sync.releaseShared(0);
    }

    public void await() throws InterruptedException{
        sync.acquireInterruptibly(0);   // 内部实现中有一块调用了tryAcquireShared
    }

    private class Sync extends AbstractQueuedSynchronizer{
        protected int tryAcquireShared(int ignore){
            // 重写
            // 非独占方式获取
            // 当闭锁开放时(state == 1)成功，否则失败
            return (getState() == 1)? 1: -1;
        }

        protected boolean tryReleaseShared(int ignored){
            setState(1);    // 将闭锁设置为打开 (功能逻辑上自定义了state == 1为闭锁开放的逻辑)
            return true;
        }
    }
}
```
- 在上例中，AQS状态用来表示闭锁状态——关闭(0)/打开(1)。1. await方法调用AQS的acquiresSharedInterruptibly，<p>2.然后接着调用OneShotLatch中的tryAcquireShared方法。`在tryAcquireShared的实现中必须返回一个值来表示该获取操作能否执行。`<p>3. 如果之前已经打开了闭锁，那么tryAcquireShared将返回成功并允许线程通过，否则就会返回一个表示获取操作失败的值(-1)。<p>4. `acquireSharedInterruptibly方法处理失败的方式，是把这个线程放入等待线程队列中`。<p>5. 类似地，signal将调用releaseShared，接下来又会调用tryReleaseShared。<p>6. 在tryReleaseShared中将无条件地把闭锁的状态设置为打开，(通过返回值)表示该同步器处于完全被释放的状态。<p>7. `因而AQS让所有等待中的线程都尝试重新请求该同步`，并且由于tryAcquireShared将返回成功，因此现在的请求操作将成功。(`注意在调用releaseShared->调用tryReleasShared->设置AQS状态=1后，所有等待的线程尝试重新请求该同步是AQS框架内部的实现`)

- `上例的oneShotLatch也可以通过扩展AQS来实现`，而不是将一些功能委托给AQS，`但这种实现并不合理`，原因如下：<p>这样做将破坏OneShotLatch接口 (只有两个方法) 的简洁性，并且虽然AQS的公共方法不允许调用者破坏闭锁的状态，`但调用者仍可以很容易地误用它们。`

- JUC中的所有同步器类都没有直接扩展AQS，而是都将它们但相应功能委托给私有的AQS子类实现。

- `要实现一个依赖状态的类` (如果没有满足依赖状态的前提条件，那么这个类的方法必须阻塞)，最好的方式是`基于现有的库类来构建 (例如Semaphore、BlockingQueue、CountDownLatch)`。有时候`现有的库类不能提供足够的功能`，在这种情况下，可以`使用内置的条件队列、显式的Condition对象或者AQS来构建自己的同步器`。

- 内置条件队列与内置锁、显式的Condition与显式的Lock都是紧密地绑定到一起的，这是因为`管理状态依赖性的机制必须与确保状态一致性的机制关联起来`。

- 显式的Condition和显式的Lock与内置条件相比，提供来扩展的功能集：<p>1. 每个锁可对应于多个等待线程集。<p>2. 基于时限的等待。 <p>3. 可中断或不可中断的条件等待。 <p>4. 公平或非公平的队列操作等。

---
## 15. 原子变量与非阻塞同步机制

