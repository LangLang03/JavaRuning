public class TestStaticFieldDefaultValues {
    private static int intField;
    private static long longField;
    private static double doubleField;
    private static float floatField;
    private static boolean booleanField;
    private static char charField;
    private static byte byteField;
    private static short shortField;
    private static String objectField;
    
    public static void main(String[] args) {
        System.out.println("=== Static Field Default Values ===");
        System.out.println("In Java, static fields are automatically initialized to default values if not explicitly initialized.");
        System.out.println();
        
        System.out.println("int field: " + intField + " (default: 0)");
        System.out.println("long field: " + longField + " (default: 0)");
        System.out.println("double field: " + doubleField + " (default: 0.0)");
        System.out.println("float field: " + floatField + " (default: 0.0)");
        System.out.println("boolean field: " + booleanField + " (default: false)");
        System.out.println("char field: [" + charField + "] (default: '\\u0000')");
        System.out.println("byte field: " + byteField + " (default: 0)");
        System.out.println("short field: " + shortField + " (default: 0)");
        System.out.println("String field: " + objectField + " (default: null)");
        
        System.out.println();
        System.out.println("=== Testing operations on default values ===");
        
        intField++;
        System.out.println("After intField++: " + intField);
        
        longField += 100L;
        System.out.println("After longField += 100L: " + longField);
        
        doubleField *= 5.5;
        System.out.println("After doubleField *= 5.5: " + doubleField);
        
        booleanField = !booleanField;
        System.out.println("After !booleanField: " + booleanField);
        
        System.out.println();
        System.out.println("=== Explanation ===");
        System.out.println("When you declare a static field like 'private static int counter;'");
        System.out.println("without initializing it, Java automatically assigns it the default value 0.");
        System.out.println("This is different from local variables, which must be explicitly initialized.");
        System.out.println();
        System.out.println("In the interpreter, we implement this by:");
        System.out.println("1. When a class is initialized, we check all static fields");
        System.out.println("2. If a field has an initializer (like 'private static int x = 5;'), we evaluate it");
        System.out.println("3. If a field has NO initializer, we set it to the default value based on its type:");
        System.out.println("   - int, long, byte, short: 0");
        System.out.println("   - double, float: 0.0");
        System.out.println("   - boolean: false");
        System.out.println("   - char: '\\u0000' (null character)");
        System.out.println("   - objects (String, etc.): null");
    }
}
