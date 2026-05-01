package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class InstanceofPatternTest {
    
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
    public void testBasicPatternMatchingString() {
        String source = "public class Test { public static void main(String[] args) { Object obj = \"hello\"; if (obj instanceof String s) { System.out.println(s.length()); } } }";
        interpreter.execute(source);
        assertEquals("5", getOutput());
    }
    
    @Test
    public void testPatternMatchingVariableScope() {
        String source = "public class Test { public static void main(String[] args) { Object obj = 42; if (obj instanceof Integer i) { System.out.println(i * 2); } else { System.out.println(\"not int\"); } } }";
        interpreter.execute(source);
        assertEquals("84", getOutput());
    }
    
    @Test
    public void testPatternMatchingNoMatch() {
        String source = "public class Test { public static void main(String[] args) { Object obj = \"test\"; if (obj instanceof Integer i) { System.out.println(i); } else { System.out.println(\"no match\"); } } }";
        interpreter.execute(source);
        assertEquals("no match", getOutput());
    }
}
