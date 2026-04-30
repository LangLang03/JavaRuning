package cn.langlang.javanter;

import cn.langlang.javanter.api.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class LambdaExpressionTest {
    
    private JavaInterpreter interpreter;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
    }
    
    @Test
    public void testSimpleLambda() {
        String source = "import java.util.function.Function; public class Test { public static void main(String[] args) { Function<Integer, Integer> f = x -> x * 2; System.out.println(f.apply(5)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaWithThread() {
        String source = "public class Test { public static void main(String[] args) { Thread t = new Thread(() -> { System.out.println(\"lambda thread\"); }); t.start(); try { t.join(); } catch (Exception e) { } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaWithBlockBody() {
        String source = "import java.util.function.Consumer; public class Test { public static void main(String[] args) { Consumer<String> consumer = s -> { System.out.println(\"Processing: \" + s); System.out.println(\"Done\"); }; consumer.accept(\"test\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaWithMultipleParameters() {
        String source = "import java.util.function.BiFunction; public class Test { public static void main(String[] args) { BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b; System.out.println(add.apply(3, 4)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaWithForEach() {
        String source = "import java.util.ArrayList; import java.util.List; public class Test { public static void main(String[] args) { List<String> list = new ArrayList<>(); list.add(\"a\"); list.add(\"b\"); list.add(\"c\"); list.forEach(s -> System.out.println(s)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaWithStream() {
        String source = "import java.util.ArrayList; import java.util.List; import java.util.stream.Collectors; public class Test { public static void main(String[] args) { List<Integer> numbers = new ArrayList<>(); numbers.add(1); numbers.add(2); numbers.add(3); numbers.add(4); List<Integer> even = numbers.stream().filter(n -> n % 2 == 0).collect(Collectors.toList()); even.forEach(n -> System.out.println(n)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaWithReturn() {
        String source = "import java.util.function.Function; public class Test { public static void main(String[] args) { Function<Integer, Integer> square = x -> { int result = x * x; return result; }; System.out.println(square.apply(5)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaCapturingLocalVariable() {
        String source = "import java.util.function.Function; public class Test { public static void main(String[] args) { int factor = 10; Function<Integer, Integer> multiply = x -> x * factor; System.out.println(multiply.apply(5)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaAsMethodParameter() {
        String source = "import java.util.function.Consumer; public class Test { public static void main(String[] args) { process(\"hello\", s -> System.out.println(s.toUpperCase())); } public static void process(String value, Consumer<String> processor) { processor.accept(value); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaReturnedFromMethod() {
        String source = "import java.util.function.Function; public class Test { public static void main(String[] args) { Function<Integer, Integer> doubler = getDoubler(); System.out.println(doubler.apply(7)); } public static Function<Integer, Integer> getDoubler() { return x -> x * 2; } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testMultipleLambdas() {
        String source = "import java.util.function.Function; import java.util.function.Consumer; public class Test { public static void main(String[] args) { Function<Integer, Integer> addOne = x -> x + 1; Function<Integer, Integer> multiplyTwo = x -> x * 2; System.out.println(addOne.apply(5)); System.out.println(multiplyTwo.apply(5)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaWithPredicate() {
        String source = "import java.util.function.Predicate; public class Test { public static void main(String[] args) { Predicate<Integer> isEven = n -> n % 2 == 0; System.out.println(isEven.test(4)); System.out.println(isEven.test(5)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaWithSupplier() {
        String source = "import java.util.function.Supplier; public class Test { public static void main(String[] args) { Supplier<String> greeting = () -> \"Hello\"; System.out.println(greeting.get()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaWithConsumer() {
        String source = "import java.util.function.Consumer; public class Test { public static void main(String[] args) { Consumer<String> printer = s -> System.out.println(\"Printed: \" + s); printer.accept(\"test\"); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testNestedLambdas() {
        String source = "import java.util.function.Function; public class Test { public static void main(String[] args) { Function<Integer, Function<Integer, Integer>> add = x -> y -> x + y; System.out.println(add.apply(3).apply(4)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaInMap() {
        String source = "import java.util.ArrayList; import java.util.List; import java.util.stream.Collectors; public class Test { public static void main(String[] args) { List<Integer> numbers = new ArrayList<>(); numbers.add(1); numbers.add(2); numbers.add(3); List<Integer> squared = numbers.stream().map(n -> n * n).collect(Collectors.toList()); squared.forEach(n -> System.out.println(n)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaWithRunnable() {
        String source = "public class Test { public static void main(String[] args) { Runnable task = () -> System.out.println(\"running\"); task.run(); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testLambdaWithComparator() {
        String source = "import java.util.ArrayList; import java.util.List; import java.util.Comparator; public class Test { public static void main(String[] args) { List<String> names = new ArrayList<>(); names.add(\"Charlie\"); names.add(\"Alice\"); names.add(\"Bob\"); names.sort((a, b) -> a.compareTo(b)); names.forEach(n -> System.out.println(n)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
}
