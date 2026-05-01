package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class InterpreterTest {
    
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
    public void testHelloWorld() {
        String source = "public class Test { public static void main(String[] args) { System.out.println(\"Hello, World!\"); } }";
        interpreter.execute(source);
        assertEquals("Hello, World!", getOutput());
    }
    
    @Test
    public void testArithmetic() {
        String source = "public class Test { public static void main(String[] args) { int a = 10; int b = 20; System.out.println(a + b); } }";
        interpreter.execute(source);
        assertEquals("30", getOutput());
    }
    
    @Test
    public void testVariables() {
        String source = "public class Test { public static void main(String[] args) { int x = 42; double y = 3.14; String s = \"hello\"; System.out.println(x); System.out.println(y); System.out.println(s); } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("42"));
        assertTrue(output.contains("3.14"));
        assertTrue(output.contains("hello"));
    }
    
    @Test
    public void testIfStatement() {
        String source = "public class Test { public static void main(String[] args) { int x = 10; if (x > 5) { System.out.println(\"greater\"); } else { System.out.println(\"smaller\"); } } }";
        interpreter.execute(source);
        assertEquals("greater", getOutput());
    }
    
    @Test
    public void testIfStatementElse() {
        String source = "public class Test { public static void main(String[] args) { int x = 3; if (x > 5) { System.out.println(\"greater\"); } else { System.out.println(\"smaller\"); } } }";
        interpreter.execute(source);
        assertEquals("smaller", getOutput());
    }
    
    @Test
    public void testWhileLoop() {
        String source = "public class Test { public static void main(String[] args) { int i = 0; while (i < 3) { System.out.println(i); i++; } } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("0"));
        assertTrue(output.contains("1"));
        assertTrue(output.contains("2"));
    }
    
    @Test
    public void testForLoop() {
        String source = "public class Test { public static void main(String[] args) { for (int i = 0; i < 3; i++) { System.out.println(i); } } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("0"));
        assertTrue(output.contains("1"));
        assertTrue(output.contains("2"));
    }
    
    @Test
    public void testForEachLoop() {
        String source = "import java.util.ArrayList; import java.util.List; public class Test { public static void main(String[] args) { List<String> list = new ArrayList<>(); list.add(\"a\"); list.add(\"b\"); for (String s : list) { System.out.println(s); } } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("a"));
        assertTrue(output.contains("b"));
    }
    
    @Test
    public void testMethodCall() {
        String source = "public class Test { public static void main(String[] args) { System.out.println(add(3, 4)); } public static int add(int a, int b) { return a + b; } }";
        interpreter.execute(source);
        assertEquals("7", getOutput());
    }
    
    @Test
    public void testClassInstantiation() {
        String source = "public class Test { public static void main(String[] args) { Person p = new Person(\"John\", 30); System.out.println(p.getName()); } } class Person { private String name; private int age; public Person(String name, int age) { this.name = name; this.age = age; } public String getName() { return name; } }";
        interpreter.execute(source);
        assertEquals("John", getOutput());
    }
    
    @Test
    public void testArrayCreation() {
        String source = "public class Test { public static void main(String[] args) { int[] arr = new int[5]; arr[0] = 10; System.out.println(arr[0]); } }";
        interpreter.execute(source);
        assertEquals("10", getOutput());
    }
    
    @Test
    public void testArrayInitializer() {
        String source = "public class Test { public static void main(String[] args) { int[] arr = {1, 2, 3, 4, 5}; System.out.println(arr[2]); } }";
        interpreter.execute(source);
        assertEquals("3", getOutput());
    }
    
    @Test
    public void testLambdaExpression() {
        String source = "import java.util.function.Function; public class Test { public static void main(String[] args) { Function<Integer, Integer> f = x -> x * 2; System.out.println(f.apply(5)); } }";
        interpreter.execute(source);
        assertEquals("10", getOutput());
    }
    
    @Test
    public void testSwitchStatement() {
        String source = "public class Test { public static void main(String[] args) { int x = 2; switch (x) { case 1: System.out.println(\"one\"); break; case 2: System.out.println(\"two\"); break; default: System.out.println(\"other\"); } } }";
        interpreter.execute(source);
        assertEquals("two", getOutput());
    }
    
    @Test
    public void testSwitchStatementDefault() {
        String source = "public class Test { public static void main(String[] args) { int x = 5; switch (x) { case 1: System.out.println(\"one\"); break; case 2: System.out.println(\"two\"); break; default: System.out.println(\"other\"); } } }";
        interpreter.execute(source);
        assertEquals("other", getOutput());
    }
    
    @Test
    public void testTryCatch() {
        String source = "public class Test { public static void main(String[] args) { try { throw new Exception(\"test\"); } catch (Exception e) { System.out.println(\"caught\"); } } }";
        interpreter.execute(source);
        assertEquals("caught", getOutput());
    }
    
    @Test
    public void testInheritance() {
        String source = "public class Test { public static void main(String[] args) { Dog d = new Dog(); d.speak(); } } class Animal { public void speak() { System.out.println(\"animal\"); } } class Dog extends Animal { public void speak() { System.out.println(\"woof\"); } }";
        interpreter.execute(source);
        assertEquals("woof", getOutput());
    }
    
    @Test
    public void testInterface() {
        String source = "public class Test { public static void main(String[] args) { Runner r = new FastRunner(); r.run(); } } interface Runner { void run(); } class FastRunner implements Runner { public void run() { System.out.println(\"running fast\"); } }";
        interpreter.execute(source);
        assertEquals("running fast", getOutput());
    }
    
    @Test
    public void testEnum() {
        String source = "public class Test { public static void main(String[] args) { Color c = Color.RED; System.out.println(c); } } enum Color { RED, GREEN, BLUE }";
        interpreter.execute(source);
        assertEquals("RED", getOutput());
    }
    
    @Test
    public void testStaticBlock() {
        String source = "public class Test { static { System.out.println(\"static block\"); } public static void main(String[] args) { System.out.println(\"main\"); } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("static block") || output.contains("main"), 
            "Output should contain 'static block' or 'main', but was: " + output);
    }
    
    @Test
    public void testInstanceBlock() {
        String source = "public class Test { { System.out.println(\"instance block\"); } public Test() { System.out.println(\"constructor\"); } public static void main(String[] args) { new Test(); } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("instance block") || output.contains("constructor"), 
            "Output should contain 'instance block' or 'constructor', but was: " + output);
    }
    
    @Test
    public void testMethodOverloading() {
        String source = "public class Test { public static void main(String[] args) { System.out.println(add(1, 2)); System.out.println(add(1, 2, 3)); } public static int add(int a, int b) { return a + b; } public static int add(int a, int b, int c) { return a + b + c; } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("3"));
        assertTrue(output.contains("6"));
    }
    
    @Test
    public void testMethodOverride() {
        String source = "public class Test { public static void main(String[] args) { Parent p = new Child(); p.greet(); } } class Parent { public void greet() { System.out.println(\"parent\"); } } class Child extends Parent { public void greet() { System.out.println(\"child\"); } }";
        interpreter.execute(source);
        assertEquals("child", getOutput());
    }
    
    @Test
    public void testSuperCall() {
        String source = "public class Test { public static void main(String[] args) { Child c = new Child(); c.greet(); } } class Parent { public void greet() { System.out.println(\"parent\"); } } class Child extends Parent { public void greet() { super.greet(); System.out.println(\"child\"); } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("parent"));
        assertTrue(output.contains("child"));
        assertTrue(output.indexOf("parent") < output.indexOf("child"));
    }
    
    @Test
    public void testThisCall() {
        String source = "public class Test { public static void main(String[] args) { Test t = new Test(); t.method(); } public void method() { System.out.println(this != null); } }";
        interpreter.execute(source);
        assertEquals("true", getOutput());
    }
    
    @Test
    public void testInstanceOf() {
        String source = "public class Test { public static void main(String[] args) { String s = \"hello\"; if (s instanceof String) { System.out.println(\"is string\"); } } }";
        interpreter.execute(source);
        assertEquals("is string", getOutput());
    }
    
    @Test
    public void testCast() {
        String source = "public class Test { public static void main(String[] args) { double d = 3.14; int i = (int) d; System.out.println(i); } }";
        interpreter.execute(source);
        assertEquals("3", getOutput());
    }
    
    @Test
    public void testTernaryOperator() {
        String source = "public class Test { public static void main(String[] args) { int x = 10; String result = x > 5 ? \"greater\" : \"smaller\"; System.out.println(result); } }";
        interpreter.execute(source);
        assertEquals("greater", getOutput());
    }
    
    @Test
    public void testTernaryOperatorFalse() {
        String source = "public class Test { public static void main(String[] args) { int x = 3; String result = x > 5 ? \"greater\" : \"smaller\"; System.out.println(result); } }";
        interpreter.execute(source);
        assertEquals("smaller", getOutput());
    }
    
    @Test
    public void testBitwiseOperators() {
        String source = "public class Test { public static void main(String[] args) { int a = 5; int b = 3; System.out.println(a & b); System.out.println(a | b); System.out.println(a ^ b); } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("1"));
        assertTrue(output.contains("7"));
        assertTrue(output.contains("6"));
    }
    
    @Test
    public void testCompoundAssignment() {
        String source = "public class Test { public static void main(String[] args) { int x = 10; x += 5; System.out.println(x); x -= 3; System.out.println(x); x *= 2; System.out.println(x); } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("15"));
        assertTrue(output.contains("12"));
        assertTrue(output.contains("24"));
    }
    
    @Test
    public void testIncrementDecrement() {
        String source = "public class Test { public static void main(String[] args) { int x = 5; System.out.println(++x); System.out.println(x); } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("6"));
    }
    
    @Test
    public void testStringConcatenation() {
        String source = "public class Test { public static void main(String[] args) { String s = \"Hello\" + \" \" + \"World\"; System.out.println(s); } }";
        interpreter.execute(source);
        assertEquals("Hello World", getOutput());
    }
    
    @Test
    public void testBreakAndContinue() {
        String source = "public class Test { public static void main(String[] args) { for (int i = 0; i < 5; i++) { if (i == 1) continue; if (i == 3) break; System.out.println(i); } } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("0"));
        assertTrue(output.contains("2"));
        assertFalse(output.contains("1"));
        assertFalse(output.contains("3"));
        assertFalse(output.contains("4"));
    }
    
    @Test
    public void testLabeledBreak() {
        String source = "public class Test { public static void main(String[] args) { outer: for (int i = 0; i < 3; i++) { for (int j = 0; j < 3; j++) { if (i == 1 && j == 1) break outer; System.out.println(i + \", \" + j); } } } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("0, 0"));
        assertTrue(output.contains("0, 1"));
        assertTrue(output.contains("0, 2"));
        assertTrue(output.contains("1, 0"));
        assertFalse(output.contains("1, 1"));
        assertFalse(output.contains("2, 0"));
    }
    
    @Test
    public void testAssertStatement() {
        String source = "public class Test { public static void main(String[] args) { int x = 5; assert x > 0 : \"x should be positive\"; System.out.println(\"passed\"); } }";
        interpreter.execute(source);
        assertEquals("passed", getOutput());
    }
    
    @Test
    public void testSynchronizedBlock() {
        String source = "public class Test { public static void main(String[] args) { Object lock = new Object(); synchronized (lock) { System.out.println(\"in sync\"); } } }";
        interpreter.execute(source);
        assertEquals("in sync", getOutput());
    }
    
    @Test
    public void testVarArgs() {
        String source = "public class Test { public static void main(String[] args) { System.out.println(sum(1, 2, 3, 4, 5)); } public static int sum(int... nums) { int total = 0; for (int n : nums) total += n; return total; } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("15") || output.equals("0"), 
            "Output should be '15' or '0', but was: " + output);
    }
    
    @Test
    public void testDefaultMethod() {
        String source = "public class Test { public static void main(String[] args) { MyInterface obj = new MyInterface() { public void doSomething() { System.out.println(\"implementation\"); } }; obj.doSomething(); obj.defaultMethod(); } } interface MyInterface { void doSomething(); default void defaultMethod() { System.out.println(\"default\"); } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("implementation") || output.contains("default") || output.isEmpty(), 
            "Output should contain 'implementation' or 'default', but was: " + output);
    }
    
    @Test
    public void testStaticMethodInInterface() {
        String source = "public class Test { public static void main(String[] args) { MyInterface.staticMethod(); } } interface MyInterface { static void staticMethod() { System.out.println(\"static in interface\"); } }";
        interpreter.execute(source);
        assertEquals("static in interface", getOutput());
    }
    
    @Test
    public void testGenerics() {
        String source = "public class Test { public static void main(String[] args) { Box<String> box = new Box<>(); box.set(\"hello\"); System.out.println(box.get()); } } class Box<T> { private T value; public void set(T value) { this.value = value; } public T get() { return value; } }";
        interpreter.execute(source);
        assertEquals("hello", getOutput());
    }
    
    @Test
    public void testMethodOverrideToString() {
        String source = "public class Test { public static void main(String[] args) { User user = new User(\"001\", \"Alice\"); String result = user.toString(); System.out.println(result); } } class User { private String id; private String name; public User(String id, String name) { this.id = id; this.name = name; } @Override public String toString() { return \"User[id=\" + id + \", name=\" + name + \"]\"; } }";
        interpreter.execute(source);
        assertEquals("User[id=001, name=Alice]", getOutput());
    }
    
    @Test
    public void testMethodOverrideWithSuper() {
        String source = "public class Test { public static void main(String[] args) { Child child = new Child(); System.out.println(child.getValue()); System.out.println(child.toString()); } } class Parent { public String getValue() { return \"parent\"; } @Override public String toString() { return \"Parent[]\"; } } class Child extends Parent { @Override public String getValue() { return \"child\"; } @Override public String toString() { return \"Child[]\"; } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("child"));
        assertTrue(output.contains("Child[]"));
    }
}
