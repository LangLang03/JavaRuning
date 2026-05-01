package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class RecordTest {
    
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
    public void testBasicRecordCreationAndAccessors() {
        String source = "public class Test { public static void main(String[] args) { record Point(int x, int y) {} Point p = new Point(10, 20); System.out.println(p.x()); System.out.println(p.y()); } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("10"), "Output was: " + output);
    }
    
    @Test
    public void testRecordWithStaticMethod() {
        String source = "public class Test { public static void main(String[] args) { record Point(int x, int y) { public static Point origin() { return new Point(0, 0); } } Point p = Point.origin(); System.out.println(p.x()); System.out.println(p.y()); } }";
        interpreter.execute(source);
        String output = getOutput();
        assertTrue(output.contains("0"), "Output was: " + output);
    }
}
