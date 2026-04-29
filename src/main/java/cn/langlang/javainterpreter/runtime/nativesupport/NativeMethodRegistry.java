package cn.langlang.javainterpreter.runtime.nativesupport;

import java.util.*;
import java.util.function.*;

public class NativeMethodRegistry {
    private final Map<Class<?>, Map<String, MethodHandler>> methodRegistry = new HashMap<>();
    private final Map<Class<?>, Map<String, FieldHandler>> fieldRegistry = new HashMap<>();
    
    @FunctionalInterface
    public interface MethodHandler {
        Object invoke(Object target, String methodName, List<Object> args);
    }
    
    @FunctionalInterface
    public interface FieldHandler {
        Object get(Object target, String fieldName);
    }
    
    public void registerMethod(Class<?> targetClass, String methodName, MethodHandler handler) {
        methodRegistry.computeIfAbsent(targetClass, k -> new HashMap<>()).put(methodName, handler);
    }
    
    public void registerField(Class<?> targetClass, String fieldName, FieldHandler handler) {
        fieldRegistry.computeIfAbsent(targetClass, k -> new HashMap<>()).put(fieldName, handler);
    }
    
    public MethodHandler getMethodHandler(Class<?> targetClass, String methodName) {
        Map<String, MethodHandler> methods = methodRegistry.get(targetClass);
        return methods != null ? methods.get(methodName) : null;
    }
    
    public FieldHandler getFieldHandler(Class<?> targetClass, String fieldName) {
        Map<String, FieldHandler> fields = fieldRegistry.get(targetClass);
        return fields != null ? fields.get(fieldName) : null;
    }
    
    public boolean hasMethodHandler(Class<?> targetClass, String methodName) {
        Map<String, MethodHandler> methods = methodRegistry.get(targetClass);
        return methods != null && methods.containsKey(methodName);
    }
    
    public boolean hasFieldHandler(Class<?> targetClass, String fieldName) {
        Map<String, FieldHandler> fields = fieldRegistry.get(targetClass);
        return fields != null && fields.containsKey(fieldName);
    }
    
    public Map<Class<?>, Map<String, MethodHandler>> getMethodRegistry() {
        return methodRegistry;
    }
}
