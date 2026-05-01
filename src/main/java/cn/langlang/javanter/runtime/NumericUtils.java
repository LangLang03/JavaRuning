package cn.langlang.javanter.runtime;

import java.math.BigDecimal;

public final class NumericUtils {
    private NumericUtils() {}
    
    public static boolean isNumeric(Object value) {
        return value instanceof Number;
    }
    
    public static boolean isIntegerType(Object value) {
        return value instanceof Integer || value instanceof Long || 
               value instanceof Short || value instanceof Byte;
    }
    
    public static boolean isFloatingPointType(Object value) {
        return value instanceof Double || value instanceof Float;
    }
    
    public static Number toNumber(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to Number");
    }
    
    public static Object add(Object left, Object right) {
        if (left instanceof String || right instanceof String) {
            return String.valueOf(left) + String.valueOf(right);
        }
        
        if (left instanceof Double || right instanceof Double) {
            return toNumber(left).doubleValue() + toNumber(right).doubleValue();
        }
        if (left instanceof Float || right instanceof Float) {
            return toNumber(left).floatValue() + toNumber(right).floatValue();
        }
        if (left instanceof Long || right instanceof Long) {
            return toNumber(left).longValue() + toNumber(right).longValue();
        }
        return toNumber(left).intValue() + toNumber(right).intValue();
    }
    
    public static Object subtract(Object left, Object right) {
        if (left instanceof Double || right instanceof Double) {
            return toNumber(left).doubleValue() - toNumber(right).doubleValue();
        }
        if (left instanceof Float || right instanceof Float) {
            return toNumber(left).floatValue() - toNumber(right).floatValue();
        }
        if (left instanceof Long || right instanceof Long) {
            return toNumber(left).longValue() - toNumber(right).longValue();
        }
        return toNumber(left).intValue() - toNumber(right).intValue();
    }
    
    public static Object multiply(Object left, Object right) {
        if (left instanceof Double || right instanceof Double) {
            return toNumber(left).doubleValue() * toNumber(right).doubleValue();
        }
        if (left instanceof Float || right instanceof Float) {
            return toNumber(left).floatValue() * toNumber(right).floatValue();
        }
        if (left instanceof Long || right instanceof Long) {
            return toNumber(left).longValue() * toNumber(right).longValue();
        }
        return toNumber(left).intValue() * toNumber(right).intValue();
    }
    
    public static Object divide(Object left, Object right) {
        if (left instanceof Double || right instanceof Double) {
            return toNumber(left).doubleValue() / toNumber(right).doubleValue();
        }
        if (left instanceof Float || right instanceof Float) {
            return toNumber(left).floatValue() / toNumber(right).floatValue();
        }
        if (left instanceof Long || right instanceof Long) {
            return toNumber(left).longValue() / toNumber(right).longValue();
        }
        return toNumber(left).intValue() / toNumber(right).intValue();
    }
    
    public static Object modulo(Object left, Object right) {
        if (left instanceof Double || right instanceof Double) {
            return toNumber(left).doubleValue() % toNumber(right).doubleValue();
        }
        if (left instanceof Float || right instanceof Float) {
            return toNumber(left).floatValue() % toNumber(right).floatValue();
        }
        if (left instanceof Long || right instanceof Long) {
            return toNumber(left).longValue() % toNumber(right).longValue();
        }
        return toNumber(left).intValue() % toNumber(right).intValue();
    }
    
    public static Object negate(Object value) {
        if (value instanceof Double) return -((Double) value);
        if (value instanceof Float) return -((Float) value);
        if (value instanceof Long) return -((Long) value);
        if (value instanceof Integer) return -((Integer) value);
        if (value instanceof Short) return -((Short) value);
        if (value instanceof Byte) return -((Byte) value);
        return -toNumber(value).intValue();
    }
    
    public static Object bitwiseAnd(Object left, Object right) {
        if (left instanceof Long || right instanceof Long) {
            return toNumber(left).longValue() & toNumber(right).longValue();
        }
        return toNumber(left).intValue() & toNumber(right).intValue();
    }
    
    public static Object bitwiseOr(Object left, Object right) {
        if (left instanceof Long || right instanceof Long) {
            return toNumber(left).longValue() | toNumber(right).longValue();
        }
        return toNumber(left).intValue() | toNumber(right).intValue();
    }
    
    public static Object bitwiseXor(Object left, Object right) {
        if (left instanceof Long || right instanceof Long) {
            return toNumber(left).longValue() ^ toNumber(right).longValue();
        }
        return toNumber(left).intValue() ^ toNumber(right).intValue();
    }
    
    public static Object bitwiseNot(Object value) {
        if (value instanceof Long) return ~((Long) value);
        return ~toNumber(value).intValue();
    }
    
    public static Object leftShift(Object left, Object right) {
        if (left instanceof Long) {
            return ((Long) left) << toNumber(right).intValue();
        }
        return toNumber(left).intValue() << toNumber(right).intValue();
    }
    
    public static Object rightShift(Object left, Object right) {
        if (left instanceof Long) {
            return ((Long) left) >> toNumber(right).intValue();
        }
        return toNumber(left).intValue() >> toNumber(right).intValue();
    }
    
    public static Object unsignedRightShift(Object left, Object right) {
        if (left instanceof Long) {
            return ((Long) left) >>> toNumber(right).intValue();
        }
        return toNumber(left).intValue() >>> toNumber(right).intValue();
    }
    
    public static int compare(Object left, Object right) {
        if (left instanceof Double || right instanceof Double) {
            return Double.compare(toNumber(left).doubleValue(), toNumber(right).doubleValue());
        }
        if (left instanceof Float || right instanceof Float) {
            return Float.compare(toNumber(left).floatValue(), toNumber(right).floatValue());
        }
        if (left instanceof Long || right instanceof Long) {
            return Long.compare(toNumber(left).longValue(), toNumber(right).longValue());
        }
        return Integer.compare(toNumber(left).intValue(), toNumber(right).intValue());
    }
    
    public static Object increment(Object value) {
        if (value instanceof Long) return (Long) value + 1L;
        if (value instanceof Double) return (Double) value + 1.0;
        if (value instanceof Float) return (Float) value + 1.0f;
        return (Integer) value + 1;
    }
    
    public static Object decrement(Object value) {
        if (value instanceof Long) return (Long) value - 1L;
        if (value instanceof Double) return (Double) value - 1.0;
        if (value instanceof Float) return (Float) value - 1.0f;
        return (Integer) value - 1;
    }
}
