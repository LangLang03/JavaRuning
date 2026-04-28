package cn.langlang.javainterpreter;

import cn.langlang.javainterpreter.api.*;
import cn.langlang.javainterpreter.annotation.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class DataAnnotationTest {
    
    private JavaInterpreter interpreter;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
        interpreter.enableLombokStyleAnnotations();
    }
    
    @Test
    public void testDataAnnotationGetterSetter() {
        String source = "import Data; @Data class Person { private String name; private int age; } public class Test { public static void main(String[] args) { Person p = new Person(); p.setName(\"John\"); p.setAge(30); System.out.println(p.getName()); System.out.println(p.getAge()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testDataAnnotationToString() {
        String source = "import Data; import ToString; @Data @ToString class Person { private String name; private int age; } public class Test { public static void main(String[] args) { Person p = new Person(); p.setName(\"John\"); p.setAge(30); System.out.println(p.toString()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testDataAnnotationEqualsAndHashCode() {
        String source = "import Data; import EqualsAndHashCode; @Data @EqualsAndHashCode class Person { private String name; private int age; } public class Test { public static void main(String[] args) { Person p1 = new Person(); Person p2 = new Person(); System.out.println(p1.equals(p2)); System.out.println(p1.hashCode()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testNoArgsConstructor() {
        String source = "import Data; import NoArgsConstructor; @Data @NoArgsConstructor class Person { private String name; } public class Test { public static void main(String[] args) { Person p = new Person(); p.setName(\"John\"); System.out.println(p.getName()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testAllArgsConstructor() {
        String source = "import Data; import AllArgsConstructor; @Data @AllArgsConstructor class Person { private String name; private int age; } public class Test { public static void main(String[] args) { Person p = new Person(\"John\", 30); System.out.println(p.getName()); System.out.println(p.getAge()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testSimpleData() {
        String source = "import Data; @Data class User { private String name; private int age; } public class Test { public static void main(String[] args) { User u = new User(); u.setName(\"Alice\"); u.setAge(25); System.out.println(u.getName()); System.out.println(u.getAge()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
}