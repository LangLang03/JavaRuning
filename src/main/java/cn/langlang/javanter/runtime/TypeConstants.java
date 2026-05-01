package cn.langlang.javanter.runtime;

public final class TypeConstants {
    private TypeConstants() {}
    
    public static final String INT = "int";
    public static final String LONG = "long";
    public static final String DOUBLE = "double";
    public static final String FLOAT = "float";
    public static final String BOOLEAN = "boolean";
    public static final String CHAR = "char";
    public static final String BYTE = "byte";
    public static final String SHORT = "short";
    public static final String VOID = "void";
    
    public static final String STRING = "String";
    public static final String STRING_QUALIFIED = "java.lang.String";
    public static final String OBJECT = "Object";
    public static final String OBJECT_QUALIFIED = "java.lang.Object";
    public static final String INTEGER = "Integer";
    public static final String LONG_WRAPPER = "Long";
    public static final String DOUBLE_WRAPPER = "Double";
    public static final String FLOAT_WRAPPER = "Float";
    public static final String BOOLEAN_WRAPPER = "Boolean";
    public static final String BYTE_WRAPPER = "Byte";
    public static final String SHORT_WRAPPER = "Short";
    public static final String CHARACTER = "Character";
    
    public static boolean isPrimitiveType(String typeName) {
        switch (typeName) {
            case INT:
            case LONG:
            case DOUBLE:
            case FLOAT:
            case BOOLEAN:
            case CHAR:
            case BYTE:
            case SHORT:
            case VOID:
                return true;
            default:
                return false;
        }
    }
    
    public static boolean isNumericPrimitive(String typeName) {
        switch (typeName) {
            case INT:
            case LONG:
            case DOUBLE:
            case FLOAT:
            case BYTE:
            case SHORT:
            case CHAR:
                return true;
            default:
                return false;
        }
    }
    
    public static Object getDefaultValue(String typeName) {
        switch (typeName) {
            case INT: return 0;
            case LONG: return 0L;
            case DOUBLE: return 0.0;
            case FLOAT: return 0.0f;
            case BOOLEAN: return false;
            case CHAR: return '\0';
            case BYTE: return (byte) 0;
            case SHORT: return (short) 0;
            default: return null;
        }
    }
    
    public static Class<?> getPrimitiveClass(String typeName) {
        switch (typeName) {
            case INT: return int.class;
            case LONG: return long.class;
            case DOUBLE: return double.class;
            case FLOAT: return float.class;
            case BOOLEAN: return boolean.class;
            case CHAR: return char.class;
            case BYTE: return byte.class;
            case SHORT: return short.class;
            case VOID: return void.class;
            default: return null;
        }
    }
    
    public static Class<?> getWrapperClass(String typeName) {
        switch (typeName) {
            case INT:
            case INTEGER:
                return Integer.class;
            case LONG:
            case LONG_WRAPPER:
                return Long.class;
            case DOUBLE:
            case DOUBLE_WRAPPER:
                return Double.class;
            case FLOAT:
            case FLOAT_WRAPPER:
                return Float.class;
            case BOOLEAN:
            case BOOLEAN_WRAPPER:
                return Boolean.class;
            case BYTE:
            case BYTE_WRAPPER:
                return Byte.class;
            case SHORT:
            case SHORT_WRAPPER:
                return Short.class;
            case CHAR:
            case CHARACTER:
                return Character.class;
            default:
                return null;
        }
    }
    
    public static boolean isWrapperType(String typeName) {
        return typeName.equals(INTEGER) || typeName.equals("java.lang.Integer") ||
               typeName.equals(LONG_WRAPPER) || typeName.equals("java.lang.Long") ||
               typeName.equals(DOUBLE_WRAPPER) || typeName.equals("java.lang.Double") ||
               typeName.equals(FLOAT_WRAPPER) || typeName.equals("java.lang.Float") ||
               typeName.equals(BOOLEAN_WRAPPER) || typeName.equals("java.lang.Boolean") ||
               typeName.equals(BYTE_WRAPPER) || typeName.equals("java.lang.Byte") ||
               typeName.equals(SHORT_WRAPPER) || typeName.equals("java.lang.Short") ||
               typeName.equals(CHARACTER) || typeName.equals("java.lang.Character");
    }
}
