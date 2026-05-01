package cn.langlang.javanter.runtime.nativesupport;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

public final class ReflectionInvoker {
    private ReflectionInvoker() {}
    
    public static Object invokeMethod(Object target, Method method, List<Object> args) {
        try {
            Object[] convertedArgs = convertArguments(method.getParameterTypes(), 
                method.isVarArgs(), args);
            method.setAccessible(true);
            return method.invoke(target, convertedArgs);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access method: " + method.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Method invocation error: " + cause.getMessage(), cause);
        }
    }
    
    public static Object invokeStaticMethod(Method method, List<Object> args) {
        return invokeMethod(null, method, args);
    }
    
    public static Object invokeConstructor(Constructor<?> constructor, List<Object> args) {
        try {
            Object[] convertedArgs = convertArguments(constructor.getParameterTypes(), 
                constructor.isVarArgs(), args);
            constructor.setAccessible(true);
            return constructor.newInstance(convertedArgs);
        } catch (InstantiationException e) {
            throw new RuntimeException("Cannot instantiate: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access constructor: " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Constructor invocation error: " + cause.getMessage(), cause);
        }
    }
    
    public static Object getFieldValue(Object target, Field field) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field: " + field.getName(), e);
        }
    }
    
    public static void setFieldValue(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot set field: " + field.getName(), e);
        }
    }
    
    public static Object getStaticFieldValue(Field field) {
        return getFieldValue(null, field);
    }
    
    public static void setStaticFieldValue(Field field, Object value) {
        setFieldValue(null, field, value);
    }
    
    private static Object[] convertArguments(Class<?>[] paramTypes, boolean isVarArgs, 
                                              List<Object> args) {
        if (!isVarArgs) {
            return args.toArray();
        }
        
        int fixedParams = paramTypes.length - 1;
        Object[] converted = new Object[paramTypes.length];
        
        for (int i = 0; i < fixedParams && i < args.size(); i++) {
            converted[i] = args.get(i);
        }
        
        Class<?> varArgType = paramTypes[fixedParams].getComponentType();
        int varArgCount = Math.max(0, args.size() - fixedParams);
        Object varArgArray = Array.newInstance(varArgType, varArgCount);
        
        for (int i = 0; i < varArgCount; i++) {
            Array.set(varArgArray, i, args.get(fixedParams + i));
        }
        converted[fixedParams] = varArgArray;
        
        return converted;
    }
    
    public static Method findBestMatch(Class<?> targetClass, String methodName, 
                                        List<Object> args) {
        List<Method> candidates = new ArrayList<>();
        
        for (Method method : targetClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                if (isCompatible(method.getParameterTypes(), method.isVarArgs(), args)) {
                    candidates.add(method);
                }
            }
        }
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        
        return selectMostSpecific(candidates, args);
    }
    
    public static Constructor<?> findBestConstructor(Class<?> targetClass, List<Object> args) {
        List<Constructor<?>> candidates = new ArrayList<>();
        
        for (Constructor<?> constructor : targetClass.getConstructors()) {
            if (isCompatible(constructor.getParameterTypes(), constructor.isVarArgs(), args)) {
                candidates.add(constructor);
            }
        }
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        
        return selectMostSpecificConstructor(candidates, args);
    }
    
    private static boolean isCompatible(Class<?>[] paramTypes, boolean isVarArgs, 
                                         List<Object> args) {
        if (isVarArgs) {
            int fixedParams = paramTypes.length - 1;
            if (args.size() < fixedParams) {
                return false;
            }
        } else {
            if (paramTypes.length != args.size()) {
                return false;
            }
        }
        
        return true;
    }
    
    private static Method selectMostSpecific(List<Method> candidates, List<Object> args) {
        Method best = candidates.get(0);
        int bestScore = computeMatchScore(best, args);
        
        for (int i = 1; i < candidates.size(); i++) {
            Method candidate = candidates.get(i);
            int score = computeMatchScore(candidate, args);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        
        return best;
    }
    
    private static Constructor<?> selectMostSpecificConstructor(List<Constructor<?>> candidates, 
                                                                 List<Object> args) {
        Constructor<?> best = candidates.get(0);
        int bestScore = computeConstructorMatchScore(best, args);
        
        for (int i = 1; i < candidates.size(); i++) {
            Constructor<?> candidate = candidates.get(i);
            int score = computeConstructorMatchScore(candidate, args);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        
        return best;
    }
    
    private static int computeMatchScore(Method method, List<Object> args) {
        Class<?>[] paramTypes = method.getParameterTypes();
        int score = 0;
        
        int fixedParams = method.isVarArgs() ? paramTypes.length - 1 : paramTypes.length;
        
        for (int i = 0; i < fixedParams && i < args.size(); i++) {
            Object arg = args.get(i);
            Class<?> paramType = paramTypes[i];
            
            if (arg == null) {
                score += 1;
            } else if (paramType.isInstance(arg)) {
                score += 10;
            } else if (paramType.isPrimitive() && arg instanceof Number) {
                score += 5;
            } else {
                score += 1;
            }
        }
        
        if (method.isVarArgs() && args.size() >= fixedParams) {
            score += 5;
        }
        
        return score;
    }
    
    private static int computeConstructorMatchScore(Constructor<?> constructor, List<Object> args) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        int score = 0;
        
        int fixedParams = constructor.isVarArgs() ? paramTypes.length - 1 : paramTypes.length;
        
        for (int i = 0; i < fixedParams && i < args.size(); i++) {
            Object arg = args.get(i);
            Class<?> paramType = paramTypes[i];
            
            if (arg == null) {
                score += 1;
            } else if (paramType.isInstance(arg)) {
                score += 10;
            } else if (paramType.isPrimitive() && arg instanceof Number) {
                score += 5;
            } else {
                score += 1;
            }
        }
        
        if (constructor.isVarArgs() && args.size() >= fixedParams) {
            score += 5;
        }
        
        return score;
    }
}
