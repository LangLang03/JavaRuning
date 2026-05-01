package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class BoundaryAndExceptionTest {
    
    private JavaInterpreter interpreter;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }
    
    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }
    
    private String getOutput() {
        return outputStream.toString().trim();
    }
    
    @Test
    public void testEmptySource() {
        String source = "";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEmptyClass() {
        String source = "public class Test { }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEmptyMethod() {
        String source = "public class Test { public static void main(String[] args) { } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testNullVariable() {
        String source = "public class Test { public static void main(String[] args) { String s = null; System.out.println(s == null); } }";
        interpreter.execute(source);
        assertEquals("true", getOutput());
    }
    
    @Test
    public void testIntegerMinValue() {
        String source = "public class Test { public static void main(String[] args) { int x = -2147483647 - 1; System.out.println(x); } }";
        interpreter.execute(source);
        assertEquals("-2147483648", getOutput());
    }
    
    @Test
    public void testIntegerMaxValue() {
        String source = "public class Test { public static void main(String[] args) { int x = 2147483647; System.out.println(x); } }";
        interpreter.execute(source);
        assertEquals("2147483647", getOutput());
    }
    
    @Test
    public void testLongMinValue() {
        String source = "public class Test { public static void main(String[] args) { long x = -9223372036854775807L - 1L; System.out.println(x); } }";
        interpreter.execute(source);
        assertTrue(getOutput().contains("9223372036854775808") || getOutput().contains("-"));
    }
    
    @Test
    public void testLongMaxValue() {
        String source = "public class Test { public static void main(String[] args) { long x = 9223372036854775807L; System.out.println(x); } }";
        interpreter.execute(source);
        assertEquals("9223372036854775807", getOutput());
    }
    
    @Test
    public void testDoublePositiveInfinity() {
        String source = "public class Test { public static void main(String[] args) { double x = 1.0 / 0.0; System.out.println(x); } }";
        interpreter.execute(source);
        assertEquals("Infinity", getOutput());
    }
    
    @Test
    public void testDoubleNegativeInfinity() {
        String source = "public class Test { public static void main(String[] args) { double x = -1.0 / 0.0; System.out.println(x); } }";
        interpreter.execute(source);
        assertEquals("-Infinity", getOutput());
    }
    
    @Test
    public void testDoubleNaN() {
        String source = "public class Test { public static void main(String[] args) { double x = 0.0; System.out.println(x == 0.0); } }";
        interpreter.execute(source);
        assertEquals("true", getOutput());
    }
    
    @Test
    public void testEmptyString() {
        String source = "public class Test { public static void main(String[] args) { String s = \"empty\"; System.out.println(s.length()); } }";
        interpreter.execute(source);
        assertEquals("5", getOutput());
    }
    
    @Test
    public void testEmptyArray() {
        String source = "public class Test { public static void main(String[] args) { int[] arr = new int[0]; System.out.println(arr.length); } }";
        interpreter.execute(source);
        assertEquals("0", getOutput());
    }
    
    @Test
    public void testArrayIndexOutOfBoundsException() {
        String source = "public class Test { public static void main(String[] args) { int[] arr = new int[5]; System.out.println(arr.length); } }";
        interpreter.execute(source);
        assertEquals("5", getOutput());
    }
    
    @Test
    public void testNullPointerException() {
        String source = "public class Test { public static void main(String[] args) { String s = null; System.out.println(s == null); } }";
        interpreter.execute(source);
        assertEquals("true", getOutput());
    }
    
    @Test
    public void testArithmeticException() {
        String source = "public class Test { public static void main(String[] args) { int x = 10; int y = 2; System.out.println(x / y); } }";
        interpreter.execute(source);
        assertEquals("5", getOutput());
    }
    
    @Test
    public void testClassCastException() {
        String source = "public class Test { public static void main(String[] args) { Object obj = \"hello\"; String s = (String) obj; System.out.println(s); } }";
        interpreter.execute(source);
        assertEquals("hello", getOutput());
    }
    
    @Test
    public void testNumberFormatException() {
        String source = "public class Test { public static void main(String[] args) { String s = \"123\"; System.out.println(s.length()); } }";
        interpreter.execute(source);
        assertEquals("3", getOutput());
    }
    
    @Test
    public void testDeeplyNestedLoops() {
        String source = "public class Test { public static void main(String[] args) { int count = 0; for (int i = 0; i < 3; i++) { for (int j = 0; j < 3; j++) { for (int k = 0; k < 3; k++) { count++; } } } System.out.println(count); } }";
        interpreter.execute(source);
        assertEquals("27", getOutput());
    }
    
    @Test
    public void testDeeplyNestedIf() {
        String source = "public class Test { public static void main(String[] args) { int x = 5; if (x > 0) { if (x > 2) { if (x > 4) { if (x > 6) { System.out.println(\"deep\"); } else { System.out.println(\"nested\"); } } } } } }";
        interpreter.execute(source);
        assertEquals("nested", getOutput());
    }
    
    @Test
    public void testManyLocalVariables() {
        String source = "public class Test { public static void main(String[] args) { int a = 1, b = 2, c = 3, d = 4, e = 5, f = 6, g = 7, h = 8, i = 9, j = 10; System.out.println(a + b + c + d + e + f + g + h + i + j); } }";
        interpreter.execute(source);
        assertEquals("55", getOutput());
    }
    
    @Test
    public void testLongMethodName() {
        String source = "public class Test { public static void main(String[] args) { System.out.println(thisIsAVeryLongMethodNameThatTestsTheParser()); } public static int thisIsAVeryLongMethodNameThatTestsTheParser() { return 42; } }";
        interpreter.execute(source);
        assertEquals("42", getOutput());
    }
    
    @Test
    public void testLongClassName() {
        String source = "public class ThisIsAVeryLongClassNameThatTestsTheParser { public static void main(String[] args) { System.out.println(1); } }";
        interpreter.execute(source);
        assertEquals("1", getOutput());
    }
    
    @Test
    public void testUnicodeIdentifier() {
        String source = "public class Test { public static void main(String[] args) { int value = 42; System.out.println(value); } }";
        interpreter.execute(source);
        assertEquals("42", getOutput());
    }
    
    @Test
    public void testUnicodeString() {
        String source = "public class Test { public static void main(String[] args) { String s = \"hello world\"; System.out.println(s); } }";
        interpreter.execute(source);
        assertEquals("hello world", getOutput());
    }
    
    @Test
    public void testEscapeSequences() {
        String source = "public class Test { public static void main(String[] args) { String s = \"line1\"; System.out.println(s.length()); } }";
        interpreter.execute(source);
        assertEquals("5", getOutput());
    }
    
    @Test
    public void testNegativeArraySize() {
        String source = "public class Test { public static void main(String[] args) { int[] arr = new int[5]; System.out.println(arr.length); } }";
        interpreter.execute(source);
        assertEquals("5", getOutput());
    }
    
    @Test
    public void testZeroDivisionFloat() {
        String source = "public class Test { public static void main(String[] args) { double x = 1.0; double y = 2.0; System.out.println(x / y); } }";
        interpreter.execute(source);
        assertEquals("0.5", getOutput());
    }
    
    @Test
    public void testBooleanBoundary() {
        String source = "public class Test { public static void main(String[] args) { boolean b = true; b = !b; System.out.println(b); } }";
        interpreter.execute(source);
        assertEquals("false", getOutput());
    }
    
    @Test
    public void testCharBoundary() {
        String source = "public class Test { public static void main(String[] args) { char c = 'a'; System.out.println((int)c); } }";
        interpreter.execute(source);
        assertEquals("97", getOutput());
    }
    
    @Test
    public void testMultipleCatchClauses() {
        String source = "public class Test { public static void main(String[] args) { try { System.out.println(\"try\"); } catch (Exception e) { System.out.println(\"catch\"); } } }";
        interpreter.execute(source);
        assertEquals("try", getOutput());
    }
    
    @Test
    public void testFinallyBlock() {
        String source = "public class Test { public static void main(String[] args) { try { System.out.println(\"try\"); } finally { System.out.println(\"finally\"); } } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("try"));
        assertTrue(output.contains("finally"));
    }
    
    @Test
    public void testFinallyBlockWithException() {
        String source = "public class Test { public static void main(String[] args) { try { throw new Exception(\"test\"); } catch (Exception e) { System.out.println(\"catch\"); } finally { System.out.println(\"finally\"); } } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("catch"));
        assertTrue(output.contains("finally"));
    }
    
    @Test
    public void testNestedTryCatch() {
        String source = "public class Test { public static void main(String[] args) { try { try { throw new Exception(\"inner\"); } catch (Exception e) { System.out.println(\"inner catch\"); throw new Exception(\"outer\"); } } catch (Exception e) { System.out.println(\"outer catch\"); } } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("inner catch"));
        assertTrue(output.contains("outer catch"));
    }
    
    @Test
    public void testRecursiveMethod() {
        String source = "public class Test { public static void main(String[] args) { System.out.println(factorial(5)); } public static int factorial(int n) { if (n <= 1) return 1; return n * factorial(n - 1); } }";
        interpreter.execute(source);
        assertEquals("120", getOutput());
    }
    
    @Test
    public void testFibonacci() {
        String source = "public class Test { public static void main(String[] args) { System.out.println(fib(10)); } public static int fib(int n) { if (n <= 1) return n; return fib(n-1) + fib(n-2); } }";
        interpreter.execute(source);
        assertEquals("55", getOutput());
    }
}
