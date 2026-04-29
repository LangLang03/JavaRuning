package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;

public class MainTest {
    
    @Test
    public void testMainJava() throws IOException {
        JavaInterpreter interpreter = new JavaInterpreter();
        String source = "public class Main { public static void main(String[] args) { System.out.println(\"Hello from Main\"); } }";
        assertDoesNotThrow(() -> {
            interpreter.execute(source);
        }, "Should be able to execute simple program");
    }
    
    @Test
    public void testSimpleProgram() {
        JavaInterpreter interpreter = new JavaInterpreter();
        String source = "public class Test { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        
        assertDoesNotThrow(() -> {
            interpreter.execute(source);
        }, "Should be able to execute simple program");
    }
    
    @Test
    public void testArithmetic() {
        JavaInterpreter interpreter = new JavaInterpreter();
        String source = "public class Test { public static void main(String[] args) { int a = 5 + 3; System.out.println(a); } }";
        
        assertDoesNotThrow(() -> {
            interpreter.execute(source);
        }, "Should be able to execute arithmetic");
    }
    
    @Test
    public void testVariables() {
        JavaInterpreter interpreter = new JavaInterpreter();
        String source = "public class Test { public static void main(String[] args) { int x = 10; System.out.println(x); } }";
        
        assertDoesNotThrow(() -> {
            interpreter.execute(source);
        }, "Should be able to execute variables");
    }
    
    @Test
    public void testLoop() {
        JavaInterpreter interpreter = new JavaInterpreter();
        String source = "public class Test { public static void main(String[] args) { for (int i = 0; i < 3; i++) { System.out.println(i); } } }";
        
        assertDoesNotThrow(() -> {
            interpreter.execute(source);
        }, "Should be able to execute loop");
    }
    
    @Test
    public void testMethod() {
        JavaInterpreter interpreter = new JavaInterpreter();
        String source = "public class Test { public static void main(String[] args) { System.out.println(add(2, 3)); } public static int add(int a, int b) { return a + b; } }";
        
        assertDoesNotThrow(() -> {
            interpreter.execute(source);
        }, "Should be able to execute method");
    }
    
    @Test
    public void testClass() {
        JavaInterpreter interpreter = new JavaInterpreter();
        String source = "public class Test { public static void main(String[] args) { Person p = new Person(); System.out.println(p.name); } } class Person { String name = \"John\"; }";
        
        assertDoesNotThrow(() -> {
            interpreter.execute(source);
        }, "Should be able to execute class");
    }
}
