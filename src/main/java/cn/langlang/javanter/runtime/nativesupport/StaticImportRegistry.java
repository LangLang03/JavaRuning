package cn.langlang.javanter.runtime.nativesupport;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

public class StaticImportRegistry {
    private final Map<String, Object> staticMembers = new HashMap<>();
    private final Map<String, Class<?>> staticClasses = new HashMap<>();
    
    public void registerConstant(String name, Object value) {
        staticMembers.put(name, value);
    }
    
    public void registerMethod(String name, StaticMethodHolder holder) {
        staticMembers.put(name, holder);
    }
    
    public void registerClass(String alias, Class<?> clazz) {
        staticClasses.put(alias, clazz);
    }
    
    public Object resolve(String name) {
        Object member = staticMembers.get(name);
        if (member != null) {
            return member;
        }
        
        for (Map.Entry<String, Class<?>> entry : staticClasses.entrySet()) {
            try {
                Class<?> clazz = entry.getValue();
                try {
                    Field field = clazz.getField(name);
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        Object value = field.get(null);
                        staticMembers.put(name, value);
                        return value;
                    }
                } catch (NoSuchFieldException e) {
                }
                
                for (Method method : clazz.getMethods()) {
                    if (method.getName().equals(name) && 
                        java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        StaticMethodHolder holder = new StaticMethodHolder(name, method);
                        staticMembers.put(name, holder);
                        return holder;
                    }
                }
            } catch (Exception e) {
            }
        }
        
        return null;
    }
    
    public boolean hasMember(String name) {
        return staticMembers.containsKey(name);
    }
    
    public void registerMathFunctions() {
        staticMembers.put("PI", Math.PI);
        staticMembers.put("E", Math.E);
        
        String[] mathMethods = {"sqrt", "pow", "abs", "max", "min", "sin", "cos", "tan", 
                               "log", "exp", "floor", "ceil", "round", "random"};
        for (String method : mathMethods) {
            staticMembers.put(method, new StaticMethodHolder(method, null));
        }
    }
    
    public void registerSystemMembers() {
        staticMembers.put("out", System.out);
        staticMembers.put("err", System.err);
        staticMembers.put("System", new StandardLibrary.SystemHolder());
    }
    
    public static class StaticMethodHolder {
        private final String methodName;
        private final Method reflectedMethod;
        
        public StaticMethodHolder(String methodName, Method reflectedMethod) {
            this.methodName = methodName;
            this.reflectedMethod = reflectedMethod;
        }
        
        public String getMethodName() {
            return methodName;
        }
        
        public Method getReflectedMethod() {
            return reflectedMethod;
        }
        
        public Object invoke(List<Object> args) {
            if (reflectedMethod != null) {
                try {
                    reflectedMethod.setAccessible(true);
                    return reflectedMethod.invoke(null, args.toArray());
                } catch (Exception e) {
                    throw new RuntimeException("Static method invocation error: " + e.getMessage(), e);
                }
            }
            return invokeMathMethod(methodName, args);
        }
        
        private Object invokeMathMethod(String name, List<Object> args) {
            try {
                switch (name) {
                    case "sqrt":
                        return Math.sqrt(((Number) args.get(0)).doubleValue());
                    case "pow":
                        return Math.pow(((Number) args.get(0)).doubleValue(), 
                                       ((Number) args.get(1)).doubleValue());
                    case "abs":
                        Object arg = args.get(0);
                        if (arg instanceof Integer) return Math.abs((Integer) arg);
                        if (arg instanceof Long) return Math.abs((Long) arg);
                        if (arg instanceof Double) return Math.abs((Double) arg);
                        if (arg instanceof Float) return Math.abs((Float) arg);
                        return Math.abs(((Number) arg).doubleValue());
                    case "max":
                        Object a = args.get(0);
                        Object b = args.get(1);
                        if (a instanceof Integer && b instanceof Integer) 
                            return Math.max((Integer) a, (Integer) b);
                        return Math.max(((Number) a).doubleValue(), ((Number) b).doubleValue());
                    case "min":
                        Object minA = args.get(0);
                        Object minB = args.get(1);
                        if (minA instanceof Integer && minB instanceof Integer) 
                            return Math.min((Integer) minA, (Integer) minB);
                        return Math.min(((Number) minA).doubleValue(), ((Number) minB).doubleValue());
                    case "sin":
                        return Math.sin(((Number) args.get(0)).doubleValue());
                    case "cos":
                        return Math.cos(((Number) args.get(0)).doubleValue());
                    case "tan":
                        return Math.tan(((Number) args.get(0)).doubleValue());
                    case "log":
                        return Math.log(((Number) args.get(0)).doubleValue());
                    case "exp":
                        return Math.exp(((Number) args.get(0)).doubleValue());
                    case "floor":
                        return Math.floor(((Number) args.get(0)).doubleValue());
                    case "ceil":
                        return Math.ceil(((Number) args.get(0)).doubleValue());
                    case "round":
                        return Math.round(((Number) args.get(0)).doubleValue());
                    case "random":
                        return Math.random();
                    default:
                        return null;
                }
            } catch (Exception e) {
                throw new RuntimeException("Static method error: " + e.getMessage(), e);
            }
        }
    }
}
