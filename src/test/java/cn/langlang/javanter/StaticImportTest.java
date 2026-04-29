package cn.langlang.javanter;

import cn.langlang.javanter.api.JavaInterpreter;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class StaticImportTest {
    
    private JavaInterpreter interpreter;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
    }
    
    @Test
    public void testStaticImportMathConstants() {
        String source = "import static java.lang.Math.PI; import static java.lang.Math.E; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(PI); System.out.println(E); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportMathMethods() {
        String source = "import static java.lang.Math.sqrt; import static java.lang.Math.abs; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(sqrt(16.0)); System.out.println(abs(-5)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportSystemOut() {
        String source = "import static java.lang.System.out; " +
            "public class Test { public static void main(String[] args) { " +
            "out.println(\"Hello from static import\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportAsterisk() {
        String source = "import static java.lang.Math.*; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(PI); System.out.println(sqrt(25.0)); System.out.println(max(3, 7)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportNotInHardcodedList() {
        String source = "import static java.lang.Math.sin; import static java.lang.Math.cos; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(sin(0.0)); System.out.println(cos(0.0)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportLogAndExp() {
        String source = "import static java.lang.Math.log; import static java.lang.Math.exp; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(log(2.718281828)); System.out.println(exp(1.0)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportFloorAndCeil() {
        String source = "import static java.lang.Math.floor; import static java.lang.Math.ceil; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(floor(3.7)); System.out.println(ceil(3.2)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportRound() {
        String source = "import static java.lang.Math.round; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(round(3.5)); System.out.println(round(3.4)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportRandom() {
        String source = "import static java.lang.Math.random; " +
            "public class Test { public static void main(String[] args) { " +
            "double r = random(); System.out.println(r >= 0.0 && r < 1.0); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportSystemNanoTime() {
        String source = "import static java.lang.System.nanoTime; " +
            "public class Test { public static void main(String[] args) { " +
            "long time = nanoTime(); System.out.println(time > 0); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportSystemCurrentTimeMillis() {
        String source = "import static java.lang.System.currentTimeMillis; " +
            "public class Test { public static void main(String[] args) { " +
            "long time = currentTimeMillis(); System.out.println(time > 0); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportMinAndMax() {
        String source = "import static java.lang.Math.min; import static java.lang.Math.max; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(min(5, 3)); System.out.println(max(5, 3)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testStaticImportPow() {
        String source = "import static java.lang.Math.pow; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(pow(2.0, 10.0)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
}
