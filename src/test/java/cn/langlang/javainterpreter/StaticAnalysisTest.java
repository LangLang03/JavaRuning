package cn.langlang.javainterpreter;

import cn.langlang.javainterpreter.api.JavaInterpreter;
import cn.langlang.javainterpreter.analyzer.StaticAnalyzer;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class StaticAnalysisTest {
    
    private JavaInterpreter interpreter;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
    }
    
    @Test
    public void testLintValidCode() {
        String source = 
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        int x = 42;" +
            "        System.out.println(x);" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintUndefinedVariable() {
        String source = 
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        int x = undefinedVar;" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().get(0).getMessage().contains("Cannot resolve symbol"));
    }
    
    @Test
    public void testLintThisInStaticContext() {
        String source = 
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Object obj = this;" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().get(0).getMessage().contains("Cannot use 'this' in static context"));
    }
    
    @Test
    public void testLintSuperInStaticContext() {
        String source = 
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Object obj = super.toString();" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().get(0).getMessage().contains("Cannot use 'super' in static context"));
    }
    
    @Test
    public void testLintNonStaticFieldFromStaticContext() {
        String source = 
            "public class Test {" +
            "    private int value = 10;" +
            "    public static void main(String[] args) {" +
            "        int x = value;" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertTrue(result.hasErrors());
    }
    
    @Test
    public void testLintNonStaticMethodFromStaticContext() {
        String source = 
            "public class Test {" +
            "    public void doSomething() {}" +
            "    public static void main(String[] args) {" +
            "        doSomething();" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertTrue(result.hasErrors());
    }
    
    @Test
    public void testLintNonStaticFieldFromClass() {
        String source = 
            "public class Test {" +
            "    public int value = 10;" +
            "    public static void main(String[] args) {" +
            "        int x = Test.value;" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> 
            e.getMessage().contains("Cannot access non-static field")));
    }
    
    @Test
    public void testLintNonStaticMethodFromClass() {
        String source = 
            "public class Test {" +
            "    public void doSomething() {}" +
            "    public static void main(String[] args) {" +
            "        Test.doSomething();" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> 
            e.getMessage().contains("Cannot call non-static method")));
    }
    
    @Test
    public void testLintValidInstanceMethod() {
        String source = 
            "public class Test {" +
            "    private int value = 10;" +
            "    public void printValue() {" +
            "        System.out.println(value);" +
            "    }" +
            "    public static void main(String[] args) {" +
            "        new Test().printValue();" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintThisInInstanceMethod() {
        String source = 
            "public class Test {" +
            "    private int value = 10;" +
            "    public void printValue() {" +
            "        System.out.println(this.value);" +
            "    }" +
            "    public static void main(String[] args) {}" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintMultipleErrors() {
        String source = 
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        int x = undefinedVar1;" +
            "        int y = undefinedVar2;" +
            "        this.doSomething();" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().size() >= 2);
    }
    
    @Test
    public void testLintGenericClass() {
        String source = 
            "class Box<T> {" +
            "    private T value;" +
            "    public void set(T v) { this.value = v; }" +
            "    public T get() { return value; }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Box<String> box = new Box<String>();" +
            "        box.set(\"Hello\");" +
            "        System.out.println(box.get());" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintBoundedTypeParameter() {
        String source = 
            "class NumberBox<T extends Number> {" +
            "    private T value;" +
            "    public void set(T v) { this.value = v; }" +
            "    public T get() { return value; }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        NumberBox<Integer> box = new NumberBox<Integer>();" +
            "        box.set(100);" +
            "        System.out.println(box.get());" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintInterface() {
        String source = 
            "interface Printable {" +
            "    void print();" +
            "}" +
            "class Document implements Printable {" +
            "    public void print() { System.out.println(\"Document\"); }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Printable p = new Document();" +
            "        p.print();" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintAbstractClass() {
        String source = 
            "abstract class Shape {" +
            "    public abstract double area();" +
            "}" +
            "class Circle extends Shape {" +
            "    private double radius;" +
            "    public Circle(double r) { this.radius = r; }" +
            "    public double area() { return 3.14 * radius * radius; }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Shape s = new Circle(5.0);" +
            "        System.out.println(s.area());" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintStaticFieldAccess() {
        String source = 
            "public class Test {" +
            "    public static int count = 0;" +
            "    public static void main(String[] args) {" +
            "        Test.count++;" +
            "        System.out.println(Test.count);" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintStaticMethodCall() {
        String source = 
            "public class Test {" +
            "    public static void greet() {" +
            "        System.out.println(\"Hello\");" +
            "    }" +
            "    public static void main(String[] args) {" +
            "        Test.greet();" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintInnerClass() {
        String source = 
            "public class Test {" +
            "    class Inner {" +
            "        public void sayHello() { System.out.println(\"Hello\"); }" +
            "    }" +
            "    public static void main(String[] args) {" +
            "        Test t = new Test();" +
            "        Inner i = t.new Inner();" +
            "        i.sayHello();" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintStaticNestedClass() {
        String source = 
            "public class Test {" +
            "    static class Nested {" +
            "        public void sayHello() { System.out.println(\"Hello\"); }" +
            "    }" +
            "    public static void main(String[] args) {" +
            "        Nested n = new Nested();" +
            "        n.sayHello();" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintLocalVariable() {
        String source = 
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        int x = 10;" +
            "        int y = x + 5;" +
            "        System.out.println(y);" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintForLoopVariable() {
        String source = 
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        for (int i = 0; i < 10; i++) {" +
            "            System.out.println(i);" +
            "        }" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintForEachVariable() {
        String source = 
            "import java.util.ArrayList;" +
            "import java.util.List;" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        List<String> list = new ArrayList<String>();" +
            "        list.add(\"a\");" +
            "        for (String s : list) {" +
            "            System.out.println(s);" +
            "        }" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintLambdaParameter() {
        String source = 
            "import java.util.function.Consumer;" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Consumer<String> c = s -> System.out.println(s);" +
            "        c.accept(\"Hello\");" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testLintCatchParameter() {
        String source = 
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        try {" +
            "            throw new Exception(\"test\");" +
            "        } catch (Exception e) {" +
            "            System.out.println(e.getMessage());" +
            "        }" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
}
