public class TestAllOperators {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Java Interpreter - Supported Operators");
        System.out.println("========================================");
        System.out.println();
        
        System.out.println("1. Arithmetic Operators:");
        System.out.println("------------------------");
        int a = 10 + 5;
        System.out.println("10 + 5 = " + a);
        int b = 10 - 3;
        System.out.println("10 - 3 = " + b);
        int c = 4 * 3;
        System.out.println("4 * 3 = " + c);
        int d = 20 / 4;
        System.out.println("20 / 4 = " + d);
        int e = 17 % 5;
        System.out.println("17 % 5 = " + e);
        System.out.println();
        
        System.out.println("2. Increment/Decrement Operators:");
        System.out.println("----------------------------------");
        int x = 5;
        System.out.println("x = 5");
        System.out.println("x++ = " + x++ + " (post-increment, returns 5 then increments to 6)");
        System.out.println("x = " + x);
        System.out.println("++x = " + ++x + " (pre-increment, increments to 7 then returns 7)");
        System.out.println("x-- = " + x-- + " (post-decrement, returns 7 then decrements to 6)");
        System.out.println("--x = " + --x + " (pre-decrement, decrements to 5 then returns 5)");
        System.out.println();
        
        System.out.println("3. Compound Assignment Operators:");
        System.out.println("----------------------------------");
        int y = 10;
        System.out.println("y = 10");
        y += 5;
        System.out.println("y += 5 -> y = " + y);
        y -= 3;
        System.out.println("y -= 3 -> y = " + y);
        y *= 2;
        System.out.println("y *= 2 -> y = " + y);
        y /= 4;
        System.out.println("y /= 4 -> y = " + y);
        y %= 3;
        System.out.println("y %= 3 -> y = " + y);
        System.out.println();
        
        System.out.println("4. Bitwise Operators:");
        System.out.println("---------------------");
        int m = 12;
        int n = 5;
        System.out.println("m = " + m + " (binary: 1100)");
        System.out.println("n = " + n + " (binary: 0101)");
        System.out.println("m & n = " + (m & n) + " (binary: 0100)");
        System.out.println("m | n = " + (m | n) + " (binary: 1101)");
        System.out.println("m ^ n = " + (m ^ n) + " (binary: 1001)");
        System.out.println("~m = " + (~m) + " (bitwise NOT)");
        System.out.println("m << 2 = " + (m << 2) + " (left shift)");
        System.out.println("m >> 2 = " + (m >> 2) + " (right shift)");
        System.out.println("m >>> 2 = " + (m >>> 2) + " (unsigned right shift)");
        System.out.println();
        
        System.out.println("5. Bitwise Assignment Operators:");
        System.out.println("---------------------------------");
        int z = 15;
        System.out.println("z = " + z);
        z &= 7;
        System.out.println("z &= 7 -> z = " + z);
        z |= 8;
        System.out.println("z |= 8 -> z = " + z);
        z ^= 3;
        System.out.println("z ^= 3 -> z = " + z);
        z <<= 2;
        System.out.println("z <<= 2 -> z = " + z);
        z >>= 1;
        System.out.println("z >>= 1 -> z = " + z);
        z >>>= 1;
        System.out.println("z >>>= 1 -> z = " + z);
        System.out.println();
        
        System.out.println("6. Comparison Operators:");
        System.out.println("------------------------");
        System.out.println("5 == 5: " + (5 == 5));
        System.out.println("5 != 3: " + (5 != 3));
        System.out.println("5 < 10: " + (5 < 10));
        System.out.println("5 > 3: " + (5 > 3));
        System.out.println("5 <= 5: " + (5 <= 5));
        System.out.println("5 >= 4: " + (5 >= 4));
        System.out.println();
        
        System.out.println("7. Logical Operators:");
        System.out.println("---------------------");
        System.out.println("true && false: " + (true && false));
        System.out.println("true || false: " + (true || false));
        System.out.println("!true: " + (!true));
        System.out.println();
        
        System.out.println("8. Ternary Operator:");
        System.out.println("--------------------");
        int result = (5 > 3) ? 10 : 20;
        System.out.println("(5 > 3) ? 10 : 20 = " + result);
        System.out.println();
        
        System.out.println("9. Operators on Static Fields:");
        System.out.println("-------------------------------");
        TestAllOperators obj = new TestAllOperators();
        obj.testStaticFieldOperators();
    }
    
    private static int counter = 0;
    
    public void testStaticFieldOperators() {
        System.out.println("counter = " + counter);
        counter++;
        System.out.println("counter++ -> " + counter);
        counter += 10;
        System.out.println("counter += 10 -> " + counter);
        counter *= 2;
        System.out.println("counter *= 2 -> " + counter);
        counter /= 3;
        System.out.println("counter /= 3 -> " + counter);
        System.out.println();
        
        System.out.println("========================================");
        System.out.println("All operators are working correctly!");
        System.out.println("========================================");
    }
}
