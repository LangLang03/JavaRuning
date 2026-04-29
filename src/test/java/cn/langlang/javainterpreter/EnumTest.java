package cn.langlang.javainterpreter;

import cn.langlang.javainterpreter.api.JavaInterpreter;
import cn.langlang.javainterpreter.analyzer.StaticAnalyzer;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class EnumTest {
    
    private JavaInterpreter interpreter;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
    }
    
    @Test
    public void testSimpleEnum() {
        String source = 
            "enum Color {" +
            "    RED, GREEN, BLUE" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Color c = Color.RED;" +
            "        System.out.println(c);" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumValues() {
        String source = 
            "enum Day {" +
            "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Day[] days = Day.values();" +
            "        System.out.println(days.length);" +
            "        for (Day d : days) {" +
            "            System.out.println(d);" +
            "        }" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumValueOf() {
        String source = 
            "enum Status {" +
            "    ACTIVE, INACTIVE, PENDING" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Status s = Status.valueOf(\"ACTIVE\");" +
            "        System.out.println(s);" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumValueOfInvalid() {
        String source = 
            "enum Status {" +
            "    ACTIVE, INACTIVE" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        try {" +
            "            Status s = Status.valueOf(\"UNKNOWN\");" +
            "        } catch (IllegalArgumentException e) {" +
            "            System.out.println(\"Caught expected exception\");" +
            "        }" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumWithConstructor() {
        String source = 
            "enum Planet {" +
            "    MERCURY(3.303e+23, 2.4397e6)," +
            "    EARTH(5.976e+24, 6.37814e6)," +
            "    JUPITER(1.9e+27, 7.1492e7);" +
            "    private final double mass;" +
            "    private final double radius;" +
            "    Planet(double mass, double radius) {" +
            "        this.mass = mass;" +
            "        this.radius = radius;" +
            "    }" +
            "    public double getMass() { return mass; }" +
            "    public double getRadius() { return radius; }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Planet p = Planet.EARTH;" +
            "        System.out.println(p.getMass());" +
            "        System.out.println(p.getRadius());" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumWithMethods() {
        String source = 
            "enum Operation {" +
            "    ADD {" +
            "        public int apply(int x, int y) { return x + y; }" +
            "    }," +
            "    SUBTRACT {" +
            "        public int apply(int x, int y) { return x - y; }" +
            "    };" +
            "    public abstract int apply(int x, int y);" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        System.out.println(Operation.ADD.apply(5, 3));" +
            "        System.out.println(Operation.SUBTRACT.apply(5, 3));" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumSwitch() {
        String source = 
            "enum Day {" +
            "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Day day = Day.WEDNESDAY;" +
            "        switch (day) {" +
            "            case MONDAY:" +
            "                System.out.println(\"Monday\");" +
            "                break;" +
            "            case WEDNESDAY:" +
            "                System.out.println(\"Wednesday\");" +
            "                break;" +
            "            default:" +
            "                System.out.println(\"Other\");" +
            "        }" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumSwitchWithMultipleCases() {
        String source = 
            "enum Color {" +
            "    RED, GREEN, BLUE" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Color c = Color.GREEN;" +
            "        switch (c) {" +
            "            case RED:" +
            "                System.out.println(\"Red\");" +
            "                break;" +
            "            case GREEN:" +
            "                System.out.println(\"Green\");" +
            "                break;" +
            "            case BLUE:" +
            "                System.out.println(\"Blue\");" +
            "                break;" +
            "        }" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumOrdinal() {
        String source = 
            "enum Season {" +
            "    SPRING, SUMMER, FALL, WINTER" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        System.out.println(Season.SPRING.ordinal());" +
            "        System.out.println(Season.SUMMER.ordinal());" +
            "        System.out.println(Season.FALL.ordinal());" +
            "        System.out.println(Season.WINTER.ordinal());" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumName() {
        String source = 
            "enum Size {" +
            "    SMALL, MEDIUM, LARGE" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Size s = Size.MEDIUM;" +
            "        System.out.println(s.name());" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumComparison() {
        String source = 
            "enum Direction {" +
            "    NORTH, SOUTH, EAST, WEST" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Direction d1 = Direction.NORTH;" +
            "        Direction d2 = Direction.NORTH;" +
            "        Direction d3 = Direction.SOUTH;" +
            "        System.out.println(d1 == d2);" +
            "        System.out.println(d1 == d3);" +
            "        System.out.println(d1.equals(d2));" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumWithFields() {
        String source = 
            "enum Currency {" +
            "    USD(\"US Dollar\")," +
            "    EUR(\"Euro\")," +
            "    GBP(\"British Pound\");" +
            "    private String name;" +
            "    Currency(String name) {" +
            "        this.name = name;" +
            "    }" +
            "    public String getCurrencyName() {" +
            "        return name;" +
            "    }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        System.out.println(Currency.USD.getCurrencyName());" +
            "        System.out.println(Currency.EUR.getCurrencyName());" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumImplementsInterface() {
        String source = 
            "interface Printable {" +
            "    void print();" +
            "}" +
            "enum Document implements Printable {" +
            "    PDF, DOC, TXT;" +
            "    public void print() {" +
            "        System.out.println(\"Document: \" + name());" +
            "    }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Document.PDF.print();" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumInClass() {
        String source = 
            "class Container {" +
            "    enum Priority {" +
            "        LOW, MEDIUM, HIGH" +
            "    }" +
            "    public static void showPriority(Priority p) {" +
            "        System.out.println(\"Priority: \" + p);" +
            "    }" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Container.Priority p = Container.Priority.HIGH;" +
            "        Container.showPriority(p);" +
            "    }" +
            "}";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testEnumStaticAnalysis() {
        String source = 
            "enum Color {" +
            "    RED, GREEN, BLUE" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Color c = Color.RED;" +
            "        System.out.println(c);" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
    
    @Test
    public void testEnumValuesStaticAnalysis() {
        String source = 
            "enum Day {" +
            "    MONDAY, TUESDAY, WEDNESDAY" +
            "}" +
            "public class Test {" +
            "    public static void main(String[] args) {" +
            "        Day[] days = Day.values();" +
            "        System.out.println(days.length);" +
            "    }" +
            "}";
        StaticAnalyzer.AnalysisResult result = interpreter.lint(source);
        assertFalse(result.hasErrors());
    }
}
