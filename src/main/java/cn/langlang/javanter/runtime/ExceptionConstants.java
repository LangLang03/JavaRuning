package cn.langlang.javanter.runtime;

import java.util.HashMap;
import java.util.Map;

public final class ExceptionConstants {
    private ExceptionConstants() {}
    
    public static final String EXCEPTION = "Exception";
    public static final String EXCEPTION_QUALIFIED = "java.lang.Exception";
    public static final String RUNTIME_EXCEPTION = "RuntimeException";
    public static final String RUNTIME_EXCEPTION_QUALIFIED = "java.lang.RuntimeException";
    public static final String NULL_POINTER_EXCEPTION = "NullPointerException";
    public static final String NULL_POINTER_EXCEPTION_QUALIFIED = "java.lang.NullPointerException";
    public static final String ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION = "ArrayIndexOutOfBoundsException";
    public static final String ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION_QUALIFIED = "java.lang.ArrayIndexOutOfBoundsException";
    public static final String INDEX_OUT_OF_BOUNDS_EXCEPTION = "IndexOutOfBoundsException";
    public static final String INDEX_OUT_OF_BOUNDS_EXCEPTION_QUALIFIED = "java.lang.IndexOutOfBoundsException";
    public static final String CLASS_CAST_EXCEPTION = "ClassCastException";
    public static final String CLASS_CAST_EXCEPTION_QUALIFIED = "java.lang.ClassCastException";
    public static final String NUMBER_FORMAT_EXCEPTION = "NumberFormatException";
    public static final String NUMBER_FORMAT_EXCEPTION_QUALIFIED = "java.lang.NumberFormatException";
    public static final String ILLEGAL_ARGUMENT_EXCEPTION = "IllegalArgumentException";
    public static final String ILLEGAL_ARGUMENT_EXCEPTION_QUALIFIED = "java.lang.IllegalArgumentException";
    public static final String ILLEGAL_STATE_EXCEPTION = "IllegalStateException";
    public static final String ILLEGAL_STATE_EXCEPTION_QUALIFIED = "java.lang.IllegalStateException";
    public static final String ARITHMETIC_EXCEPTION = "ArithmeticException";
    public static final String ARITHMETIC_EXCEPTION_QUALIFIED = "java.lang.ArithmeticException";
    public static final String UNSUPPORTED_OPERATION_EXCEPTION = "UnsupportedOperationException";
    public static final String UNSUPPORTED_OPERATION_EXCEPTION_QUALIFIED = "java.lang.UnsupportedOperationException";
    public static final String NEGATIVE_ARRAY_SIZE_EXCEPTION = "NegativeArraySizeException";
    public static final String NEGATIVE_ARRAY_SIZE_EXCEPTION_QUALIFIED = "java.lang.NegativeArraySizeException";
    public static final String ARRAY_STORE_EXCEPTION = "ArrayStoreException";
    public static final String ARRAY_STORE_EXCEPTION_QUALIFIED = "java.lang.ArrayStoreException";
    public static final String ILLEGAL_ACCESS_EXCEPTION = "IllegalAccessException";
    public static final String ILLEGAL_ACCESS_EXCEPTION_QUALIFIED = "java.lang.IllegalAccessException";
    public static final String INVOCATION_TARGET_EXCEPTION = "InvocationTargetException";
    public static final String INVOCATION_TARGET_EXCEPTION_QUALIFIED = "java.lang.reflect.InvocationTargetException";
    public static final String INTERRUPTED_EXCEPTION = "InterruptedException";
    public static final String INTERRUPTED_EXCEPTION_QUALIFIED = "java.lang.InterruptedException";
    public static final String IO_EXCEPTION = "IOException";
    public static final String IO_EXCEPTION_QUALIFIED = "java.io.IOException";
    public static final String FILE_NOT_FOUND_EXCEPTION = "FileNotFoundException";
    public static final String FILE_NOT_FOUND_EXCEPTION_QUALIFIED = "java.io.FileNotFoundException";
    public static final String THROWABLE = "Throwable";
    public static final String THROWABLE_QUALIFIED = "java.lang.Throwable";
    public static final String ERROR = "Error";
    public static final String ERROR_QUALIFIED = "java.lang.Error";
    
    private static final Map<String, Class<?>> EXCEPTION_CLASSES = new HashMap<>();
    
