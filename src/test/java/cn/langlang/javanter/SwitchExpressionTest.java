package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class SwitchExpressionTest {
    
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
    public void testSwitchExpressionBasic() {
        String source = "public class Test { public static void main(String[] args) { var day = 1; var result = switch (day) { case 1 -> \"Monday\"; case 2 -> \"Tuesday\"; default -> \"Other\"; }; System.out.println(result); } }";
        interpreter.execute(source);
        assertEquals("Monday", getOutput());
    }
    
    @Test
    public void testSwitchExpressionDefaultCase() {
        String source = "public class Test { public static void main(String[] args) { var day = 99; var result = switch (day) { case 1 -> \"Monday\"; case 2 -> \"Tuesday\"; default -> \"Other\"; }; System.out.println(result); } }";
        interpreter.execute(source);
        assertEquals("Other", getOutput());
    }
    
    @Test
    public void testSwitchExpressionMultipleCases() {
        String source = "public class Test { public static void main(String[] args) { var day = 3; var result = switch (day) { case 1, 2, 3, 4, 5 -> \"Workday\"; case 6, 7 -> \"Weekend\"; default -> \"Invalid\"; }; System.out.println(result); } }";
        interpreter.execute(source);
        assertEquals("Workday", getOutput());
    }
    
    @Test
    public void testSwitchStatementArrowSyntax() {
        String source = "public class Test { public static void main(String[] args) { int x = 1; switch (x) { case 1 -> System.out.println(\"one\"); case 2 -> System.out.println(\"two\"); default -> System.out.println(\"other\"); } } }";
        interpreter.execute(source);
        assertEquals("one", getOutput());
    }
    
    @Test
    public void testSwitchStatementArrowSyntaxDefault() {
        String source = "public class Test { public static void main(String[] args) { int x = 99; switch (x) { case 1 -> System.out.println(\"one\"); case 2 -> System.out.println(\"two\"); default -> System.out.println(\"other\"); } } }";
        interpreter.execute(source);
        assertEquals("other", getOutput());
    }
}
