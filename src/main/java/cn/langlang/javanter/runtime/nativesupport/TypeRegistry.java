package cn.langlang.javanter.runtime.nativesupport;

import cn.langlang.javanter.runtime.model.RuntimeObject;
import cn.langlang.javanter.runtime.model.ScriptClass;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TypeRegistry {
    private final Map<String, Class<?>> primitiveTypes = new HashMap<>();
    private final Map<String, Class<?>> wrapperTypes = new HashMap<>();
    private final Map<String, Class<?>> commonTypes = new HashMap<>();
    private final Map<Class<?>, Function<Object, Object>> typeConverters = new HashMap<>();
    
    public TypeRegistry() {
        initializePrimitiveTypes();
        initializeWrapperTypes();
        initializeCommonTypes();
        initializeTypeConverters();
    }
    
    private void initializePrimitiveTypes() {
        primitiveTypes.put("int", int.class);
        primitiveTypes.put("long", long.class);
        primitiveTypes.put("short", short.class);
        primitiveTypes.put("byte", byte.class);
        primitiveTypes.put("char", char.class);
        primitiveTypes.put("boolean", boolean.class);
        primitiveTypes.put("float", float.class);
        primitiveTypes.put("double", double.class);
        primitiveTypes.put("void", void.class);
    }
    
    private void initializeWrapperTypes() {
        wrapperTypes.put("Integer", Integer.class);
        wrapperTypes.put("java.lang.Integer", Integer.class);
        wrapperTypes.put("Long", Long.class);
        wrapperTypes.put("java.lang.Long", Long.class);
        wrapperTypes.put("Double", Double.class);
        wrapperTypes.put("java.lang.Double", Double.class);
        wrapperTypes.put("Float", Float.class);
        wrapperTypes.put("java.lang.Float", Float.class);
        wrapperTypes.put("Boolean", Boolean.class);
        wrapperTypes.put("java.lang.Boolean", Boolean.class);
        wrapperTypes.put("Character", Character.class);
        wrapperTypes.put("java.lang.Character", Character.class);
        wrapperTypes.put("Byte", Byte.class);
        wrapperTypes.put("java.lang.Byte", Byte.class);
        wrapperTypes.put("Short", Short.class);
        wrapperTypes.put("java.lang.Short", Short.class);
        wrapperTypes.put("Number", Number.class);
        wrapperTypes.put("java.lang.Number", Number.class);
    }
    
    private void initializeCommonTypes() {
    }
    
    private void initializeTypeConverters() {
        typeConverters.put(int.class, value -> {
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof Character) return (int) (Character) value;
            return value;
        });
        
        typeConverters.put(long.class, value -> {
            if (value instanceof Number) return ((Number) value).longValue();
            if (value instanceof Character) return (long) (Character) value;
            return value;
        });
        
        typeConverters.put(short.class, value -> {
            if (value instanceof Number) return ((Number) value).shortValue();
            if (value instanceof Character) return (short) ((Character) value).charValue();
            return value;
        });
        
        typeConverters.put(byte.class, value -> {
            if (value instanceof Number) return ((Number) value).byteValue();
            if (value instanceof Character) return (byte) ((Character) value).charValue();
            return value;
        });
        
        typeConverters.put(char.class, value -> {
            if (value instanceof Number) return (char) ((Number) value).intValue();
            if (value instanceof Character) return value;
            return value;
        });
        
        typeConverters.put(float.class, value -> {
            if (value instanceof Number) return ((Number) value).floatValue();
            return value;
        });
        
        typeConverters.put(double.class, value -> {
            if (value instanceof Number) return ((Number) value).doubleValue();
            if (value instanceof Character) return (double) (Character) value;
            return value;
        });
        
        typeConverters.put(boolean.class, value -> {
            if (value instanceof Boolean) return value;
            return value;
        });
    }
    
    public Class<?> getClassLiteral(String typeName) {
        Class<?> primitive = primitiveTypes.get(typeName);
        if (primitive != null) return primitive;
        
        Class<?> wrapper = wrapperTypes.get(typeName);
        if (wrapper != null) return wrapper;
        
        Class<?> cached = commonTypes.get(typeName);
        if (cached != null) return cached;
        
        Class<?> dynamicClass = loadClassDynamically(typeName);
        if (dynamicClass != null) {
            commonTypes.put(typeName, dynamicClass);
            return dynamicClass;
        }
        
        return null;
    }
    
    private Class<?> loadClassDynamically(String typeName) {
        try {
            if (typeName.contains(".")) {
                return Class.forName(typeName);
            }
            
            String[] commonPackages = {
                "java.lang.",
                "java.util.",
                "java.io.",
                "java.net.",
                "java.util.regex.",
                "java.util.stream.",
                "java.util.function.",
                "java.text.",
                "java.lang.reflect.",
                "java.nio.",
                "java.nio.file.",
                "java.math.",
                "java.time.",
                "java.util.concurrent.",
                "java.util.concurrent.atomic.",
                "java.util.concurrent.locks."
            };
            
            for (String pkg : commonPackages) {
                try {
                    return Class.forName(pkg + typeName);
                } catch (ClassNotFoundException e) {
                    continue;
                }
            }
            
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    public boolean isPrimitiveType(String typeName) {
        return primitiveTypes.containsKey(typeName);
    }
    
    public boolean isWrapperType(String typeName) {
        return wrapperTypes.containsKey(typeName);
    }
    
    public Object castValue(Object value, String typeName) {
        if (value == null) return null;
        
        Class<?> targetType = primitiveTypes.get(typeName);
        if (targetType == null) {
            targetType = wrapperTypes.get(typeName);
        }
        
        if (targetType != null) {
            Function<Object, Object> converter = typeConverters.get(targetType);
            if (converter != null) {
                return converter.apply(value);
            }
        }
        
        return value;
    }
    
    public boolean isInstance(Object value, String typeName) {
        if (value == null) return !isPrimitiveType(typeName);
        
        Class<?> typeClass = getClassLiteral(typeName);
        if (typeClass != null) {
            return typeClass.isInstance(value);
        }
        
        if (value instanceof RuntimeObject) {
            return true;
        }
        
        return false;
    }
    
    public boolean isArrayType(String typeName, int arrayDimensions) {
        if (arrayDimensions > 0) return true;
        return false;
    }
    
    public void registerCustomType(String name, Class<?> type) {
        commonTypes.put(name, type);
    }
}
