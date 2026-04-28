public class TestThreadSimple {
    public static void main(String[] args) {
        System.out.println("Main thread started");
        
        Thread t = new Thread(new Runnable() {
            public void run() {
                System.out.println("Thread is running");
                System.out.println("Thread finished");
            }
        });
        
        System.out.println("Starting thread...");
        t.start();
        
        try {
            t.join();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        
        System.out.println("Main thread finished");
    }
}
