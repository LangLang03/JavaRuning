package cn.langlang.javainterpreter.runtime;

import cn.langlang.javainterpreter.interpreter.*;
import cn.langlang.javainterpreter.ast.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.lang.reflect.*;

public class StandardLibrary {
    private final Interpreter interpreter;
    private final Map<String, ScriptClass> standardClasses;
    
    public StandardLibrary(Interpreter interpreter) {
        this.interpreter = interpreter;
        this.standardClasses = new HashMap<>();
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
        mathMethods.put("abs", (Function<Double, Double>) Math::abs);
        mathMethods.put("max", (BiFunction<Double, Double, Double>) Math::max);
        mathMethods.put("min", (BiFunction<Double, Double, Double>) Math::min);
        mathMethods.put("sqrt", (Function<Double, Double>) Math::sqrt);
        mathMethods.put("pow", (BiFunction<Double, Double, Double>) Math::pow);
        mathMethods.put("sin", (Function<Double, Double>) Math::sin);
        mathMethods.put("cos", (Function<Double, Double>) Math::cos);
        mathMethods.put("tan", (Function<Double, Double>) Math::tan);
        mathMethods.put("log", (Function<Double, Double>) Math::log);
        mathMethods.put("exp", (Function<Double, Double>) Math::exp);
        mathMethods.put("floor", (Function<Double, Double>) Math::floor);
        mathMethods.put("ceil", (Function<Double, Double>) Math::ceil);
        mathMethods.put("round", (Function<Double, Long>) Math::round);
        mathMethods.put("random", (Supplier<Double>) Math::random);
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
    
    public Object invokeMethod(Object target, String methodName, List<Object> args) {
        if (target instanceof StaticMethodHolder) {
            return ((StaticMethodHolder) target).invoke(args);
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
        
        if (target instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) target;
            if (methodName.equals("get")) {
                return map.get(args.get(0));
            } else if (methodName.equals("put")) {
                return ((Map) map).put(args.get(0), args.get(1));
            } else if (methodName.equals("containsKey")) {
                return map.containsKey(args.get(0));
            } else if (methodName.equals("keySet")) {
                return map.keySet();
            } else if (methodName.equals("values")) {
                return map.values();
            } else if (methodName.equals("size")) {
                return map.size();
            }
        }
        
        if (target instanceof List) {
            List<?> list = (List<?>) target;
            if (methodName.equals("get")) {
                return list.get((Integer) args.get(0));
            } else if (methodName.equals("add")) {
                ((List) target).add(args.get(0));
                return true;
            } else if (methodName.equals("remove")) {
                if (args.get(0) instanceof Integer) {
                    return list.remove((int) args.get(0));
                }
                return ((List) target).remove(args.get(0));
            } else if (methodName.equals("size")) {
                return list.size();
            } else if (methodName.equals("isEmpty")) {
                return list.isEmpty();
            } else if (methodName.equals("contains")) {
                return list.contains(args.get(0));
            } else if (methodName.equals("clear")) {
                ((List) target).clear();
                return null;
            } else if (methodName.equals("forEach")) {
                Object consumer = args.get(0);
                if (consumer instanceof LambdaObject) {
                    LambdaObject lambda = (LambdaObject) consumer;
                    for (Object elem : list) {
                        invokeLambda(lambda, Arrays.asList(elem));
                    }
                }
                return null;
            } else if (methodName.equals("stream")) {
                return ((List<?>) target).stream();
            }
        }
        
        if (target instanceof Set) {
            Set<?> set = (Set<?>) target;
            if (methodName.equals("add")) {
                return ((Set) target).add(args.get(0));
            } else if (methodName.equals("contains")) {
                return set.contains(args.get(0));
            } else if (methodName.equals("remove")) {
                return ((Set) target).remove(args.get(0));
            } else if (methodName.equals("size")) {
                return set.size();
            } else if (methodName.equals("isEmpty")) {
                return set.isEmpty();
            }
        }
        
        if (target instanceof Stream) {
            Stream<?> stream = (Stream<?>) target;
            if (methodName.equals("forEach")) {
                Object consumer = args.get(0);
                if (consumer instanceof LambdaObject) {
                    LambdaObject lambda = (LambdaObject) consumer;
                    stream.forEach(elem -> invokeLambda(lambda, Arrays.asList(elem)));
                }
                return null;
            } else if (methodName.equals("map")) {
                Object mapper = args.get(0);
                if (mapper instanceof LambdaObject) {
                    LambdaObject lambda = (LambdaObject) mapper;
                    return stream.map(elem -> invokeLambda(lambda, Arrays.asList(elem)));
                }
            } else if (methodName.equals("filter")) {
                Object predicate = args.get(0);
                if (predicate instanceof LambdaObject) {
                    LambdaObject lambda = (LambdaObject) predicate;
                    return stream.filter(elem -> (Boolean) invokeLambda(lambda, Arrays.asList(elem)));
                }
            } else if (methodName.equals("collect")) {
                Object collector = args.get(0);
                if (collector instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) collector;
                    if (map.containsKey("type")) {
                        String type = (String) map.get("type");
                        if (type.equals("toList")) {
                            return stream.collect(Collectors.toList());
                        } else if (type.equals("toSet")) {
                            return stream.collect(Collectors.toSet());
                        }
                    }
                }
            } else if (methodName.equals("count")) {
                return stream.count();
            } else if (methodName.equals("findFirst")) {
                return stream.findFirst();
            }
        }
        
        if (target instanceof String) {
            String str = (String) target;
            if (methodName.equals("length")) {
                return str.length();
            } else if (methodName.equals("charAt")) {
                return str.charAt((Integer) args.get(0));
            } else if (methodName.equals("substring")) {
                if (args.size() == 1) {
                    return str.substring((Integer) args.get(0));
                }
                return str.substring((Integer) args.get(0), (Integer) args.get(1));
            } else if (methodName.equals("indexOf")) {
                return str.indexOf((String) args.get(0));
            } else if (methodName.equals("contains")) {
                return str.contains((String) args.get(0));
            } else if (methodName.equals("startsWith")) {
                return str.startsWith((String) args.get(0));
            } else if (methodName.equals("endsWith")) {
                return str.endsWith((String) args.get(0));
            } else if (methodName.equals("trim")) {
                return str.trim();
            } else if (methodName.equals("toLowerCase")) {
                return str.toLowerCase();
            } else if (methodName.equals("toUpperCase")) {
                return str.toUpperCase();
            } else if (methodName.equals("replace")) {
                return str.replace((CharSequence) args.get(0), (CharSequence) args.get(1));
            } else if (methodName.equals("split")) {
                return str.split((String) args.get(0));
            } else if (methodName.equals("equals")) {
                return str.equals(args.get(0));
            } else if (methodName.equals("equalsIgnoreCase")) {
                return str.equalsIgnoreCase((String) args.get(0));
            } else if (methodName.equals("compareTo")) {
                return str.compareTo((String) args.get(0));
            } else if (methodName.equals("compareToIgnoreCase")) {
                return str.compareToIgnoreCase((String) args.get(0));
            } else if (methodName.equals("isEmpty")) {
                return str.isEmpty();
            } else if (methodName.equals("concat")) {
                return str.concat((String) args.get(0));
            } else if (methodName.equals("getBytes")) {
                return str.getBytes();
            } else if (methodName.equals("toCharArray")) {
                return str.toCharArray();
            }
        }
        
        if (target instanceof Integer) {
            Integer num = (Integer) target;
            if (methodName.equals("intValue")) {
                return num.intValue();
            } else if (methodName.equals("longValue")) {
                return num.longValue();
            } else if (methodName.equals("doubleValue")) {
                return num.doubleValue();
            } else if (methodName.equals("floatValue")) {
                return num.floatValue();
            } else if (methodName.equals("shortValue")) {
                return num.shortValue();
            } else if (methodName.equals("byteValue")) {
                return num.byteValue();
            } else if (methodName.equals("compareTo")) {
                return num.compareTo((Integer) args.get(0));
            } else if (methodName.equals("toString")) {
                return num.toString();
            } else if (methodName.equals("equals")) {
                return num.equals(args.get(0));
            }
        }
        
        if (target instanceof Long) {
            Long num = (Long) target;
            if (methodName.equals("intValue")) {
                return num.intValue();
            } else if (methodName.equals("longValue")) {
                return num.longValue();
            } else if (methodName.equals("doubleValue")) {
                return num.doubleValue();
            }
        }
        
        if (target instanceof Double) {
            Double num = (Double) target;
            if (methodName.equals("intValue")) {
                return num.intValue();
            } else if (methodName.equals("longValue")) {
                return num.longValue();
            } else if (methodName.equals("doubleValue")) {
                return num.doubleValue();
            }
        }
        
        if (target instanceof Float) {
            Float num = (Float) target;
            if (methodName.equals("intValue")) {
                return num.intValue();
            } else if (methodName.equals("floatValue")) {
                return num.floatValue();
            } else if (methodName.equals("doubleValue")) {
                return num.doubleValue();
            }
        }
        
        if (target instanceof Character) {
            Character ch = (Character) target;
            if (methodName.equals("charValue")) {
                return ch.charValue();
            } else if (methodName.equals("compareTo")) {
                return ch.compareTo((Character) args.get(0));
            } else if (methodName.equals("equals")) {
                return ch.equals(args.get(0));
            }
        }
        
        if (target instanceof Boolean) {
            Boolean bool = (Boolean) target;
            if (methodName.equals("booleanValue")) {
                return bool.booleanValue();
            } else if (methodName.equals("compareTo")) {
                return bool.compareTo((Boolean) args.get(0));
            }
        }
        
        if (target instanceof java.io.PrintStream) {
            java.io.PrintStream ps = (java.io.PrintStream) target;
            if (methodName.equals("println")) {
                if (args.isEmpty()) {
                    ps.println();
                } else {
                    ps.println(args.get(0));
                }
                return null;
            } else if (methodName.equals("print")) {
                ps.print(args.get(0));
                return null;
            } else if (methodName.equals("printf")) {
                Object[] formatArgs = args.subList(1, args.size()).toArray();
                return ps.printf((String) args.get(0), formatArgs);
            } else if (methodName.equals("flush")) {
                ps.flush();
                return null;
            }
        }
        
        if (target instanceof Optional) {
            Optional<?> opt = (Optional<?>) target;
            if (methodName.equals("isPresent")) {
                return opt.isPresent();
            } else if (methodName.equals("get")) {
                return opt.get();
            } else if (methodName.equals("orElse")) {
                @SuppressWarnings("unchecked")
                Optional<Object> optObj = (Optional<Object>) opt;
                return optObj.orElse(args.get(0));
            } else if (methodName.equals("ifPresent")) {
                Object consumer = args.get(0);
                if (consumer instanceof LambdaObject) {
                    LambdaObject lambda = (LambdaObject) consumer;
                    opt.ifPresent(elem -> invokeLambda(lambda, Arrays.asList(elem)));
                }
                return null;
            } else if (methodName.equals("map")) {
                Object mapper = args.get(0);
                if (mapper instanceof LambdaObject) {
                    LambdaObject lambda = (LambdaObject) mapper;
                    return opt.map(elem -> invokeLambda(lambda, Arrays.asList(elem)));
                }
            }
        }
        
        if (target instanceof Throwable) {
            Throwable throwable = (Throwable) target;
            if (methodName.equals("getMessage")) {
                return throwable.getMessage();
            } else if (methodName.equals("getLocalizedMessage")) {
                return throwable.getLocalizedMessage();
            } else if (methodName.equals("getCause")) {
                return throwable.getCause();
            } else if (methodName.equals("printStackTrace")) {
                throwable.printStackTrace();
                return null;
            } else if (methodName.equals("toString")) {
                return throwable.toString();
            } else if (methodName.equals("fillInStackTrace")) {
                return throwable.fillInStackTrace();
            } else if (methodName.equals("getStackTrace")) {
                return throwable.getStackTrace();
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
        
        try {
            java.lang.reflect.Method method = target.getClass().getMethod(methodName, 
                args.stream().map(Object::getClass).toArray(Class<?>[]::new));
            return method.invoke(target, args.toArray());
        } catch (Exception e) {
            try {
                for (java.lang.reflect.Method method : target.getClass().getMethods()) {
                    if (method.getName().equals(methodName) && method.getParameterCount() == args.size()) {
                        return method.invoke(target, args.toArray());
                    }
                }
            } catch (Exception ex) {
            }
        }
        
        return null;
    }
    
    public Object getField(Object target, String fieldName) {
        if (target instanceof Object[]) {
            if (fieldName.equals("length")) {
                return ((Object[]) target).length;
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
        switch (typeName) {
            case "Thread":
            case "java.lang.Thread":
                return createThread(args);
            case "ArrayList":
            case "java.util.ArrayList":
                return new ArrayList<>();
            case "LinkedList":
            case "java.util.LinkedList":
                return new LinkedList<>();
            case "HashMap":
            case "java.util.HashMap":
                return new HashMap<>();
            case "HashSet":
            case "java.util.HashSet":
                return new HashSet<>();
            case "TreeMap":
            case "java.util.TreeMap":
                return new TreeMap<>();
            case "TreeSet":
            case "java.util.TreeSet":
                return new TreeSet<>();
            case "StringBuilder":
            case "java.lang.StringBuilder":
                return new StringBuilder();
            case "StringBuffer":
            case "java.lang.StringBuffer":
                return new StringBuffer();
            case "Object":
            case "java.lang.Object":
                return new Object();
            case "Integer":
            case "java.lang.Integer":
                if (args.isEmpty()) return 0;
                return ((Number) args.get(0)).intValue();
            case "Long":
            case "java.lang.Long":
                if (args.isEmpty()) return 0L;
                return ((Number) args.get(0)).longValue();
            case "Double":
            case "java.lang.Double":
                if (args.isEmpty()) return 0.0;
                return ((Number) args.get(0)).doubleValue();
            case "Float":
            case "java.lang.Float":
                if (args.isEmpty()) return 0.0f;
                return ((Number) args.get(0)).floatValue();
            case "Boolean":
            case "java.lang.Boolean":
                if (args.isEmpty()) return false;
                return args.get(0);
            case "Character":
            case "java.lang.Character":
                if (args.isEmpty()) return '\0';
                return args.get(0);
            case "String":
            case "java.lang.String":
                if (args.isEmpty()) return "";
                return String.valueOf(args.get(0));
            case "IOException":
            case "java.io.IOException":
                if (args.isEmpty()) return new java.io.IOException();
                return new java.io.IOException(String.valueOf(args.get(0)));
            case "Exception":
            case "java.lang.Exception":
                if (args.isEmpty()) return new Exception();
                return new Exception(String.valueOf(args.get(0)));
            case "RuntimeException":
            case "java.lang.RuntimeException":
                if (args.isEmpty()) return new RuntimeException();
                return new RuntimeException(String.valueOf(args.get(0)));
            case "IllegalArgumentException":
            case "java.lang.IllegalArgumentException":
                if (args.isEmpty()) return new IllegalArgumentException();
                return new IllegalArgumentException(String.valueOf(args.get(0)));
            case "NullPointerException":
            case "java.lang.NullPointerException":
                if (args.isEmpty()) return new NullPointerException();
                return new NullPointerException(String.valueOf(args.get(0)));
            case "ArithmeticException":
            case "java.lang.ArithmeticException":
                if (args.isEmpty()) return new ArithmeticException();
                return new ArithmeticException(String.valueOf(args.get(0)));
            case "IndexOutOfBoundsException":
            case "java.lang.IndexOutOfBoundsException":
                if (args.isEmpty()) return new IndexOutOfBoundsException();
                return new IndexOutOfBoundsException(String.valueOf(args.get(0)));
            case "ArrayIndexOutOfBoundsException":
            case "java.lang.ArrayIndexOutOfBoundsException":
                if (args.isEmpty()) return new ArrayIndexOutOfBoundsException();
                return new ArrayIndexOutOfBoundsException(String.valueOf(args.get(0)));
            case "ClassCastException":
            case "java.lang.ClassCastException":
                if (args.isEmpty()) return new ClassCastException();
                return new ClassCastException(String.valueOf(args.get(0)));
            case "NumberFormatException":
            case "java.lang.NumberFormatException":
                if (args.isEmpty()) return new NumberFormatException();
                return new NumberFormatException(String.valueOf(args.get(0)));
            case "UnsupportedOperationException":
            case "java.lang.UnsupportedOperationException":
                if (args.isEmpty()) return new UnsupportedOperationException();
                return new UnsupportedOperationException(String.valueOf(args.get(0)));
            case "Throwable":
            case "java.lang.Throwable":
                if (args.isEmpty()) return new Throwable();
                return new Throwable(String.valueOf(args.get(0)));
            case "Error":
            case "java.lang.Error":
                if (args.isEmpty()) return new Error();
                return new Error(String.valueOf(args.get(0)));
            default:
                return null;
        }
    }
    
    private Thread createThread(List<Object> args) {
        if (args.isEmpty()) {
            return new Thread();
        }
        
        Object target = args.get(0);
        
        if (target instanceof LambdaObject) {
            LambdaObject lambda = (LambdaObject) target;
            Runnable runnable = () -> invokeLambda(lambda, new ArrayList<>());
            return new Thread(runnable);
        }
        
        if (target instanceof MethodReferenceObject) {
            MethodReferenceObject methodRef = (MethodReferenceObject) target;
            Runnable runnable = () -> invokeMethodReference(methodRef, new ArrayList<>());
            return new Thread(runnable);
        }
        
        if (target instanceof RuntimeObject) {
            RuntimeObject obj = (RuntimeObject) target;
            ScriptMethod runMethod = obj.getScriptClass().getMethod("run", new ArrayList<>());
            if (runMethod != null) {
                Runnable runnable = () -> {
                    try {
                        interpreter.invokeMethod(obj, runMethod, new ArrayList<>());
                    } catch (Exception e) {
                        e.printStackTrace();
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
        switch (name) {
            case "out": return System.out;
            case "err": return System.err;
            case "PI": return Math.PI;
            case "E": return Math.E;
            case "System": return new SystemHolder();
            case "sqrt": return new StaticMethodHolder("sqrt");
            case "pow": return new StaticMethodHolder("pow");
            case "abs": return new StaticMethodHolder("abs");
            case "max": return new StaticMethodHolder("max");
            case "min": return new StaticMethodHolder("min");
            case "sin": return new StaticMethodHolder("sin");
            case "cos": return new StaticMethodHolder("cos");
            case "tan": return new StaticMethodHolder("tan");
            case "log": return new StaticMethodHolder("log");
            case "exp": return new StaticMethodHolder("exp");
            case "floor": return new StaticMethodHolder("floor");
            case "ceil": return new StaticMethodHolder("ceil");
            case "round": return new StaticMethodHolder("round");
            case "random": return new StaticMethodHolder("random");
            default: return null;
        }
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
                cn.langlang.javainterpreter.ast.Type type = ((ClassLiteralExpression) targetExpr).getType();
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
    
    private Object createArray(cn.langlang.javainterpreter.ast.Type type, int size) {
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
                    return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Reflection error: " + e.getMessage(), e);
        }
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
