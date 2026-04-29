public class HelloTest {
    public static void main(String[] args) {
        System.out.println("Hello from HelloTest");
        int result = add(2, 3);
        System.out.println("2 + 3 = " + result);
    }
    
    public static int add(int a, int b) {
        return a + b;
    }
}
