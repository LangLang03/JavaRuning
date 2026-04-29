package cn.langlang.javainterpreter.runtime.nativesupport;

import cn.langlang.javainterpreter.interpreter.Interpreter;
import cn.langlang.javainterpreter.interpreter.exception.ReturnException;
import cn.langlang.javainterpreter.interpreter.exception.InterpreterException;
import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.declaration.InterfaceDeclaration;
import cn.langlang.javainterpreter.ast.expression.*;
import cn.langlang.javainterpreter.ast.statement.BlockStatement;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.runtime.environment.Environment;
import cn.langlang.javainterpreter.runtime.model.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.lang.reflect.*;

public class StandardLibrary {
    private final Interpreter interpreter;
    private final Map<String, ScriptClass> standardClasses;
    private final NativeMethodRegistry methodRegistry;
    private final ConstructorRegistry constructorRegistry;
    private final StaticImportRegistry staticImportRegistry;
    
    public StandardLibrary(Interpreter interpreter) {
        this.interpreter = interpreter;
        this.standardClasses = new HashMap<>();
        this.methodRegistry = new NativeMethodRegistry();
        this.constructorRegistry = new ConstructorRegistry();
        this.staticImportRegistry = new StaticImportRegistry();
        
        initializeRegistries();
    }
    
    private void initializeRegistries() {
        staticImportRegistry.registerMathFunctions();
        staticImportRegistry.registerSystemMembers();
        staticImportRegistry.registerClass("Math", Math.class);
        staticImportRegistry.registerClass("System", System.class);
    }
    
    public void initializeStandardClasses(Environment env) {
        initializeSystem(env);
        initializeMath(env);
        initializeCollections(env);
        initializeString(env);
        initializeIO(env);
    }
    
    private void initializeSystem(Environment env) {
        env.defineVariable("out", System.out);
        env.defineVariable("err", System.err);
        env.defineVariable("System", new SystemHolder());
    }
    
    private void initializeMath(Environment env) {
        Map<String, Object> mathMethods = new HashMap<>();
        mathMethods.put("abs", (java.util.function.Function<Object, Object>) arg -> {
            if (arg instanceof Integer) return Math.abs((Integer) arg);
            if (arg instanceof Long) return Math.abs((Long) arg);
            if (arg instanceof Double) return Math.abs((Double) arg);
            if (arg instanceof Float) return Math.abs((Float) arg);
            return Math.abs(((Number) arg).doubleValue());
        });
        mathMethods.put("max", (java.util.function.BiFunction<Object, Object, Object>) (a, b) -> {
            if (a instanceof Integer && b instanceof Integer) return Math.max((Integer) a, (Integer) b);
            if (a instanceof Long && b instanceof Long) return Math.max((Long) a, (Long) b);
            return Math.max(((Number) a).doubleValue(), ((Number) b).doubleValue());
        });
        mathMethods.put("min", (java.util.function.BiFunction<Object, Object, Object>) (a, b) -> {
            if (a instanceof Integer && b instanceof Integer) return Math.min((Integer) a, (Integer) b);
            if (a instanceof Long && b instanceof Long) return Math.min((Long) a, (Long) b);
            return Math.min(((Number) a).doubleValue(), ((Number) b).doubleValue());
        });
        mathMethods.put("sqrt", (java.util.function.Function<Object, Object>) arg -> Math.sqrt(((Number) arg).doubleValue()));
        mathMethods.put("pow", (java.util.function.BiFunction<Object, Object, Object>) (a, b) -> Math.pow(((Number) a).doubleValue(), ((Number) b).doubleValue()));
        mathMethods.put("sin", (java.util.function.Function<Object, Object>) arg -> Math.sin(((Number) arg).doubleValue()));
        mathMethods.put("cos", (java.util.function.Function<Object, Object>) arg -> Math.cos(((Number) arg).doubleValue()));
        mathMethods.put("tan", (java.util.function.Function<Object, Object>) arg -> Math.tan(((Number) arg).doubleValue()));
        mathMethods.put("log", (java.util.function.Function<Object, Object>) arg -> Math.log(((Number) arg).doubleValue()));
        mathMethods.put("exp", (java.util.function.Function<Object, Object>) arg -> Math.exp(((Number) arg).doubleValue()));
        mathMethods.put("floor", (java.util.function.Function<Object, Object>) arg -> Math.floor(((Number) arg).doubleValue()));
        mathMethods.put("ceil", (java.util.function.Function<Object, Object>) arg -> Math.ceil(((Number) arg).doubleValue()));
        mathMethods.put("round", (java.util.function.Function<Object, Object>) arg -> Math.round(((Number) arg).doubleValue()));
        mathMethods.put("random", (java.util.function.Supplier<Double>) Math::random);
        mathMethods.put("PI", Math.PI);
        mathMethods.put("E", Math.E);
        env.defineVariable("Math", mathMethods);
    }
    
    private void initializeCollections(Environment env) {
    }
    
    private void initializeString(Environment env) {
    }
    
    private void initializeIO(Environment env) {
    }
    
