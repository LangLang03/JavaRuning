package cn.langlang.javainterpreter.runtime;

import cn.langlang.javainterpreter.interpreter.*;
import cn.langlang.javainterpreter.ast.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

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
            default:
                return null;
        }
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
            default: return null;
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
}
