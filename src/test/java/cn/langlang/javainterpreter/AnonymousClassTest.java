package cn.langlang.javainterpreter;

import cn.langlang.javainterpreter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class AnonymousClassTest {
    
    private JavaInterpreter interpreter;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
    }
    
    @Test
    public void testAnonymousRunnable() {
        String source = "public class Test { public static void main(String[] args) { Runnable r = new Runnable() { public void run() { System.out.println(\"running\"); } }; r.run(); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testAnonymousThread() {
        String source = "public class Test { public static void main(String[] args) { Thread t = new Thread() { public void run() { System.out.println(\"thread running\"); } }; t.start(); try { t.join(); } catch (Exception e) { } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testAnonymousInterface() {
        String source = "public class Test { public static void main(String[] args) { Calculator calc = new Calculator() { public int calculate(int a, int b) { return a + b; } }; System.out.println(calc.calculate(5, 3)); } } interface Calculator { int calculate(int a, int b); }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testAnonymousClassWithFields() {
        String source = "public class Test { public static void main(String[] args) { DataHolder holder = new DataHolder() { private int value = 42; public int getValue() { return value; } }; System.out.println(holder.getValue()); } } interface DataHolder { int getValue(); }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testAnonymousClassWithMultipleMethods() {
        String source = "public class Test { public static void main(String[] args) { MultiMethod obj = new MultiMethod() { public void method1() { System.out.println(\"method1\"); } public void method2() { System.out.println(\"method2\"); } }; obj.method1(); obj.method2(); } } interface MultiMethod { void method1(); void method2(); }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testAnonymousClassWithConstructor() {
        String source = "public class Test { public static void main(String[] args) { Thread t = new Thread(\"MyThread\") { public void run() { System.out.println(\"named thread\"); } }; t.start(); try { t.join(); } catch (Exception e) { } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testNestedAnonymousClass() {
        String source = "public class Test { public static void main(String[] args) { OuterInterface outer = new OuterInterface() { public void outerMethod() { System.out.println(\"outer\"); } public InnerInterface getInner() { return new InnerInterface() { public void innerMethod() { System.out.println(\"inner\"); } }; } }; outer.outerMethod(); outer.getInner().innerMethod(); } } interface OuterInterface { void outerMethod(); InnerInterface getInner(); } interface InnerInterface { void innerMethod(); }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testAnonymousClassAccessingLocalVariables() {
        String source = "public class Test { public static void main(String[] args) { int x = 10; String msg = \"hello\"; Printer printer = new Printer() { public void print() { System.out.println(x + \" \" + msg); } }; printer.print(); } } interface Printer { void print(); }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testAnonymousClassWithInitializerBlock() {
        String source = "public class Test { public static void main(String[] args) { InitializerTest obj = new InitializerTest() { { System.out.println(\"initializer\"); } public void test() { System.out.println(\"test\"); } }; obj.test(); } } interface InitializerTest { void test(); }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testMultipleAnonymousClassInstances() {
        String source = "public class Test { public static void main(String[] args) { for (int i = 0; i < 3; i++) { Runnable r = new Runnable() { public void run() { System.out.println(\"instance\"); } }; r.run(); } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
}
