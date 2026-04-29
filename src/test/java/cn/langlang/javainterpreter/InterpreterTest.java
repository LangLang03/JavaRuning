package cn.langlang.javainterpreter;

import cn.langlang.javainterpreter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class InterpreterTest {
    
    private JavaInterpreter interpreter;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
    }
    
    @Test
    public void testHelloWorld() {
        String source = "public class Test { public static void main(String[] args) { System.out.println(\"Hello, World!\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testArithmetic() {
        String source = "public class Test { public static void main(String[] args) { int a = 10; int b = 20; System.out.println(a + b); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testVariables() {
        String source = "public class Test { public static void main(String[] args) { int x = 42; double y = 3.14; String s = \"hello\"; System.out.println(x); System.out.println(y); System.out.println(s); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testIfStatement() {
        String source = "public class Test { public static void main(String[] args) { int x = 10; if (x > 5) { System.out.println(\"greater\"); } else { System.out.println(\"smaller\"); } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testWhileLoop() {
        String source = "public class Test { public static void main(String[] args) { int i = 0; while (i < 5) { System.out.println(i); i++; } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testForLoop() {
        String source = "public class Test { public static void main(String[] args) { for (int i = 0; i < 5; i++) { System.out.println(i); } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testForEachLoop() {
        String source = "import java.util.ArrayList; import java.util.List; public class Test { public static void main(String[] args) { List<String> list = new ArrayList<>(); list.add(\"a\"); list.add(\"b\"); for (String s : list) { System.out.println(s); } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testMethodCall() {
        String source = "public class Test { public static void main(String[] args) { System.out.println(add(3, 4)); } public static int add(int a, int b) { return a + b; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testClassInstantiation() {
        String source = "public class Test { public static void main(String[] args) { Person p = new Person(\"John\", 30); System.out.println(p.getName()); } } class Person { private String name; private int age; public Person(String name, int age) { this.name = name; this.age = age; } public String getName() { return name; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testArrayCreation() {
        String source = "public class Test { public static void main(String[] args) { int[] arr = new int[5]; arr[0] = 10; System.out.println(arr[0]); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testArrayInitializer() {
        String source = "public class Test { public static void main(String[] args) { int[] arr = {1, 2, 3, 4, 5}; System.out.println(arr[2]); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaExpression() {
        String source = "import java.util.function.Function; public class Test { public static void main(String[] args) { Function<Integer, Integer> f = x -> x * 2; System.out.println(f.apply(5)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSwitchStatement() {
        String source = "public class Test { public static void main(String[] args) { int x = 2; switch (x) { case 1: System.out.println(\"one\"); break; case 2: System.out.println(\"two\"); break; default: System.out.println(\"other\"); } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testTryCatch() {
        String source = "public class Test { public static void main(String[] args) { try { throw new Exception(\"test\"); } catch (Exception e) { System.out.println(\"caught\"); } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testInheritance() {
        String source = "public class Test { public static void main(String[] args) { Dog d = new Dog(); d.speak(); } } class Animal { public void speak() { System.out.println(\"animal\"); } } class Dog extends Animal { public void speak() { System.out.println(\"woof\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testInterface() {
        String source = "public class Test { public static void main(String[] args) { Runner r = new FastRunner(); r.run(); } } interface Runner { void run(); } class FastRunner implements Runner { public void run() { System.out.println(\"running fast\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnum() {
        String source = "public class Test { public static void main(String[] args) { Color c = Color.RED; System.out.println(c); } } enum Color { RED, GREEN, BLUE }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticBlock() {
        String source = "public class Test { static { System.out.println(\"static block\"); } public static void main(String[] args) { System.out.println(\"main\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testInstanceBlock() {
        String source = "public class Test { { System.out.println(\"instance block\"); } public Test() { System.out.println(\"constructor\"); } public static void main(String[] args) { new Test(); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testMethodOverloading() {
        String source = "public class Test { public static void main(String[] args) { System.out.println(add(1, 2)); System.out.println(add(1, 2, 3)); } public static int add(int a, int b) { return a + b; } public static int add(int a, int b, int c) { return a + b + c; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testMethodOverride() {
        String source = "public class Test { public static void main(String[] args) { Parent p = new Child(); p.greet(); } } class Parent { public void greet() { System.out.println(\"parent\"); } } class Child extends Parent { public void greet() { System.out.println(\"child\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSuperCall() {
        String source = "public class Test { public static void main(String[] args) { Child c = new Child(); c.greet(); } } class Parent { public void greet() { System.out.println(\"parent\"); } } class Child extends Parent { public void greet() { super.greet(); System.out.println(\"child\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testThisCall() {
        String source = "public class Test { public static void main(String[] args) { Test t = new Test(); t.method(); } public void method() { System.out.println(this); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testInstanceOf() {
        String source = "public class Test { public static void main(String[] args) { String s = \"hello\"; if (s instanceof String) { System.out.println(\"is string\"); } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testCast() {
        String source = "public class Test { public static void main(String[] args) { double d = 3.14; int i = (int) d; System.out.println(i); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testTernaryOperator() {
        String source = "public class Test { public static void main(String[] args) { int x = 10; String result = x > 5 ? \"greater\" : \"smaller\"; System.out.println(result); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testBitwiseOperators() {
        String source = "public class Test { public static void main(String[] args) { int a = 5; int b = 3; System.out.println(a & b); System.out.println(a | b); System.out.println(a ^ b); System.out.println(~a); System.out.println(a << 1); System.out.println(a >> 1); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testCompoundAssignment() {
        String source = "public class Test { public static void main(String[] args) { int x = 10; x += 5; System.out.println(x); x -= 3; System.out.println(x); x *= 2; System.out.println(x); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testIncrementDecrement() {
        String source = "public class Test { public static void main(String[] args) { int x = 5; System.out.println(++x); System.out.println(x++); System.out.println(x); System.out.println(--x); System.out.println(x--); System.out.println(x); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStringConcatenation() {
        String source = "public class Test { public static void main(String[] args) { String s = \"Hello\" + \" \" + \"World\"; System.out.println(s); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testBreakAndContinue() {
        String source = "public class Test { public static void main(String[] args) { for (int i = 0; i < 10; i++) { if (i == 3) continue; if (i == 7) break; System.out.println(i); } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLabeledBreak() {
        String source = "public class Test { public static void main(String[] args) { outer: for (int i = 0; i < 3; i++) { for (int j = 0; j < 3; j++) { if (i == 1 && j == 1) break outer; System.out.println(i + \", \" + j); } } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testAssertStatement() {
        String source = "public class Test { public static void main(String[] args) { int x = 5; assert x > 0 : \"x should be positive\"; System.out.println(\"passed\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSynchronizedBlock() {
        String source = "public class Test { public static void main(String[] args) { Object lock = new Object(); synchronized (lock) { System.out.println(\"in sync\"); } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testVarArgs() {
        String source = "public class Test { public static void main(String[] args) { System.out.println(sum(1, 2, 3, 4, 5)); } public static int sum(int... nums) { int total = 0; for (int n : nums) total += n; return total; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testDefaultMethod() {
        String source = "public class Test { public static void main(String[] args) { MyInterface obj = new MyInterface() { public void doSomething() { System.out.println(\"implementation\"); } }; obj.doSomething(); obj.defaultMethod(); } } interface MyInterface { void doSomething(); default void defaultMethod() { System.out.println(\"default\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticMethodInInterface() {
        String source = "public class Test { public static void main(String[] args) { MyInterface.staticMethod(); } } interface MyInterface { static void staticMethod() { System.out.println(\"static in interface\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testGenerics() {
        String source = "public class Test { public static void main(String[] args) { Box<String> box = new Box<>(); box.set(\"hello\"); System.out.println(box.get()); } } class Box<T> { private T value; public void set(T value) { this.value = value; } public T get() { return value; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testMethodOverrideToString() {
        String source = "public class Test { public static void main(String[] args) { User user = new User(\"001\", \"Alice\"); String result = user.toString(); System.out.println(result); } } class User { private String id; private String name; public User(String id, String name) { this.id = id; this.name = name; } @Override public String toString() { return \"User[id=\" + id + \", name=\" + name + \"]\"; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testMethodOverrideWithSuper() {
        String source = "public class Test { public static void main(String[] args) { Child child = new Child(); System.out.println(child.getValue()); System.out.println(child.toString()); } } class Parent { public String getValue() { return \"parent\"; } @Override public String toString() { return \"Parent[]\"; } } class Child extends Parent { @Override public String getValue() { return \"child\"; } @Override public String toString() { return \"Child[]\"; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
}
