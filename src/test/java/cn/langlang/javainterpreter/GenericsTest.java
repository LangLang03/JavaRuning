package cn.langlang.javainterpreter;

import cn.langlang.javainterpreter.api.JavaInterpreter;
import cn.langlang.javainterpreter.analyzer.StaticAnalyzer;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class GenericsTest {
    
    private JavaInterpreter interpreter;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
    }
    
    @Test
    public void testSimpleGenericClass() {
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
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testGenericClassWithInteger() {
        String source = 
            "class Container<T> {" +
            "    private T data;" +
            "    public void setData(T data) { this.data = data; }" +
            "    public T getData() { return data; }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Container<Integer> c = new Container<Integer>();" +
            "        c.setData(42);" +
            "        System.out.println(c.getData());" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testMultipleTypeParameters() {
        String source = 
            "class Pair<K, V> {" +
            "    private K key;" +
            "    private V value;" +
            "    public Pair(K k, V v) { this.key = k; this.value = v; }" +
            "    public K getKey() { return key; }" +
            "    public V getValue() { return value; }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Pair<String, Integer> p = new Pair<String, Integer>(\"age\", 25);" +
            "        System.out.println(p.getKey());" +
            "        System.out.println(p.getValue());" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testBoundedTypeParameter() {
        String source = 
            "class NumberBox<T extends Number> {" +
            "    private T value;" +
            "    public void set(T v) { this.value = v; }" +
            "    public T get() { return value; }" +
            "    public double doubleValue() { return value.doubleValue(); }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        NumberBox<Integer> box = new NumberBox<Integer>();" +
            "        box.set(100);" +
            "        System.out.println(box.get());" +
            "        System.out.println(box.doubleValue());" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testGenericMethod() {
        String source = 
            "public class Test {" +
            "    public static <T> void print(T item) {" +
            "        System.out.println(item);" +
            "    }" +
            "    public static void main(String[] args) {" +
            "        print(\"Hello\");" +
            "        print(42);" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testGenericMethodWithReturn() {
        String source = 
            "public class Test {" +
            "    public static <T> T identity(T item) {" +
            "        return item;" +
            "    }" +
            "    public static void main(String[] args) {" +
            "        String s = identity(\"test\");" +
            "        System.out.println(s);" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testNestedGenericTypes() {
        String source = 
            "class Box<T> {" +
            "    private T value;" +
            "    public void set(T v) { this.value = v; }" +
            "    public T get() { return value; }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Box<Box<String>> nested = new Box<Box<String>>();" +
            "        Box<String> inner = new Box<String>();" +
            "        inner.set(\"nested\");" +
            "        nested.set(inner);" +
            "        System.out.println(nested.get().get());" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testGenericWithInterface() {
        String source = 
            "interface Container<T> {" +
            "    T getContent();" +
            "    void setContent(T content);" +
            "}" +
            "class StringContainer implements Container<String> {" +
            "    private String content;" +
            "    public String getContent() { return content; }" +
            "    public void setContent(String c) { this.content = c; }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        StringContainer sc = new StringContainer();" +
            "        sc.setContent(\"Hello Interface\");" +
            "        System.out.println(sc.getContent());" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testGenericArray() {
        String source = 
            "class ArrayWrapper<T> {" +
            "    private Object[] array = new Object[10];" +
            "    public void set(int i, T item) { array[i] = item; }" +
            "    @SuppressWarnings(\"unchecked\")" +
            "    public T get(int i) { return (T) array[i]; }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        ArrayWrapper<String> aw = new ArrayWrapper<String>();" +
            "        aw.set(0, \"test\");" +
            "        System.out.println(aw.get(0));" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testWildcardUnbounded() {
        String source = 
            "class Box<T> {" +
            "    private T value;" +
            "    public void set(T v) { this.value = v; }" +
            "    public T get() { return value; }" +
            "}" +
            "public class Test {" +
            "    public static void printBox(Box<?> box) {" +
            "        System.out.println(box.get());" +
            "    }" +
            "    public static void main(String[] args) {" +
            "        Box<String> b = new Box<String>();" +
            "        b.set(\"wildcard\");" +
            "        printBox(b);" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testGenericInheritance() {
        String source = 
            "class Box<T> {" +
            "    protected T value;" +
            "    public void set(T v) { this.value = v; }" +
            "    public T get() { return value; }" +
            "}" +
            "class StringBox extends Box<String> {" +
            "    public void append(String s) { this.value = this.value + s; }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        StringBox sb = new StringBox();" +
            "        sb.set(\"Hello\");" +
            "        sb.append(\" World\");" +
            "        System.out.println(sb.get());" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
}
