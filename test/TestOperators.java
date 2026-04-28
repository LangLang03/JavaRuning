public class TestOperators {
    public static void main(String[] args) {
        System.out.println("=== Testing ++ and -- operators ===");
        
        int a = 5;
        System.out.println("Initial a: " + a);
        
        System.out.println("a++: " + a++);
        System.out.println("After a++: " + a);
        
        System.out.println("++a: " + ++a);
        System.out.println("After ++a: " + a);
        
        System.out.println("a--: " + a--);
        System.out.println("After a--: " + a);
        
        System.out.println("--a: " + --a);
        System.out.println("After --a: " + a);
        
        System.out.println("\n=== Testing compound assignment operators ===");
        
        int b = 10;
        System.out.println("Initial b: " + b);
        
        b += 5;
        System.out.println("After b += 5: " + b);
        
        b -= 3;
        System.out.println("After b -= 3: " + b);
        
        b *= 2;
        System.out.println("After b *= 2: " + b);
        
        b /= 4;
        System.out.println("After b /= 4: " + b);
        
        b %= 3;
        System.out.println("After b %= 3: " + b);
        
        System.out.println("\n=== Testing bitwise assignment operators ===");
        
        int c = 15;
        System.out.println("Initial c: " + c);
        
        c &= 7;
        System.out.println("After c &= 7: " + c);
        
        c |= 8;
        System.out.println("After c |= 8: " + c);
        
        c ^= 3;
        System.out.println("After c ^= 3: " + c);
        
        c <<= 2;
        System.out.println("After c <<= 2: " + c);
        
        c >>= 1;
        System.out.println("After c >>= 1: " + c);
        
        c >>>= 1;
        System.out.println("After c >>>= 1: " + c);
        
        System.out.println("\n=== Testing operators on static fields ===");
        
        TestOperators obj = new TestOperators();
        obj.testStaticOperators();
    }
    
    private static int counter = 0;
    
    public void testStaticOperators() {
        System.out.println("Initial counter: " + counter);
        
        counter++;
        System.out.println("After counter++: " + counter);
        
        ++counter;
        System.out.println("After ++counter: " + counter);
        
        counter--;
        System.out.println("After counter--: " + counter);
        
        --counter;
        System.out.println("After --counter: " + counter);
        
        counter += 10;
        System.out.println("After counter += 10: " + counter);
        
        counter -= 5;
        System.out.println("After counter -= 5: " + counter);
        
        counter *= 2;
        System.out.println("After counter *= 2: " + counter);
        
        counter /= 3;
        System.out.println("After counter /= 3: " + counter);
    }
}
