package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ThreadSafetyModifierTest {
    
    private JavaInterpreter interpreter;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
    }
    
    @Test
    public void testSynchronizedMethod() {
        String source = "public class Test { private int count = 0; public synchronized void increment() { count++; } public static void main(String[] args) { Test t = new Test(); t.increment(); System.out.println(t.getCount()); } public int getCount() { return count; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedBlock() {
        String source = "public class Test { private int count = 0; private Object lock = new Object(); public void increment() { synchronized (lock) { count++; } } public static void main(String[] args) { Test t = new Test(); t.increment(); System.out.println(t.getCount()); } public int getCount() { return count; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedBlockWithThis() {
        String source = "public class Test { private int count = 0; public void increment() { synchronized (this) { count++; } } public static void main(String[] args) { Test t = new Test(); t.increment(); System.out.println(t.getCount()); } public int getCount() { return count; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testVolatileField() {
        String source = "public class Test { private volatile boolean running = true; public static void main(String[] args) { Test t = new Test(); System.out.println(t.isRunning()); t.setRunning(false); System.out.println(t.isRunning()); } public boolean isRunning() { return running; } public void setRunning(boolean running) { this.running = running; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testTransientField() {
        String source = "public class Test { private transient String tempData = \"temporary\"; private String persistentData = \"persistent\"; public static void main(String[] args) { Test t = new Test(); System.out.println(t.getTempData()); System.out.println(t.getPersistentData()); } public String getTempData() { return tempData; } public String getPersistentData() { return persistentData; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedStaticMethod() {
        String source = "public class Test { private static int count = 0; public static synchronized void increment() { count++; } public static void main(String[] args) { Test.increment(); Test.increment(); System.out.println(Test.getCount()); } public static int getCount() { return count; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testMultipleSynchronizedMethods() {
        String source = "public class Test { private int value = 0; public synchronized void add(int amount) { value += amount; } public synchronized void subtract(int amount) { value -= amount; } public synchronized int getValue() { return value; } public static void main(String[] args) { Test t = new Test(); t.add(10); t.subtract(3); System.out.println(t.getValue()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedWithThread() {
        String source = "public class Test { private int counter = 0; public synchronized void increment() { counter++; } public static void main(String[] args) { Test t = new Test(); Thread t1 = new Thread(() -> { for (int i = 0; i < 5; i++) t.increment(); }); Thread t2 = new Thread(() -> { for (int i = 0; i < 5; i++) t.increment(); }); t1.start(); t2.start(); try { t1.join(); t2.join(); } catch (Exception e) { } System.out.println(t.getCounter()); } public int getCounter() { return counter; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testVolatileWithMultipleThreads() {
        String source = "public class Test { private volatile boolean flag = false; public static void main(String[] args) { Test t = new Test(); Thread writer = new Thread(() -> { t.setFlag(true); }); Thread reader = new Thread(() -> { while (!t.isFlag()) { } System.out.println(\"flag is true\"); }); reader.start(); writer.start(); try { writer.join(); reader.join(1000); } catch (Exception e) { } } public boolean isFlag() { return flag; } public void setFlag(boolean flag) { this.flag = flag; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedBlockInLoop() {
        String source = "public class Test { private int sum = 0; private Object lock = new Object(); public void add(int value) { synchronized (lock) { sum += value; } } public static void main(String[] args) { Test t = new Test(); for (int i = 0; i < 10; i++) { t.add(i); } System.out.println(t.getSum()); } public int getSum() { return sum; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedWithException() {
        String source = "public class Test { private Object lock = new Object(); public void method() { synchronized (lock) { try { throw new Exception(\"test\"); } catch (Exception e) { System.out.println(\"caught\"); } } } public static void main(String[] args) { Test t = new Test(); t.method(); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testVolatileAndTransientTogether() {
        String source = "public class Test { private volatile int counter = 0; private transient String cache = null; public static void main(String[] args) { Test t = new Test(); t.setCounter(5); t.setCache(\"cached\"); System.out.println(t.getCounter()); System.out.println(t.getCache()); } public int getCounter() { return counter; } public void setCounter(int counter) { this.counter = counter; } public String getCache() { return cache; } public void setCache(String cache) { this.cache = cache; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedOnNullLock() {
        String source = "public class Test { private Object lock = null; public void method() { synchronized (lock) { System.out.println(\"in sync\"); } } public static void main(String[] args) { Test t = new Test(); try { t.method(); } catch (NullPointerException e) { System.out.println(\"NPE caught\"); } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedNested() {
        String source = "public class Test { private Object lock1 = new Object(); private Object lock2 = new Object(); public void nested() { synchronized (lock1) { synchronized (lock2) { System.out.println(\"nested\"); } } } public static void main(String[] args) { Test t = new Test(); t.nested(); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedWithReturn() {
        String source = "public class Test { private Object lock = new Object(); public int getValue() { synchronized (lock) { return 42; } } public static void main(String[] args) { Test t = new Test(); System.out.println(t.getValue()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedWithWaitNotify() {
        String source = "public class Test { private Object lock = new Object(); private boolean ready = false; public void waitForReady() throws InterruptedException { synchronized (lock) { while (!ready) { lock.wait(); } System.out.println(\"ready\"); } } public void setReady() { synchronized (lock) { ready = true; lock.notify(); } } public static void main(String[] args) { Test t = new Test(); Thread waiter = new Thread(() -> { try { t.waitForReady(); } catch (Exception e) { } }); Thread notifier = new Thread(() -> { try { Thread.sleep(100); t.setReady(); } catch (Exception e) { } }); waiter.start(); notifier.start(); try { waiter.join(2000); notifier.join(); } catch (Exception e) { } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testFinalVolatileField() {
        String source = "public class Test { private final volatile int value; public Test(int value) { this.value = value; } public static void main(String[] args) { Test t = new Test(100); System.out.println(t.getValue()); } public int getValue() { return value; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedInAnonymousClass() {
        String source = "public class Test { public static void main(String[] args) { Runnable r = new Runnable() { private int count = 0; public synchronized void run() { count++; System.out.println(count); } }; r.run(); r.run(); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedWithLambda() {
        String source = "public class Test { private static int counter = 0; private static Object lock = new Object(); public static void main(String[] args) { Runnable r = () -> { synchronized (lock) { counter++; System.out.println(counter); } }; r.run(); r.run(); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testTransientInSerialization() {
        String source = "public class Test { private String name = \"test\"; private transient String password = \"secret\"; public static void main(String[] args) { Test t = new Test(); System.out.println(t.getName()); System.out.println(t.getPassword()); } public String getName() { return name; } public String getPassword() { return password; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedPerformance() {
        String source = "public class Test { private int counter = 0; private Object lock = new Object(); public void increment() { synchronized (lock) { counter++; } } public static void main(String[] args) { Test t = new Test(); long start = System.currentTimeMillis(); for (int i = 0; i < 1000; i++) { t.increment(); } long end = System.currentTimeMillis(); System.out.println(t.getCounter()); System.out.println(\"Time: \" + (end - start) + \"ms\"); } public int getCounter() { return counter; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
}
