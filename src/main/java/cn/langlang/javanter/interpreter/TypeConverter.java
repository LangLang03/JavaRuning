package cn.langlang.javanter.interpreter;

import cn.langlang.javanter.runtime.TypeConstants;
import java.util.HashMap;
import java.util.Map;

public final class TypeConverter {
    private TypeConverter() {}
    
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = new HashMap<>();
    
    static {
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(void.class, Void.class);
        
        for (Map.Entry<Class<?>, Class<?>> entry : PRIMITIVE_TO_WRAPPER.entrySet()) {
            WRAPPER_TO_PRIMITIVE.put(entry.getValue(), entry.getKey());
        }
    }
    
    public static Class<?> wrap(Class<?> primitiveType) {
        return PRIMITIVE_TO_WRAPPER.getOrDefault(primitiveType, primitiveType);
    }
    
    public static Class<?> unwrap(Class<?> wrapperType) {
        return WRAPPER_TO_PRIMITIVE.getOrDefault(wrapperType, wrapperType);
    }
    
    public static boolean isWrapperOf(Class<?> wrapperType, Class<?> primitiveType) {
        return PRIMITIVE_TO_WRAPPER.get(primitiveType) == wrapperType;
    }
    
    public static Object convertToType(Object value, Class<?> targetType) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                return TypeConstants.getDefaultValue(targetType.getName());
            }
            return null;
        }
        
        if (targetType.isInstance(value)) {
            return value;
        }
        
        if (targetType.isPrimitive()) {
            return convertToPrimitive(value, targetType);
        }
        
        Class<?> unwrappedTarget = WRAPPER_TO_PRIMITIVE.get(targetType);
        if (unwrappedTarget != null) {
            return convertToPrimitive(value, unwrappedTarget);
        }
        
        if (Number.class.isAssignableFrom(targetType) && value instanceof Number) {
            return convertNumberToWrapper((Number) value, targetType);
        }
        
        return value;
    }
    
    private static Object convertToPrimitive(Object value, Class<?> targetType) {
        if (!(value instanceof Number)) {
            if (targetType == boolean.class && value instanceof Boolean) {
                return value;
            }
            if (targetType == char.class && value instanceof Character) {
                return value;
            }
            return TypeConstants.getDefaultValue(targetType.getName());
        }
        
        Number num = (Number) value;
        
        if (targetType == int.class) return num.intValue();
        if (targetType == long.class) return num.longValue();
        if (targetType == double.class) return num.doubleValue();
        if (targetType == float.class) return num.floatValue();
        if (targetType == byte.class) return num.byteValue();
        if (targetType == short.class) return num.shortValue();
        if (targetType == char.class) return (char) num.intValue();
        
        return num;
    }
    
    private static Object convertNumberToWrapper(Number value, Class<?> targetType) {
        if (targetType == Integer.class) return value.intValue();
        if (targetType == Long.class) return value.longValue();
        if (targetType == Double.class) return value.doubleValue();
        if (targetType == Float.class) return value.floatValue();
        if (targetType == Byte.class) return value.byteValue();
        if (targetType == Short.class) return value.shortValue();
        return value;
    }
    
    public static boolean isWideningConversion(Class<?> from, Class<?> to) {
        if (from == to) return true;
        
        if (from == byte.class) {
            return to == short.class || to == int.class || to == long.class || 
                   to == float.class || to == double.class;
        }
        if (from == short.class) {
            return to == int.class || to == long.class || to == float.class || to == double.class;
        }
        if (from == char.class) {
            return to == int.class || to == long.class || to == float.class || to == double.class;
        }
        if (from == int.class) {
            return to == long.class || to == float.class || to == double.class;
        }
        if (from == long.class) {
            return to == float.class || to == double.class;
        }
        if (from == float.class) {
            return to == double.class;
        }
        
        return false;
    }
    
    public static boolean isNarrowingConversion(Class<?> from, Class<?> to) {
        if (from == to) return false;
        
        if (from == double.class) {
            return to == float.class || to == long.class || to == int.class ||
                   to == short.class || to == byte.class || to == char.class;
        }
        if (from == float.class) {
            return to == long.class || to == int.class || to == short.class || 
                   to == byte.class || to == char.class;
        }
        if (from == long.class) {
            return to == int.class || to == short.class || to == byte.class || to == char.class;
        }
        if (from == int.class) {
            return to == short.class || to == byte.class || to == char.class;
        }
        if (from == short.class) {
            return to == byte.class;
        }
        
        return false;
    }
    
    public static boolean isBoxingConversion(Class<?> from, Class<?> to) {
        return PRIMITIVE_TO_WRAPPER.get(from) == to;
    }
    
    public static boolean isUnboxingConversion(Class<?> from, Class<?> to) {
        return WRAPPER_TO_PRIMITIVE.get(from) == to;
    }
}
