package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class SealedClassTest {
    
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
    public void testSealedInterfaceWithFinalSubclasses() {
        String source = "public class Test { public static void main(String[] args) { sealed interface Shape permits Circle, Rectangle {} final class Circle implements Shape {} final class Rectangle implements Shape {} System.out.println(\"sealed interface works\"); } }";
        interpreter.execute(source);
        assertEquals("sealed interface works", getOutput());
    }
    
    @Test
    public void testSealedClassWithFinalSubclasses() {
        String source = "public class Test { public static void main(String[] args) { sealed abstract class Animal permits Dog, Cat {} final class Dog extends Animal {} final class Cat extends Animal {} System.out.println(\"sealed class works\"); } }";
        interpreter.execute(source);
        assertEquals("sealed class works", getOutput());
    }
}
