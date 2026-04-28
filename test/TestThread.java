public class TestThread {
    public static void main(String[] args) {
        System.out.println("Main thread started");
        
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Thread 1 is running");
                for (int i = 0; i < 5; i++) {
                    System.out.println("Thread 1: " + i);
                }
                System.out.println("Thread 1 finished");
            }
        });
        
        Thread t2 = new Thread(() -> {
            System.out.println("Thread 2 (lambda) is running");
            for (int i = 0; i < 5; i++) {
                System.out.println("Thread 2: " + i);
            }
            System.out.println("Thread 2 finished");
        });
        
        System.out.println("Starting thread 1...");
        t1.start();
        
        System.out.println("Starting thread 2...");
        t2.start();
        
        try {
            t1.join();
            t2.join();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        
        System.out.println("Main thread finished");
    }
}