    static {
        EXCEPTION_CLASSES.put(EXCEPTION, Exception.class);
        EXCEPTION_CLASSES.put(EXCEPTION_QUALIFIED, Exception.class);
        EXCEPTION_CLASSES.put(RUNTIME_EXCEPTION, RuntimeException.class);
        EXCEPTION_CLASSES.put(RUNTIME_EXCEPTION_QUALIFIED, RuntimeException.class);
        EXCEPTION_CLASSES.put(NULL_POINTER_EXCEPTION, NullPointerException.class);
        EXCEPTION_CLASSES.put(NULL_POINTER_EXCEPTION_QUALIFIED, NullPointerException.class);
        EXCEPTION_CLASSES.put(ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION, ArrayIndexOutOfBoundsException.class);
        EXCEPTION_CLASSES.put(ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION_QUALIFIED, ArrayIndexOutOfBoundsException.class);
        EXCEPTION_CLASSES.put(INDEX_OUT_OF_BOUNDS_EXCEPTION, IndexOutOfBoundsException.class);
        EXCEPTION_CLASSES.put(INDEX_OUT_OF_BOUNDS_EXCEPTION_QUALIFIED, IndexOutOfBoundsException.class);
        EXCEPTION_CLASSES.put(CLASS_CAST_EXCEPTION, ClassCastException.class);
        EXCEPTION_CLASSES.put(CLASS_CAST_EXCEPTION_QUALIFIED, ClassCastException.class);
        EXCEPTION_CLASSES.put(NUMBER_FORMAT_EXCEPTION, NumberFormatException.class);
        EXCEPTION_CLASSES.put(NUMBER_FORMAT_EXCEPTION_QUALIFIED, NumberFormatException.class);
        EXCEPTION_CLASSES.put(ILLEGAL_ARGUMENT_EXCEPTION, IllegalArgumentException.class);
        EXCEPTION_CLASSES.put(ILLEGAL_ARGUMENT_EXCEPTION_QUALIFIED, IllegalArgumentException.class);
        EXCEPTION_CLASSES.put(ILLEGAL_STATE_EXCEPTION, IllegalStateException.class);
        EXCEPTION_CLASSES.put(ILLEGAL_STATE_EXCEPTION_QUALIFIED, IllegalStateException.class);
        EXCEPTION_CLASSES.put(ARITHMETIC_EXCEPTION, ArithmeticException.class);
        EXCEPTION_CLASSES.put(ARITHMETIC_EXCEPTION_QUALIFIED, ArithmeticException.class);
        EXCEPTION_CLASSES.put(UNSUPPORTED_OPERATION_EXCEPTION, UnsupportedOperationException.class);
        EXCEPTION_CLASSES.put(UNSUPPORTED_OPERATION_EXCEPTION_QUALIFIED, UnsupportedOperationException.class);
        EXCEPTION_CLASSES.put(NEGATIVE_ARRAY_SIZE_EXCEPTION, NegativeArraySizeException.class);
        EXCEPTION_CLASSES.put(NEGATIVE_ARRAY_SIZE_EXCEPTION_QUALIFIED, NegativeArraySizeException.class);
        EXCEPTION_CLASSES.put(ARRAY_STORE_EXCEPTION, ArrayStoreException.class);
        EXCEPTION_CLASSES.put(ARRAY_STORE_EXCEPTION_QUALIFIED, ArrayStoreException.class);
        EXCEPTION_CLASSES.put(THROWABLE, Throwable.class);
        EXCEPTION_CLASSES.put(THROWABLE_QUALIFIED, Throwable.class);
        EXCEPTION_CLASSES.put(ERROR, Error.class);
        EXCEPTION_CLASSES.put(ERROR_QUALIFIED, Error.class);
    }
    
    public static Class<?> getExceptionClass(String typeName) {
        return EXCEPTION_CLASSES.get(typeName);
    }
    
    public static boolean isExceptionType(String typeName) {
        return EXCEPTION_CLASSES.containsKey(typeName);
    }
    
    public static boolean isAssignableFrom(String exceptionTypeName, Class<?> thrownExceptionClass) {
        Class<?> exceptionClass = getExceptionClass(exceptionTypeName);
        if (exceptionClass == null) return false;
        return exceptionClass.isAssignableFrom(thrownExceptionClass);
    }
}
