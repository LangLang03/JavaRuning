package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class PrivateInterfaceMethodTest {
    
    private JavaInterpreter interpreter;
    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOutput;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
        originalOut = System.out;
        capturedOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOutput));
    }
    
    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }
    
    private String getOutput() {
        return capturedOutput.toString().trim();
    }
    
    @Test
    public void testPrivateInstanceMethodInInterface() {
        String source = "public class Test { public static void main(String[] args) { interface MyInterface { default void publicMethod() { helper(); } private void helper() { System.out.println(\"private method called\"); } } MyInterface obj = new MyInterface() {}; obj.publicMethod(); } }";
        interpreter.execute(source);
        assertEquals("private method called", getOutput());
    }
    
    @Test
    public void testPrivateStaticMethodInInterface() {
        String source = "public class Test { public static void main(String[] args) { interface MyInterface { static void publicStatic() { staticHelper(); } private static void staticHelper() { System.out.println(\"private static method\"); } } MyInterface.publicStatic(); } }";
        interpreter.execute(source);
        assertEquals("private static method", getOutput());
    }
}
