package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class TextBlockTest {
    
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
    public void testBasicTextBlock() {
        String source = "public class Test { public static void main(String[] args) { var s = \"\"\"\n                hello\n                world\n                \"\"\"; System.out.println(s); } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("hello"));
        assertTrue(output.contains("world"));
    }
    
    @Test
    public void testTextBlockSingleLine() {
        String source = "public class Test { public static void main(String[] args) { var s = \"\"\"\n                simple\n                \"\"\"; System.out.println(s); } }";
        interpreter.execute(source);
        assertEquals("simple", getOutput());
    }
}
