public class TestStaticField {
    private static int counter = 0;
    
    public static void main(String[] args) {
        System.out.println("Initial counter: " + counter);
        counter++;
        System.out.println("After increment: " + counter);
        counter++;
        System.out.println("After second increment: " + counter);
    }
}
