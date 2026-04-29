package cn.langlang.javanter.runtime.nativesupport;

import cn.langlang.javanter.runtime.model.LambdaObject;
import cn.langlang.javanter.runtime.model.MethodReferenceObject;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.*;

public class ConstructorRegistry {
    private final Map<String, Function<List<Object>, Object>> constructorRegistry = new HashMap<>();
    
    public void register(String typeName, Function<List<Object>, Object> constructor) {
        constructorRegistry.put(typeName, constructor);
        String simpleName = typeName.substring(typeName.lastIndexOf('.') + 1);
        if (!simpleName.equals(typeName)) {
            constructorRegistry.put(simpleName, constructor);
        }
    }
    
    public Function<List<Object>, Object> getConstructor(String typeName) {
        Function<List<Object>, Object> cached = constructorRegistry.get(typeName);
        if (cached != null) {
            return cached;
        }
        
        Function<List<Object>, Object> dynamicConstructor = createDynamicConstructor(typeName);
        if (dynamicConstructor != null) {
            constructorRegistry.put(typeName, dynamicConstructor);
            String simpleName = typeName.substring(typeName.lastIndexOf('.') + 1);
            if (!simpleName.equals(typeName)) {
                constructorRegistry.put(simpleName, dynamicConstructor);
            }
            return dynamicConstructor;
        }
        
        return null;
    }
    
    private Function<List<Object>, Object> createDynamicConstructor(String typeName) {
        return args -> {
            try {
                String className = typeName;
                if (!typeName.contains(".")) {
                    className = resolveFullClassName(typeName);
                }
                
                if (className == null) {
                    return null;
                }
                
                Class<?> clazz = Class.forName(className);
                
                if (args.isEmpty()) {
                    try {
                        return clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                    }
                }
                
                Object[] argsArray = args.toArray();
                
                for (Constructor<?> constructor : clazz.getConstructors()) {
                    if (constructor.isVarArgs()) {
                        int fixedParams = constructor.getParameterCount() - 1;
                        if (args.size() >= fixedParams) {
                            try {
                                constructor.setAccessible(true);
                                Object[] convertedArgs = convertArgsForVarargsConstructor(constructor, argsArray);
                                return constructor.newInstance(convertedArgs);
                            } catch (Exception e) {
                                continue;
                            }
                        }
                    } else if (constructor.getParameterCount() == args.size()) {
                        try {
                            constructor.setAccessible(true);
                            Object[] convertedArgs = convertArgsForConstructor(constructor, argsArray);
                            return constructor.newInstance(convertedArgs);
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
                
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    if (constructor.isVarArgs()) {
                        int fixedParams = constructor.getParameterCount() - 1;
                        if (args.size() >= fixedParams) {
                            try {
                                constructor.setAccessible(true);
                                Object[] convertedArgs = convertArgsForVarargsConstructor(constructor, argsArray);
                                return constructor.newInstance(convertedArgs);
                            } catch (Exception e) {
                                continue;
                            }
                        }
                    } else if (constructor.getParameterCount() == args.size()) {
                        try {
                            constructor.setAccessible(true);
                            Object[] convertedArgs = convertArgsForConstructor(constructor, argsArray);
                            return constructor.newInstance(convertedArgs);
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
                
                return null;
            } catch (ClassNotFoundException e) {
                return null;
            } catch (Exception e) {
                return null;
            }
        };
    }
    
    private String resolveFullClassName(String simpleName) {
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
                Class.forName(pkg + simpleName);
                return pkg + simpleName;
            } catch (ClassNotFoundException e) {
                continue;
            }
        }
        
        return null;
    }
    
    private Object[] convertArgsForConstructor(Constructor<?> constructor, Object[] args) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        Object[] converted = new Object[args.length];
        
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Class<?> paramType = paramTypes[i];
            
            if (arg == null) {
                converted[i] = null;
            } else if (paramType == int.class) {
                converted[i] = ((Number) arg).intValue();
            } else if (paramType == long.class) {
                converted[i] = ((Number) arg).longValue();
            } else if (paramType == double.class) {
                converted[i] = ((Number) arg).doubleValue();
            } else if (paramType == float.class) {
                converted[i] = ((Number) arg).floatValue();
            } else if (paramType == boolean.class) {
                converted[i] = arg;
            } else if (paramType == char.class) {
                converted[i] = arg;
            } else if (paramType == byte.class) {
                converted[i] = ((Number) arg).byteValue();
            } else if (paramType == short.class) {
                converted[i] = ((Number) arg).shortValue();
            } else {
                converted[i] = arg;
            }
        }
        
        return converted;
    }
    
    private Object[] convertArgsForVarargsConstructor(Constructor<?> constructor, Object[] args) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        int fixedParams = paramTypes.length - 1;
        Object[] converted = new Object[paramTypes.length];
        
        for (int i = 0; i < fixedParams; i++) {
            Object arg = args[i];
            Class<?> paramType = paramTypes[i];
            converted[i] = convertSingleArg(arg, paramType);
        }
        
        Class<?> varargType = paramTypes[fixedParams];
        Class<?> componentType = varargType.getComponentType();
        int varargCount = args.length - fixedParams;
        Object varargArray = java.lang.reflect.Array.newInstance(componentType, varargCount);
        
        for (int i = 0; i < varargCount; i++) {
            Object arg = args[fixedParams + i];
            Object convertedArg = convertSingleArg(arg, componentType);
            java.lang.reflect.Array.set(varargArray, i, convertedArg);
        }
        converted[fixedParams] = varargArray;
        
        return converted;
    }
    
    private Object convertSingleArg(Object arg, Class<?> paramType) {
        if (arg == null) {
            return null;
        } else if (paramType == int.class) {
            return ((Number) arg).intValue();
        } else if (paramType == long.class) {
            return ((Number) arg).longValue();
        } else if (paramType == double.class) {
            return ((Number) arg).doubleValue();
        } else if (paramType == float.class) {
            return ((Number) arg).floatValue();
        } else if (paramType == boolean.class) {
            return arg;
        } else if (paramType == char.class) {
            return arg;
        } else if (paramType == byte.class) {
            return ((Number) arg).byteValue();
        } else if (paramType == short.class) {
            return ((Number) arg).shortValue();
        } else {
            return arg;
        }
    }
    
    public boolean hasConstructor(String typeName) {
        return constructorRegistry.containsKey(typeName) || getConstructor(typeName) != null;
    }
}
