package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class VarKeywordTest {
    
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
    public void testVarBasicDeclaration() {
        String source = "public class Test { public static void main(String[] args) { var x = 10; System.out.println(x); } }";
        interpreter.execute(source);
        assertEquals("10", getOutput());
    }
    
    @Test
    public void testVarWithString() {
        String source = "public class Test { public static void main(String[] args) { var s = \"hello\"; System.out.println(s); } }";
        interpreter.execute(source);
        assertEquals("hello", getOutput());
    }
    
    @Test
    public void testVarTypeInferenceFromExpression() {
        String source = "public class Test { public static void main(String[] args) { var result = 100 + 200; System.out.println(result); } }";
        interpreter.execute(source);
        assertEquals("300", getOutput());
    }
    
    @Test
    public void testVarInForLoop() {
        String source = "public class Test { public static void main(String[] args) { for (var i = 0; i < 3; i++) { System.out.print(i + \" \"); } } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.startsWith("0"));
        assertTrue(output.contains("1"));
        assertTrue(output.endsWith("2"));
    }
    
    @Test
    public void testVarInForEach() {
        String source = "public class Test { public static void main(String[] args) { int[] arr = {1, 2, 3}; for (var x : arr) { System.out.print(x + \" \"); } } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("1"), "Output was: " + output);
        assertTrue(output.contains("2"), "Output was: " + output);
        assertTrue(output.contains("3"), "Output was: " + output);
    }
    
    @Test
    public void testVarReassignment() {
        String source = "public class Test { public static void main(String[] args) { var x = 5; x = x * 3; System.out.println(x); } }";
        interpreter.execute(source);
        assertEquals("15", getOutput());
    }
}