    private void registerStringMethods() {
        Map<String, NativeMethodRegistry.MethodHandler> stringMethods = new HashMap<>();
        
        stringMethods.put("length", (target, name, args) -> ((String) target).length());
        stringMethods.put("charAt", (target, name, args) -> ((String) target).charAt((Integer) args.get(0)));
        stringMethods.put("substring", (target, name, args) -> {
            String str = (String) target;
            if (args.size() == 1) return str.substring((Integer) args.get(0));
            return str.substring((Integer) args.get(0), (Integer) args.get(1));
        });
        stringMethods.put("indexOf", (target, name, args) -> ((String) target).indexOf((String) args.get(0)));
        stringMethods.put("contains", (target, name, args) -> ((String) target).contains((String) args.get(0)));
        stringMethods.put("startsWith", (target, name, args) -> ((String) target).startsWith((String) args.get(0)));
        stringMethods.put("endsWith", (target, name, args) -> ((String) target).endsWith((String) args.get(0)));
        stringMethods.put("trim", (target, name, args) -> ((String) target).trim());
        stringMethods.put("toLowerCase", (target, name, args) -> ((String) target).toLowerCase());
        stringMethods.put("toUpperCase", (target, name, args) -> ((String) target).toUpperCase());
        stringMethods.put("replace", (target, name, args) -> ((String) target).replace((CharSequence) args.get(0), (CharSequence) args.get(1)));
        stringMethods.put("split", (target, name, args) -> ((String) target).split((String) args.get(0)));
        stringMethods.put("equals", (target, name, args) -> ((String) target).equals(args.get(0)));
        stringMethods.put("equalsIgnoreCase", (target, name, args) -> ((String) target).equalsIgnoreCase((String) args.get(0)));
        stringMethods.put("compareTo", (target, name, args) -> ((String) target).compareTo((String) args.get(0)));
        stringMethods.put("compareToIgnoreCase", (target, name, args) -> ((String) target).compareToIgnoreCase((String) args.get(0)));
        stringMethods.put("isEmpty", (target, name, args) -> ((String) target).isEmpty());
        stringMethods.put("concat", (target, name, args) -> ((String) target).concat((String) args.get(0)));
        stringMethods.put("getBytes", (target, name, args) -> ((String) target).getBytes());
        stringMethods.put("toCharArray", (target, name, args) -> ((String) target).toCharArray());
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : stringMethods.entrySet()) {
            methodRegistry.registerMethod(String.class, entry.getKey(), entry.getValue());
        }
    }
    
    private void registerListMethods() {
        Map<String, NativeMethodRegistry.MethodHandler> listMethods = new HashMap<>();
        
        listMethods.put("get", (target, name, args) -> ((List<?>) target).get((Integer) args.get(0)));
        listMethods.put("add", (target, name, args) -> { ((List) target).add(args.get(0)); return true; });
        listMethods.put("remove", (target, name, args) -> {
            List<?> list = (List<?>) target;
            if (args.get(0) instanceof Integer) {
                return list.remove((int) args.get(0));
            }
            return ((List) target).remove(args.get(0));
        });
        listMethods.put("size", (target, name, args) -> ((List<?>) target).size());
        listMethods.put("isEmpty", (target, name, args) -> ((List<?>) target).isEmpty());
        listMethods.put("contains", (target, name, args) -> ((List<?>) target).contains(args.get(0)));
        listMethods.put("clear", (target, name, args) -> { ((List) target).clear(); return null; });
        listMethods.put("forEach", (target, name, args) -> {
            Object consumer = args.get(0);
            if (consumer instanceof LambdaObject) {
                LambdaObject lambda = (LambdaObject) consumer;
                for (Object elem : (List<?>) target) {
                    invokeLambda(lambda, Arrays.asList(elem));
                }
            }
            return null;
        });
        listMethods.put("stream", (target, name, args) -> ((List<?>) target).stream());
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : listMethods.entrySet()) {
            methodRegistry.registerMethod(List.class, entry.getKey(), entry.getValue());
        }
    }
    
    private void registerSetMethods() {
        Map<String, NativeMethodRegistry.MethodHandler> setMethods = new HashMap<>();
        
        setMethods.put("add", (target, name, args) -> ((Set) target).add(args.get(0)));
        setMethods.put("contains", (target, name, args) -> ((Set<?>) target).contains(args.get(0)));
        setMethods.put("remove", (target, name, args) -> ((Set) target).remove(args.get(0)));
        setMethods.put("size", (target, name, args) -> ((Set<?>) target).size());
        setMethods.put("isEmpty", (target, name, args) -> ((Set<?>) target).isEmpty());
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : setMethods.entrySet()) {
            methodRegistry.registerMethod(Set.class, entry.getKey(), entry.getValue());
        }
    }
    
    private void registerMapMethods() {
        Map<String, NativeMethodRegistry.MethodHandler> mapMethods = new HashMap<>();
        
        mapMethods.put("get", (target, name, args) -> ((Map<?, ?>) target).get(args.get(0)));
        mapMethods.put("put", (target, name, args) -> ((Map) target).put(args.get(0), args.get(1)));
        mapMethods.put("containsKey", (target, name, args) -> ((Map<?, ?>) target).containsKey(args.get(0)));
        mapMethods.put("keySet", (target, name, args) -> ((Map<?, ?>) target).keySet());
        mapMethods.put("values", (target, name, args) -> ((Map<?, ?>) target).values());
        mapMethods.put("size", (target, name, args) -> ((Map<?, ?>) target).size());
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : mapMethods.entrySet()) {
            methodRegistry.registerMethod(Map.class, entry.getKey(), entry.getValue());
        }
    }
    
    private void registerStreamMethods() {
        Map<String, NativeMethodRegistry.MethodHandler> streamMethods = new HashMap<>();
        
        streamMethods.put("forEach", (target, name, args) -> {
            Object consumer = args.get(0);
            if (consumer instanceof LambdaObject) {
                LambdaObject lambda = (LambdaObject) consumer;
                ((Stream<?>) target).forEach(elem -> invokeLambda(lambda, Arrays.asList(elem)));
            }
            return null;
        });
        streamMethods.put("map", (target, name, args) -> {
            Object mapper = args.get(0);
            if (mapper instanceof LambdaObject) {
                LambdaObject lambda = (LambdaObject) mapper;
                return ((Stream<?>) target).map(elem -> invokeLambda(lambda, Arrays.asList(elem)));
            }
            return null;
        });
        streamMethods.put("filter", (target, name, args) -> {
            Object predicate = args.get(0);
            if (predicate instanceof LambdaObject) {
                LambdaObject lambda = (LambdaObject) predicate;
                return ((Stream<?>) target).filter(elem -> (Boolean) invokeLambda(lambda, Arrays.asList(elem)));
            }
            return null;
        });
        streamMethods.put("collect", (target, name, args) -> {
            Object collector = args.get(0);
            if (collector instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) collector;
                if (map.containsKey("type")) {
                    String type = (String) map.get("type");
                    if (type.equals("toList")) {
                        return ((Stream<?>) target).collect(Collectors.toList());
                    } else if (type.equals("toSet")) {
                        return ((Stream<?>) target).collect(Collectors.toSet());
                    }
                }
            }
            return null;
        });
        streamMethods.put("count", (target, name, args) -> ((Stream<?>) target).count());
        streamMethods.put("findFirst", (target, name, args) -> ((Stream<?>) target).findFirst());
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : streamMethods.entrySet()) {
            methodRegistry.registerMethod(Stream.class, entry.getKey(), entry.getValue());
        }
    }
    
    private void registerOptionalMethods() {
        Map<String, NativeMethodRegistry.MethodHandler> optionalMethods = new HashMap<>();
        
        optionalMethods.put("isPresent", (target, name, args) -> ((Optional<?>) target).isPresent());
        optionalMethods.put("get", (target, name, args) -> ((Optional<?>) target).get());
        optionalMethods.put("orElse", (target, name, args) -> {
            @SuppressWarnings("unchecked")
            Optional<Object> opt = (Optional<Object>) target;
            return opt.orElse(args.get(0));
        });
        optionalMethods.put("ifPresent", (target, name, args) -> {
            Object consumer = args.get(0);
            if (consumer instanceof LambdaObject) {
                LambdaObject lambda = (LambdaObject) consumer;
                ((Optional<?>) target).ifPresent(elem -> invokeLambda(lambda, Arrays.asList(elem)));
            }
            return null;
        });
        optionalMethods.put("map", (target, name, args) -> {
            Object mapper = args.get(0);
            if (mapper instanceof LambdaObject) {
                LambdaObject lambda = (LambdaObject) mapper;
                return ((Optional<?>) target).map(elem -> invokeLambda(lambda, Arrays.asList(elem)));
            }
            return null;
        });
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : optionalMethods.entrySet()) {
            methodRegistry.registerMethod(Optional.class, entry.getKey(), entry.getValue());
        }
    }
    
    private void registerPrimitiveMethods() {
        Map<String, NativeMethodRegistry.MethodHandler> integerMethods = new HashMap<>();
        integerMethods.put("intValue", (target, name, args) -> ((Integer) target).intValue());
        integerMethods.put("longValue", (target, name, args) -> ((Integer) target).longValue());
        integerMethods.put("doubleValue", (target, name, args) -> ((Integer) target).doubleValue());
        integerMethods.put("floatValue", (target, name, args) -> ((Integer) target).floatValue());
        integerMethods.put("shortValue", (target, name, args) -> ((Integer) target).shortValue());
        integerMethods.put("byteValue", (target, name, args) -> ((Integer) target).byteValue());
        integerMethods.put("compareTo", (target, name, args) -> ((Integer) target).compareTo((Integer) args.get(0)));
        integerMethods.put("toString", (target, name, args) -> ((Integer) target).toString());
        integerMethods.put("equals", (target, name, args) -> ((Integer) target).equals(args.get(0)));
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : integerMethods.entrySet()) {
            methodRegistry.registerMethod(Integer.class, entry.getKey(), entry.getValue());
        }
        
        Map<String, NativeMethodRegistry.MethodHandler> longMethods = new HashMap<>();
        longMethods.put("intValue", (target, name, args) -> ((Long) target).intValue());
        longMethods.put("longValue", (target, name, args) -> ((Long) target).longValue());
        longMethods.put("doubleValue", (target, name, args) -> ((Long) target).doubleValue());
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : longMethods.entrySet()) {
            methodRegistry.registerMethod(Long.class, entry.getKey(), entry.getValue());
        }
        
        Map<String, NativeMethodRegistry.MethodHandler> doubleMethods = new HashMap<>();
        doubleMethods.put("intValue", (target, name, args) -> ((Double) target).intValue());
        doubleMethods.put("longValue", (target, name, args) -> ((Double) target).longValue());
        doubleMethods.put("doubleValue", (target, name, args) -> ((Double) target).doubleValue());
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : doubleMethods.entrySet()) {
            methodRegistry.registerMethod(Double.class, entry.getKey(), entry.getValue());
        }
        
        Map<String, NativeMethodRegistry.MethodHandler> floatMethods = new HashMap<>();
        floatMethods.put("intValue", (target, name, args) -> ((Float) target).intValue());
        floatMethods.put("floatValue", (target, name, args) -> ((Float) target).floatValue());
        floatMethods.put("doubleValue", (target, name, args) -> ((Float) target).doubleValue());
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : floatMethods.entrySet()) {
            methodRegistry.registerMethod(Float.class, entry.getKey(), entry.getValue());
        }
        
        Map<String, NativeMethodRegistry.MethodHandler> charMethods = new HashMap<>();
        charMethods.put("charValue", (target, name, args) -> ((Character) target).charValue());
        charMethods.put("compareTo", (target, name, args) -> ((Character) target).compareTo((Character) args.get(0)));
        charMethods.put("equals", (target, name, args) -> ((Character) target).equals(args.get(0)));
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : charMethods.entrySet()) {
            methodRegistry.registerMethod(Character.class, entry.getKey(), entry.getValue());
        }
        
        Map<String, NativeMethodRegistry.MethodHandler> boolMethods = new HashMap<>();
        boolMethods.put("booleanValue", (target, name, args) -> ((Boolean) target).booleanValue());
        boolMethods.put("compareTo", (target, name, args) -> ((Boolean) target).compareTo((Boolean) args.get(0)));
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : boolMethods.entrySet()) {
            methodRegistry.registerMethod(Boolean.class, entry.getKey(), entry.getValue());
        }
    }
    
    private void registerThrowableMethods() {
        Map<String, NativeMethodRegistry.MethodHandler> throwableMethods = new HashMap<>();
        
        throwableMethods.put("getMessage", (target, name, args) -> ((Throwable) target).getMessage());
        throwableMethods.put("getLocalizedMessage", (target, name, args) -> ((Throwable) target).getLocalizedMessage());
        throwableMethods.put("getCause", (target, name, args) -> ((Throwable) target).getCause());
        throwableMethods.put("printStackTrace", (target, name, args) -> { ((Throwable) target).printStackTrace(); return null; });
        throwableMethods.put("toString", (target, name, args) -> ((Throwable) target).toString());
        throwableMethods.put("fillInStackTrace", (target, name, args) -> ((Throwable) target).fillInStackTrace());
        throwableMethods.put("getStackTrace", (target, name, args) -> ((Throwable) target).getStackTrace());
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : throwableMethods.entrySet()) {
            methodRegistry.registerMethod(Throwable.class, entry.getKey(), entry.getValue());
        }
    }
    
    private void registerIOClasses() {
        Map<String, NativeMethodRegistry.MethodHandler> printStreamMethods = new HashMap<>();
        
        printStreamMethods.put("println", (target, name, args) -> {
            java.io.PrintStream ps = (java.io.PrintStream) target;
            if (args.isEmpty()) {
                ps.println();
            } else {
                ps.println(args.get(0));
            }
            return null;
        });
        printStreamMethods.put("print", (target, name, args) -> {
            ((java.io.PrintStream) target).print(args.get(0));
            return null;
        });
        printStreamMethods.put("printf", (target, name, args) -> {
            Object[] formatArgs = args.subList(1, args.size()).toArray();
            return ((java.io.PrintStream) target).printf((String) args.get(0), formatArgs);
        });
        printStreamMethods.put("flush", (target, name, args) -> {
            ((java.io.PrintStream) target).flush();
            return null;
        });
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : printStreamMethods.entrySet()) {
            methodRegistry.registerMethod(java.io.PrintStream.class, entry.getKey(), entry.getValue());
        }
        
        Map<String, NativeMethodRegistry.MethodHandler> inputStreamMethods = new HashMap<>();
        inputStreamMethods.put("read", (target, name, args) -> {
            java.io.InputStream is = (java.io.InputStream) target;
            try {
                if (args.isEmpty()) {
                    return is.read();
                } else if (args.size() == 1 && args.get(0) instanceof byte[]) {
                    return is.read((byte[]) args.get(0));
                } else if (args.size() == 3 && args.get(0) instanceof byte[]) {
                    return is.read((byte[]) args.get(0), (Integer) args.get(1), (Integer) args.get(2));
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        inputStreamMethods.put("available", (target, name, args) -> {
            try { return ((java.io.InputStream) target).available(); } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        inputStreamMethods.put("close", (target, name, args) -> {
            try { ((java.io.InputStream) target).close(); return null; } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        inputStreamMethods.put("skip", (target, name, args) -> {
            try { return ((java.io.InputStream) target).skip((Long) args.get(0)); } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : inputStreamMethods.entrySet()) {
            methodRegistry.registerMethod(java.io.InputStream.class, entry.getKey(), entry.getValue());
        }
        
        Map<String, NativeMethodRegistry.MethodHandler> outputStreamMethods = new HashMap<>();
        outputStreamMethods.put("write", (target, name, args) -> {
            java.io.OutputStream os = (java.io.OutputStream) target;
            try {
                if (args.size() == 1) {
                    Object arg = args.get(0);
                    if (arg instanceof Integer) {
                        os.write((Integer) arg);
                    } else if (arg instanceof byte[]) {
                        os.write((byte[]) arg);
                    }
                } else if (args.size() == 3 && args.get(0) instanceof byte[]) {
                    os.write((byte[]) args.get(0), (Integer) args.get(1), (Integer) args.get(2));
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        outputStreamMethods.put("flush", (target, name, args) -> {
            try { ((java.io.OutputStream) target).flush(); return null; } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        outputStreamMethods.put("close", (target, name, args) -> {
            try { ((java.io.OutputStream) target).close(); return null; } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : outputStreamMethods.entrySet()) {
            methodRegistry.registerMethod(java.io.OutputStream.class, entry.getKey(), entry.getValue());
        }
    }
    
    private void registerNetworkClasses() {
        Map<String, NativeMethodRegistry.MethodHandler> socketMethods = new HashMap<>();
        
        socketMethods.put("getInputStream", (target, name, args) -> {
            try { return ((java.net.Socket) target).getInputStream(); } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        socketMethods.put("getOutputStream", (target, name, args) -> {
            try { return ((java.net.Socket) target).getOutputStream(); } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        socketMethods.put("close", (target, name, args) -> {
            try { ((java.net.Socket) target).close(); return null; } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        socketMethods.put("setSoTimeout", (target, name, args) -> {
            try { ((java.net.Socket) target).setSoTimeout((Integer) args.get(0)); return null; } catch (java.net.SocketException e) { throw new RuntimeException(e); }
        });
        socketMethods.put("setTcpNoDelay", (target, name, args) -> {
            try { ((java.net.Socket) target).setTcpNoDelay((Boolean) args.get(0)); return null; } catch (java.net.SocketException e) { throw new RuntimeException(e); }
        });
        socketMethods.put("isClosed", (target, name, args) -> ((java.net.Socket) target).isClosed());
        socketMethods.put("isConnected", (target, name, args) -> ((java.net.Socket) target).isConnected());
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : socketMethods.entrySet()) {
            methodRegistry.registerMethod(java.net.Socket.class, entry.getKey(), entry.getValue());
        }
        
        Map<String, NativeMethodRegistry.MethodHandler> serverSocketMethods = new HashMap<>();
        
        serverSocketMethods.put("accept", (target, name, args) -> {
            try { return ((java.net.ServerSocket) target).accept(); } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        serverSocketMethods.put("close", (target, name, args) -> {
            try { ((java.net.ServerSocket) target).close(); return null; } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        serverSocketMethods.put("isClosed", (target, name, args) -> ((java.net.ServerSocket) target).isClosed());
        
        for (Map.Entry<String, NativeMethodRegistry.MethodHandler> entry : serverSocketMethods.entrySet()) {
            methodRegistry.registerMethod(java.net.ServerSocket.class, entry.getKey(), entry.getValue());
        }
    }
    
    private void registerConstructors() {
    }
    
    public Object invokeMethod(Object target, String methodName, List<Object> args) {
        if (target instanceof StaticMethodHolder) {
            return ((StaticMethodHolder) target).invoke(args);
        }
        
        if (target instanceof StaticImportRegistry.StaticMethodHolder) {
            return ((StaticImportRegistry.StaticMethodHolder) target).invoke(args);
        }
        
        if (target instanceof SystemHolder) {
            return ((SystemHolder) target).invokeMethod(methodName, args);
        }
        
        if (target instanceof Class) {
            return invokeClassMethod((Class<?>) target, methodName, args);
        }
        
        if (target instanceof ScriptClass) {
            return invokeScriptClassMethod((ScriptClass) target, methodName, args);
        }
        
        if (target instanceof ScriptMethod) {
            return invokeScriptMethodMethod((ScriptMethod) target, methodName, args);
        }
        
        if (target instanceof ScriptField) {
            return invokeScriptFieldMethod((ScriptField) target, methodName, args);
        }
        
        if (target instanceof Method) {
            return invokeReflectionMethod((Method) target, methodName, args);
        }
        
        if (target instanceof Field) {
            return invokeFieldMethod((Field) target, args);
        }
        
        if (target instanceof Constructor) {
            return invokeConstructorMethod((Constructor<?>) target, args);
        }
        
        Class<?> targetClass = target.getClass();
        NativeMethodRegistry.MethodHandler handler = findMethodHandler(targetClass, methodName);
        if (handler != null) {
            return handler.invoke(target, methodName, args);
        }
        
        if (target instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) target;
            Object methodValue = map.get(methodName);
            if (methodValue != null) {
                if (methodValue instanceof java.util.function.Function) {
                    return ((java.util.function.Function) methodValue).apply(args.get(0));
                } else if (methodValue instanceof java.util.function.BiFunction) {
                    return ((java.util.function.BiFunction) methodValue).apply(args.get(0), args.get(1));
                } else if (methodValue instanceof java.util.function.Supplier) {
                    return ((java.util.function.Supplier) methodValue).get();
                }
            }
        }
        
        if (methodName.equals("getClass") && args.isEmpty()) {
            return target.getClass();
        }
        
        if (methodName.equals("hashCode") && args.isEmpty()) {
            return target.hashCode();
        }
        
        if (methodName.equals("toString") && args.isEmpty()) {
            return target.toString();
        }
        
        if (methodName.equals("equals") && args.size() == 1) {
            return target.equals(args.get(0));
        }
        
        return invokeByReflection(target, methodName, args);
    }
    
    private NativeMethodRegistry.MethodHandler findMethodHandler(Class<?> targetClass, String methodName) {
        NativeMethodRegistry.MethodHandler handler = methodRegistry.getMethodHandler(targetClass, methodName);
        if (handler != null) {
            return handler;
        }
        
        for (Map.Entry<Class<?>, Map<String, NativeMethodRegistry.MethodHandler>> entry : 
             methodRegistry.getMethodRegistry().entrySet()) {
            if (entry.getKey().isAssignableFrom(targetClass)) {
                handler = entry.getValue().get(methodName);
                if (handler != null) {
                    return handler;
                }
            }
        }
        
        return null;
    }
    
    private Object invokeByReflection(Object target, String methodName, List<Object> args) {
        try {
            Class<?> targetClass = target.getClass();
            
            Set<Class<?>> visitedInterfaces = new HashSet<>();
            List<Class<?>> allInterfaces = new ArrayList<>();
            collectAllInterfaces(targetClass, allInterfaces, visitedInterfaces);
            
            for (Class<?> iface : allInterfaces) {
                for (java.lang.reflect.Method method : iface.getMethods()) {
                    if (method.getName().equals(methodName) && method.getParameterCount() == args.size()) {
                        try {
                            method.setAccessible(true);
                            Object[] convertedArgs = convertArgsForMethod(method, args);
                            return method.invoke(target, convertedArgs);
                        } catch (Exception ex) {
                            continue;
                        }
                    }
                }
            }
            
            for (java.lang.reflect.Method method : targetClass.getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == args.size()) {
                    try {
                        method.setAccessible(true);
                        Object[] convertedArgs = convertArgsForMethod(method, args);
                        return method.invoke(target, convertedArgs);
                    } catch (Exception ex) {
                        continue;
                    }
                }
            }
            
            for (java.lang.reflect.Method method : targetClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == args.size()) {
                    try {
                        method.setAccessible(true);
                        Object[] convertedArgs = convertArgsForMethod(method, args);
                        return method.invoke(target, convertedArgs);
                    } catch (Exception ex) {
                        continue;
                    }
                }
            }
        } catch (Exception e) {
        }
        
        return null;
    }
    
    private void collectAllInterfaces(Class<?> clazz, List<Class<?>> interfaces, Set<Class<?>> visited) {
        if (clazz == null || visited.contains(clazz)) {
            return;
        }
        visited.add(clazz);
        
        for (Class<?> iface : clazz.getInterfaces()) {
            if (!interfaces.contains(iface)) {
                interfaces.add(iface);
                collectAllInterfaces(iface, interfaces, visited);
            }
        }
        
        collectAllInterfaces(clazz.getSuperclass(), interfaces, visited);
    }
    
    public Object getField(Object target, String fieldName) {
        if (target instanceof Object[]) {
            if (fieldName.equals("length")) {
                return ((Object[]) target).length;
            }
        }
        
        if (target instanceof byte[]) {
            if (fieldName.equals("length")) {
                return ((byte[]) target).length;
            }
        }
        
        if (target instanceof int[]) {
            if (fieldName.equals("length")) {
                return ((int[]) target).length;
            }
        }
        
        if (target instanceof long[]) {
            if (fieldName.equals("length")) {
                return ((long[]) target).length;
            }
        }
        
        if (target instanceof double[]) {
            if (fieldName.equals("length")) {
                return ((double[]) target).length;
            }
        }
        
        if (target instanceof char[]) {
            if (fieldName.equals("length")) {
                return ((char[]) target).length;
            }
        }
        
        if (target instanceof boolean[]) {
            if (fieldName.equals("length")) {
                return ((boolean[]) target).length;
            }
        }
        
        if (target instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) target;
            return map.get(fieldName);
        }
        
        return null;
    }
    
    public Object createObject(String typeName, List<Object> args) {
        if (typeName.equals("Thread") || typeName.equals("java.lang.Thread")) {
            return createThread(args);
        }
        
        Function<List<Object>, Object> constructor = constructorRegistry.getConstructor(typeName);
        if (constructor != null) {
            Object result = constructor.apply(args);
            if (result != null) {
                return result;
            }
        }
        
        return createObjectByReflection(typeName, args);
    }
    
    private Object createObjectByReflection(String typeName, List<Object> args) {
        try {
            String className = typeName;
            if (!typeName.contains(".")) {
                className = "java.lang." + typeName;
            }
            
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                if (!typeName.contains(".")) {
                    String[] commonPackages = {"java.io.", "java.util.", "java.net.", "java.lang.reflect."};
                    for (String pkg : commonPackages) {
                        try {
                            clazz = Class.forName(pkg + typeName);
                            break;
                        } catch (ClassNotFoundException e2) {
                            continue;
                        }
                    }
                    if (clazz == null) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
            
            if (args.isEmpty()) {
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                }
            }
            
            Object[] argsArray = args.toArray();
            
            for (Constructor<?> constructor : clazz.getConstructors()) {
                if (constructor.getParameterCount() == args.size()) {
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
                if (constructor.getParameterCount() == args.size()) {
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
            System.err.println("[DEBUG createNativeInstance] class not found: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("[DEBUG createNativeInstance] exception: " + e.getMessage());
            return null;
        }
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
    
    private Thread createThread(List<Object> args) {
        if (args.isEmpty()) {
            return new Thread();
        }
        
        Object target = args.get(0);
        
        if (target instanceof LambdaObject) {
            LambdaObject lambda = (LambdaObject) target;
            Environment capturedEnv = interpreter.getCurrentEnv();
            ScriptClass capturedClass = capturedEnv != null ? capturedEnv.getCurrentClass() : null;
            Runnable runnable = () -> {
                Environment threadEnv = new Environment(interpreter.getGlobalEnv());
                if (capturedClass != null) {
                    threadEnv.setCurrentClass(capturedClass);
                }
                interpreter.setCurrentEnv(threadEnv);
                try {
                    invokeLambda(lambda, new ArrayList<>());
                } finally {
                    interpreter.setCurrentEnv(capturedEnv);
                }
            };
            return new Thread(runnable);
        }
        
        if (target instanceof MethodReferenceObject) {
            MethodReferenceObject methodRef = (MethodReferenceObject) target;
            Environment capturedEnv = interpreter.getCurrentEnv();
            ScriptClass capturedClass = capturedEnv != null ? capturedEnv.getCurrentClass() : null;
            Runnable runnable = () -> {
                Environment threadEnv = new Environment(interpreter.getGlobalEnv());
                if (capturedClass != null) {
                    threadEnv.setCurrentClass(capturedClass);
                }
                interpreter.setCurrentEnv(threadEnv);
                try {
                    invokeMethodReference(methodRef, new ArrayList<>());
                } finally {
                    interpreter.setCurrentEnv(capturedEnv);
                }
            };
            return new Thread(runnable);
        }
        
        if (target instanceof RuntimeObject) {
            RuntimeObject obj = (RuntimeObject) target;
            ScriptMethod runMethod = obj.getScriptClass().getMethod("run", new ArrayList<>());
            if (runMethod != null) {
                Environment capturedEnv = interpreter.getCurrentEnv();
                ScriptClass capturedClass = capturedEnv != null ? capturedEnv.getCurrentClass() : null;
                Runnable runnable = () -> {
                    Environment threadEnv = new Environment(interpreter.getGlobalEnv());
                    if (capturedClass != null) {
                        threadEnv.setCurrentClass(capturedClass);
                    }
                    threadEnv.setThisObject(null);
                    
                    for (Map.Entry<String, Object> entry : obj.getCapturedVariables().entrySet()) {
                        threadEnv.defineVariable(entry.getKey(), entry.getValue());
                    }
                    
                    interpreter.setCurrentEnv(threadEnv);
                    try {
                        interpreter.invokeMethod(obj, runMethod, new ArrayList<>());
                    } catch (InterpreterException e) {
                        System.err.println(e.getFullStackTrace());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        interpreter.setCurrentEnv(capturedEnv);
                    }
                };
                return new Thread(runnable);
            }
        }
        
        if (target instanceof Runnable) {
            return new Thread((Runnable) target);
        }
        
        return new Thread();
    }
    
    public ScriptClass getStandardClass(String name) {
        return standardClasses.get(name);
    }
    
    public Object resolveStaticImport(String name) {
        return staticImportRegistry.resolve(name);
    }
    
    public StaticImportRegistry getStaticImportRegistry() {
        return staticImportRegistry;
    }
    
    public static class SystemHolder {
        public Object getField(String name) {
            if (name.equals("out")) return System.out;
            if (name.equals("err")) return System.err;
            return null;
        }
        
        public Object invokeMethod(String methodName, List<Object> args) {
            if (methodName.equals("currentTimeMillis")) {
                return System.currentTimeMillis();
            }
            if (methodName.equals("nanoTime")) {
                return System.nanoTime();
            }
            if (methodName.equals("exit")) {
                System.exit(((Number) args.get(0)).intValue());
                return null;
            }
            if (methodName.equals("gc")) {
                System.gc();
                return null;
            }
            return null;
        }
    }
    
    public class StaticMethodHolder {
        private final String methodName;
        
        public StaticMethodHolder(String methodName) {
            this.methodName = methodName;
        }
        
        public Object invoke(List<Object> args) {
            return invokeStaticMethod(methodName, args);
        }
    }
    
    private Object invokeStaticMethod(String methodName, List<Object> args) {
        try {
            switch (methodName) {
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
    
    private Object invokeLambda(LambdaObject lambda, List<Object> args) {
        LambdaExpression lambdaExpr = lambda.getLambda();
        Environment closureEnv = lambda.getClosureEnv();
        Environment previous = interpreter.getCurrentEnv();
        Environment newEnv = new Environment(closureEnv);
        interpreter.setCurrentEnv(newEnv);
        
        try {
            List<LambdaExpression.LambdaParameter> params = lambdaExpr.getParameters();
            for (int i = 0; i < args.size() && i < params.size(); i++) {
                newEnv.defineVariable(params.get(i).getName(), args.get(i));
            }
            
            ASTNode body = lambdaExpr.getBody();
            if (body instanceof Expression) {
                return ((Expression) body).accept(interpreter);
            } else if (body instanceof BlockStatement) {
                try {
                    ((BlockStatement) body).accept(interpreter);
                    return null;
                } catch (ReturnException e) {
                    return e.getValue();
                }
            }
        } finally {
            interpreter.setCurrentEnv(previous);
        }
        
        return null;
    }
    
    private Object invokeMethodReference(MethodReferenceObject methodRefObj, List<Object> args) {
        MethodReferenceExpression methodRef = methodRefObj.getMethodRef();
        Expression targetExpr = methodRef.getTarget();
        String methodName = methodRef.getMethodName();
        
        if (methodName.equals("new")) {
            if (targetExpr instanceof ClassLiteralExpression) {
                cn.langlang.javainterpreter.ast.type.Type type = ((ClassLiteralExpression) targetExpr).getType();
                if (type.getArrayDimensions() > 0 || type.getName().equals("int") || 
                    type.getName().equals("long") || type.getName().equals("double")) {
                    int size = args.isEmpty() ? 0 : ((Number) args.get(0)).intValue();
                    return createArray(type, size);
                }
            }
        }
        
        if (targetExpr != null) {
            Object targetObj = targetExpr.accept(interpreter);
            if (targetObj instanceof ScriptClass) {
                ScriptClass scriptClass = (ScriptClass) targetObj;
                ScriptMethod method = scriptClass.getMethod(methodName, args);
                if (method != null && method.isStatic()) {
                    return interpreter.invokeMethod(null, method, args);
                }
            }
            if (targetObj instanceof RuntimeObject) {
                RuntimeObject runtimeObj = (RuntimeObject) targetObj;
                ScriptMethod method = runtimeObj.getScriptClass().getMethod(methodName, args);
                if (method != null) {
                    return interpreter.invokeMethod(runtimeObj, method, args);
                }
            }
            return invokeMethod(targetObj, methodName, args);
        }
        
        return null;
    }
    
    private Object createArray(cn.langlang.javainterpreter.ast.type.Type type, int size) {
        String typeName = type.getName();
        int dims = type.getArrayDimensions();
        
        if (dims > 1 || typeName.equals("int")) return new int[size];
        if (typeName.equals("long")) return new long[size];
        if (typeName.equals("double")) return new double[size];
        if (typeName.equals("float")) return new float[size];
        if (typeName.equals("boolean")) return new boolean[size];
        if (typeName.equals("char")) return new char[size];
        if (typeName.equals("byte")) return new byte[size];
        if (typeName.equals("short")) return new short[size];
        return new Object[size];
    }
    
    private Object invokeClassMethod(Class<?> clazz, String methodName, List<Object> args) {
        try {
            switch (methodName) {
                case "forName":
                    String className = (String) args.get(0);
                    return Class.forName(className);
                case "getName":
                    return clazz.getName();
                case "getSimpleName":
                    return clazz.getSimpleName();
                case "getCanonicalName":
                    return clazz.getCanonicalName();
                case "isInterface":
                    return clazz.isInterface();
                case "isArray":
                    return clazz.isArray();
                case "isPrimitive":
                    return clazz.isPrimitive();
                case "isAnnotation":
                    return clazz.isAnnotation();
                case "isEnum":
                    return clazz.isEnum();
                case "getSuperclass":
                    return clazz.getSuperclass();
                case "getInterfaces":
                    return clazz.getInterfaces();
                case "getMethods":
                    return clazz.getMethods();
                case "getDeclaredMethods":
                    return clazz.getDeclaredMethods();
                case "getFields":
                    return clazz.getFields();
                case "getDeclaredFields":
                    return clazz.getDeclaredFields();
                case "getConstructors":
                    return clazz.getConstructors();
                case "getDeclaredConstructors":
                    return clazz.getDeclaredConstructors();
                case "getMethod":
                    String methodNameArg = (String) args.get(0);
                    Class<?>[] paramTypes = extractParamTypes(args, 1);
                    return clazz.getMethod(methodNameArg, paramTypes);
                case "getDeclaredMethod":
                    String declaredMethodName = (String) args.get(0);
                    Class<?>[] declaredParamTypes = extractParamTypes(args, 1);
                    return clazz.getDeclaredMethod(declaredMethodName, declaredParamTypes);
                case "getField":
                    return clazz.getField((String) args.get(0));
                case "getDeclaredField":
                    return clazz.getDeclaredField((String) args.get(0));
                case "getConstructor":
                    return clazz.getConstructor(extractParamTypes(args, 0));
                case "getDeclaredConstructor":
                    return clazz.getDeclaredConstructor(extractParamTypes(args, 0));
                case "newInstance":
                    return clazz.getDeclaredConstructor().newInstance();
                case "getClassLoader":
                    return clazz.getClassLoader();
                case "getModifiers":
                    return clazz.getModifiers();
                case "getPackage":
                    return clazz.getPackage();
                case "cast":
                    return clazz.cast(args.get(0));
                case "isInstance":
                    return clazz.isInstance(args.get(0));
                case "isAssignableFrom":
                    Class<?> otherClass = (Class<?>) args.get(0);
                    return clazz.isAssignableFrom(otherClass);
                default:
                    return invokeStaticMethodByReflection(clazz, methodName, args);
            }
        } catch (Exception e) {
            throw new RuntimeException("Reflection error: " + e.getMessage(), e);
        }
    }
    
    private Object invokeStaticMethodByReflection(Class<?> clazz, String methodName, List<Object> args) {
        try {
            for (java.lang.reflect.Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && 
                    java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    if (method.isVarArgs()) {
                        int fixedParams = method.getParameterCount() - 1;
                        if (args.size() >= fixedParams) {
                            method.setAccessible(true);
                            Object[] argsArray = convertArgsForVarargsMethod(method, args);
                            return method.invoke(null, argsArray);
                        }
                    } else if (method.getParameterCount() == args.size()) {
                        method.setAccessible(true);
                        Object[] argsArray = convertArgsForMethod(method, args);
                        return method.invoke(null, argsArray);
                    }
                }
            }
            
            for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && 
                    java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    if (method.isVarArgs()) {
                        int fixedParams = method.getParameterCount() - 1;
                        if (args.size() >= fixedParams) {
                            method.setAccessible(true);
                            Object[] argsArray = convertArgsForVarargsMethod(method, args);
                            return method.invoke(null, argsArray);
                        }
                    } else if (method.getParameterCount() == args.size()) {
                        method.setAccessible(true);
                        Object[] argsArray = convertArgsForMethod(method, args);
                        return method.invoke(null, argsArray);
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Static method invocation error: " + e.getMessage(), e);
        }
    }
    
    private Object[] convertArgsForVarargsMethod(java.lang.reflect.Method method, List<Object> args) {
        Class<?>[] paramTypes = method.getParameterTypes();
        int fixedParams = paramTypes.length - 1;
        Object[] converted = new Object[paramTypes.length];
        
        for (int i = 0; i < fixedParams; i++) {
            Object arg = args.get(i);
            Class<?> paramType = paramTypes[i];
            converted[i] = convertSingleArg(arg, paramType);
        }
        
        Class<?> varargType = paramTypes[fixedParams];
        Class<?> componentType = varargType.getComponentType();
        int varargCount = args.size() - fixedParams;
        Object varargArray = java.lang.reflect.Array.newInstance(componentType, varargCount);
        
        for (int i = 0; i < varargCount; i++) {
            Object arg = args.get(fixedParams + i);
            Object convertedArg = convertSingleArg(arg, componentType);
            java.lang.reflect.Array.set(varargArray, i, convertedArg);
        }
        converted[fixedParams] = varargArray;
        
        return converted;
    }
    
    private Object convertSingleArg(Object arg, Class<?> paramType) {
        if (arg == null) {
            return null;
        } else if (arg instanceof LambdaObject) {
            return convertLambdaToFunctionalInterface((LambdaObject) arg, paramType);
        } else if (arg instanceof MethodReferenceObject) {
            return convertMethodReferenceToFunctionalInterface((MethodReferenceObject) arg, paramType);
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
    
    private Object[] convertArgsForMethod(java.lang.reflect.Method method, List<Object> args) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] converted = new Object[args.size()];
        
        for (int i = 0; i < args.size(); i++) {
            Object arg = args.get(i);
            Class<?> paramType = paramTypes[i];
            
            if (arg == null) {
                converted[i] = null;
            } else if (arg instanceof LambdaObject) {
                converted[i] = convertLambdaToFunctionalInterface((LambdaObject) arg, paramType);
            } else if (arg instanceof MethodReferenceObject) {
                converted[i] = convertMethodReferenceToFunctionalInterface((MethodReferenceObject) arg, paramType);
            } else if (paramType.isArray()) {
                if (paramType.isInstance(arg)) {
                    converted[i] = arg;
                } else if (arg instanceof List) {
                    List<?> list = (List<?>) arg;
                    Class<?> componentType = paramType.getComponentType();
                    if (componentType == int.class) {
                        int[] arr = new int[list.size()];
                        for (int j = 0; j < list.size(); j++) {
                            arr[j] = ((Number) list.get(j)).intValue();
                        }
                        converted[i] = arr;
                    } else if (componentType == long.class) {
                        long[] arr = new long[list.size()];
                        for (int j = 0; j < list.size(); j++) {
                            arr[j] = ((Number) list.get(j)).longValue();
                        }
                        converted[i] = arr;
                    } else if (componentType == double.class) {
                        double[] arr = new double[list.size()];
                        for (int j = 0; j < list.size(); j++) {
                            arr[j] = ((Number) list.get(j)).doubleValue();
                        }
                        converted[i] = arr;
                    } else {
                        converted[i] = list.toArray();
                    }
                } else if (arg instanceof Object[]) {
                    Object[] objArr = (Object[]) arg;
                    Class<?> componentType = paramType.getComponentType();
                    if (componentType == int.class) {
                        int[] arr = new int[objArr.length];
                        for (int j = 0; j < objArr.length; j++) {
                            arr[j] = objArr[j] != null ? ((Number) objArr[j]).intValue() : 0;
                        }
                        converted[i] = arr;
                    } else if (componentType == long.class) {
                        long[] arr = new long[objArr.length];
                        for (int j = 0; j < objArr.length; j++) {
                            arr[j] = objArr[j] != null ? ((Number) objArr[j]).longValue() : 0L;
                        }
                        converted[i] = arr;
                    } else if (componentType == double.class) {
                        double[] arr = new double[objArr.length];
                        for (int j = 0; j < objArr.length; j++) {
                            arr[j] = objArr[j] != null ? ((Number) objArr[j]).doubleValue() : 0.0;
                        }
                        converted[i] = arr;
                    } else if (componentType == float.class) {
                        float[] arr = new float[objArr.length];
                        for (int j = 0; j < objArr.length; j++) {
                            arr[j] = objArr[j] != null ? ((Number) objArr[j]).floatValue() : 0.0f;
                        }
                        converted[i] = arr;
                    } else if (componentType == boolean.class) {
                        boolean[] arr = new boolean[objArr.length];
                        for (int j = 0; j < objArr.length; j++) {
                            arr[j] = objArr[j] != null && (Boolean) objArr[j];
                        }
                        converted[i] = arr;
                    } else if (componentType == char.class) {
                        char[] arr = new char[objArr.length];
                        for (int j = 0; j < objArr.length; j++) {
                            arr[j] = objArr[j] != null ? (Character) objArr[j] : '\0';
                        }
                        converted[i] = arr;
                    } else if (componentType == byte.class) {
                        byte[] arr = new byte[objArr.length];
                        for (int j = 0; j < objArr.length; j++) {
                            arr[j] = objArr[j] != null ? ((Number) objArr[j]).byteValue() : 0;
                        }
                        converted[i] = arr;
                    } else if (componentType == short.class) {
                        short[] arr = new short[objArr.length];
                        for (int j = 0; j < objArr.length; j++) {
                            arr[j] = objArr[j] != null ? ((Number) objArr[j]).shortValue() : 0;
                        }
                        converted[i] = arr;
                    } else {
                        converted[i] = arg;
                    }
                } else {
                    converted[i] = arg;
                }
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
    
    private Object convertLambdaToFunctionalInterface(LambdaObject lambda, Class<?> functionalInterface) {
        if (java.util.function.Supplier.class.isAssignableFrom(functionalInterface)) {
            return (java.util.function.Supplier<Object>) () -> {
                return invokeLambda(lambda, new ArrayList<>());
            };
        } else if (java.util.function.Consumer.class.isAssignableFrom(functionalInterface)) {
            return (java.util.function.Consumer<Object>) (arg) -> {
                invokeLambda(lambda, Arrays.asList(arg));
            };
        } else if (java.util.function.Function.class.isAssignableFrom(functionalInterface)) {
            return (java.util.function.Function<Object, Object>) (arg) -> {
                return invokeLambda(lambda, Arrays.asList(arg));
            };
        } else if (java.util.function.Predicate.class.isAssignableFrom(functionalInterface)) {
            return (java.util.function.Predicate<Object>) (arg) -> {
                Object result = invokeLambda(lambda, Arrays.asList(arg));
                return interpreter.toBoolean(result);
            };
        } else if (java.util.function.BiFunction.class.isAssignableFrom(functionalInterface)) {
            return (java.util.function.BiFunction<Object, Object, Object>) (a, b) -> {
                return invokeLambda(lambda, Arrays.asList(a, b));
            };
        } else if (java.util.function.BiConsumer.class.isAssignableFrom(functionalInterface)) {
            return (java.util.function.BiConsumer<Object, Object>) (a, b) -> {
                invokeLambda(lambda, Arrays.asList(a, b));
            };
        } else if (Runnable.class.isAssignableFrom(functionalInterface)) {
            return (Runnable) () -> {
                invokeLambda(lambda, new ArrayList<>());
            };
        }
        return lambda;
    }
    
    private Object convertMethodReferenceToFunctionalInterface(MethodReferenceObject methodRef, Class<?> functionalInterface) {
        if (java.util.function.Supplier.class.isAssignableFrom(functionalInterface)) {
            return (java.util.function.Supplier<Object>) () -> {
                return invokeMethodReference(methodRef, new ArrayList<>());
            };
        } else if (java.util.function.Consumer.class.isAssignableFrom(functionalInterface)) {
            return (java.util.function.Consumer<Object>) (arg) -> {
                invokeMethodReference(methodRef, Arrays.asList(arg));
            };
        } else if (java.util.function.Function.class.isAssignableFrom(functionalInterface)) {
            return (java.util.function.Function<Object, Object>) (arg) -> {
                return invokeMethodReference(methodRef, Arrays.asList(arg));
            };
        }
        return methodRef;
    }
    
    private Class<?>[] extractParamTypes(List<Object> args, int startIndex) {
        if (args.size() <= startIndex) {
            return new Class<?>[0];
        }
        List<Class<?>> types = new ArrayList<>();
        for (int i = startIndex; i < args.size(); i++) {
            Object arg = args.get(i);
            if (arg instanceof Class) {
                types.add((Class<?>) arg);
            }
        }
        return types.toArray(new Class<?>[0]);
    }
    
    private Object invokeReflectionMethod(Method method, String methodName, List<Object> args) {
        try {
            if (methodName.equals("invoke")) {
                if (args.isEmpty()) {
                    return method.invoke(null);
                }
                Object invokeTarget = args.get(0);
                Object[] invokeArgs = args.size() > 1 ? 
                    args.subList(1, args.size()).toArray() : new Object[0];
                return method.invoke(invokeTarget, invokeArgs);
            }
            
            switch (methodName) {
                case "getName":
                    return method.getName();
                case "getReturnType":
                    return method.getReturnType();
                case "getParameterTypes":
                    return method.getParameterTypes();
                case "getDeclaringClass":
                    return method.getDeclaringClass();
                case "getModifiers":
                    return method.getModifiers();
                case "setAccessible":
                    method.setAccessible((Boolean) args.get(0));
                    return null;
                case "getAnnotation":
                    @SuppressWarnings("unchecked")
                    Class<? extends java.lang.annotation.Annotation> annotationClass = 
                        (Class<? extends java.lang.annotation.Annotation>) args.get(0);
                    return method.getAnnotation(annotationClass);
                case "getAnnotations":
                    return method.getAnnotations();
                case "isAccessible":
                    return method.isAccessible();
                default:
                    return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Method reflection error: " + e.getMessage(), e);
        }
    }
    
    private Object invokeFieldMethod(Field field, List<Object> args) {
        try {
            String methodName = "";
            if (args.isEmpty()) {
                if (field.getType() == int.class) return field.getInt(null);
                if (field.getType() == long.class) return field.getLong(null);
                if (field.getType() == double.class) return field.getDouble(null);
                if (field.getType() == float.class) return field.getFloat(null);
                if (field.getType() == boolean.class) return field.getBoolean(null);
                if (field.getType() == char.class) return field.getChar(null);
                if (field.getType() == short.class) return field.getShort(null);
                if (field.getType() == byte.class) return field.getByte(null);
                return field.get(null);
            }
            
            Object arg = args.get(0);
            if (arg instanceof String) {
                methodName = (String) arg;
            } else if (arg instanceof Boolean) {
                field.setAccessible((Boolean) arg);
                return null;
            } else {
                if (args.size() == 1) {
                    return field.get(arg);
                } else {
                    field.set(arg, args.get(1));
                    return null;
                }
            }
            
            switch (methodName) {
                case "getName":
                    return field.getName();
                case "getType":
                    return field.getType();
                case "getDeclaringClass":
                    return field.getDeclaringClass();
                case "getModifiers":
                    return field.getModifiers();
                case "setAccessible":
                    field.setAccessible((Boolean) args.get(1));
                    return null;
                case "getAnnotation":
                    @SuppressWarnings("unchecked")
                    Class<? extends java.lang.annotation.Annotation> fieldAnnotationClass = 
                        (Class<? extends java.lang.annotation.Annotation>) args.get(1);
                    return field.getAnnotation(fieldAnnotationClass);
                case "getAnnotations":
                    return field.getAnnotations();
                case "isAccessible":
                    return field.isAccessible();
                default:
                    return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Field reflection error: " + e.getMessage(), e);
        }
    }
    
    private Object invokeConstructorMethod(Constructor<?> constructor, List<Object> args) {
        try {
            String methodName = args.isEmpty() ? "newInstance" : "";
            if (args.size() > 0 && args.get(0) instanceof String) {
                methodName = (String) args.get(0);
            }
            
            switch (methodName) {
                case "newInstance":
                    Object[] initArgs = args.isEmpty() ? new Object[0] : args.toArray();
                    return constructor.newInstance(initArgs);
                case "getName":
                    return constructor.getName();
                case "getParameterTypes":
                    return constructor.getParameterTypes();
                case "getDeclaringClass":
                    return constructor.getDeclaringClass();
                case "getModifiers":
                    return constructor.getModifiers();
                case "setAccessible":
                    constructor.setAccessible((Boolean) args.get(1));
                    return null;
                default:
                    Object[] createArgs = args.toArray();
                    return constructor.newInstance(createArgs);
            }
        } catch (Exception e) {
            throw new RuntimeException("Constructor reflection error: " + e.getMessage(), e);
        }
    }
    
    private Object invokeScriptClassMethod(ScriptClass scriptClass, String methodName, List<Object> args) {
        try {
            switch (methodName) {
                case "getName":
                    return scriptClass.getName();
                case "getSimpleName":
                    return scriptClass.getName();
                case "getCanonicalName":
                    return scriptClass.getQualifiedName();
                case "isInterface":
                    return scriptClass.getAstNode() instanceof InterfaceDeclaration;
                case "isArray":
                    return false;
                case "isPrimitive":
                    return false;
                case "getSuperclass":
                    return scriptClass.getSuperClass();
                case "getInterfaces":
                    return scriptClass.getInterfaces();
                case "getFields":
                    return new java.util.ArrayList<>(scriptClass.getFields().values());
                case "getMethods":
                    java.util.List<ScriptMethod> allMethods = new java.util.ArrayList<>();
                    for (java.util.List<ScriptMethod> methodList : scriptClass.getMethods().values()) {
                        allMethods.addAll(methodList);
                    }
                    return allMethods;
                case "getConstructors":
                    return scriptClass.getConstructors();
                case "getField":
                    return scriptClass.getField((String) args.get(0));
                case "getMethod":
                    String methodNameArg = (String) args.get(0);
                    return scriptClass.getMethod(methodNameArg, args.size() > 1 ? args.subList(1, args.size()) : new java.util.ArrayList<>());
                case "newInstance":
                    ScriptMethod constructor = findBestConstructor(scriptClass, args);
                    if (constructor != null) {
                        RuntimeObject instance = new RuntimeObject(scriptClass);
                        return instance;
                    }
                    return null;
                default:
                    return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("ScriptClass reflection error: " + e.getMessage(), e);
        }
    }
    
    private ScriptMethod findBestConstructor(ScriptClass scriptClass, List<Object> args) {
        for (ScriptMethod constructor : scriptClass.getConstructors()) {
            if (constructor.getParameters().size() == args.size()) {
                return constructor;
            }
        }
        return scriptClass.getConstructors().isEmpty() ? null : scriptClass.getConstructors().get(0);
    }
    
    private Object invokeScriptMethodMethod(ScriptMethod scriptMethod, String methodName, List<Object> args) {
        try {
            switch (methodName) {
                case "getName":
                    return scriptMethod.getName();
                case "getReturnType":
                    return scriptMethod.getReturnType();
                case "getParameterTypes":
                    return scriptMethod.getParameters();
                case "getDeclaringClass":
                    return scriptMethod.getDeclaringClass();
                case "getModifiers":
                    return scriptMethod.getModifiers();
                case "getAnnotations":
                    return scriptMethod.getAnnotations();
                case "getAnnotation":
                    String annotationName = (String) args.get(0);
                    return scriptMethod.getAnnotation(annotationName);
                case "isVarArgs":
                    return scriptMethod.isVarArgs();
                case "isDefault":
                    return scriptMethod.isDefault();
                case "isConstructor":
                    return scriptMethod.isConstructor();
                case "toString":
                    return "ScriptMethod[" + scriptMethod.getName() + "]";
                default:
                    return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("ScriptMethod reflection error: " + e.getMessage(), e);
        }
    }
    
    private Object invokeScriptFieldMethod(ScriptField scriptField, String methodName, List<Object> args) {
        try {
            switch (methodName) {
                case "getName":
                    return scriptField.getName();
                case "getType":
                    return scriptField.getType();
                case "getDeclaringClass":
                    return scriptField.getDeclaringClass();
                case "getModifiers":
                    return scriptField.getModifiers();
                case "getAnnotations":
                    return scriptField.getAnnotations();
                case "getAnnotation":
                    String annotationName = (String) args.get(0);
                    return scriptField.getAnnotation(annotationName);
                case "isStatic":
                    return scriptField.isStatic();
                case "isFinal":
                    return scriptField.isFinal();
                case "toString":
                    return "ScriptField[" + scriptField.getName() + "]";
                default:
                    return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("ScriptField reflection error: " + e.getMessage(), e);
        }
    }
}
