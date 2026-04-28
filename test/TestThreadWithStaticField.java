public class TestThreadWithStaticField {
    private static int counter = 0;
    private static Object lock = new Object();
    
    public static void main(String[] args) {
        System.out.println("Main thread started");
        System.out.println("Initial counter: " + counter);
        
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                System.out.println("Thread 1 started");
                synchronized (lock) {
                    counter++;
                    System.out.println("Thread 1: counter = " + counter);
                }
                System.out.println("Thread 1 finished");
            }
        });
        
        Thread t2 = new Thread(() -> {
            System.out.println("Thread 2 (lambda) started");
            synchronized (lock) {
                counter++;
                System.out.println("Thread 2: counter = " + counter);
            }
            System.out.println("Thread 2 finished");
        });
        
        System.out.println("Starting threads...");
        t1.start();
        t2.start();
        
        try {
            t1.join();
            t2.join();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        
        System.out.println("Final counter: " + counter);
        System.out.println("Main thread finished");
    }
}
