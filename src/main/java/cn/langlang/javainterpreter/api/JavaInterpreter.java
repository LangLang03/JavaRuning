package cn.langlang.javainterpreter.api;

import cn.langlang.javainterpreter.lexer.*;
import cn.langlang.javainterpreter.parser.*;
import cn.langlang.javainterpreter.parser.Modifier;
import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.interpreter.*;
import cn.langlang.javainterpreter.runtime.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public class JavaInterpreter {
    private final Interpreter interpreter;
    private final Environment globalEnv;
    private final Map<String, CompilationUnit> loadedUnits;
    private String mainClassName;
    
    public JavaInterpreter() {
        this.interpreter = new Interpreter();
        this.globalEnv = interpreter.getGlobalEnvironment();
        this.loadedUnits = new HashMap<>();
    }
    
    public void load(String source) {
        load(source, null);
    }
    
    public void load(String source, String fileName) {
        String trimmedSource = source.trim();

        java.util.regex.Pattern classPattern = java.util.regex.Pattern.compile(
            "(?:^|\\s)(?:public\\s+)?(?:abstract\\s+)?(?:final\\s+)?class\\s+(\\w+)");
        java.util.regex.Matcher classMatcher = classPattern.matcher(trimmedSource);
        if (classMatcher.find()) {
            mainClassName = classMatcher.group(1);
        }

        String processedSource = preprocessSource(source);
        
        if (fileName != null) {
            interpreter.setCurrentFileName(fileName);
        }

        Lexer lexer = new Lexer(processedSource);
        List<Token> tokens = lexer.scanTokens();

        Parser parser = new Parser(tokens);
        CompilationUnit ast = parser.parseCompilationUnit();

        interpreter.interpretDeclarations(ast);
        loadedUnits.put(source.hashCode() + "", ast);
    }
    
    public Object execute(String source) {
        load(source);
        return runMain();
    }
    
    public Object executeFile(String filePath) throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(filePath)));
        interpreter.setCurrentFileName(new File(filePath).getName());
        return execute(source);
    }
    
    public Object runMain() {
        if (mainClassName == null) {
            return null;
        }
        ScriptClass mainClass = globalEnv.getClass(mainClassName);
        if (mainClass == null) {
            return null;
        }
        List<ScriptMethod> methods = mainClass.getMethods().get("main");
        if (methods == null || methods.isEmpty()) {
            return null;
        }
        Object[] emptyArgs = new Object[1];
        emptyArgs[0] = new String[0];
        return interpreter.invokeStaticMethod(mainClass, "main", Arrays.asList(emptyArgs));
    }

    private String preprocessSource(String source) {
        String trimmedSource = source.trim();
        
        java.util.regex.Pattern classPattern = java.util.regex.Pattern.compile(
            "(?:^|\\s)(?:public\\s+)?(?:abstract\\s+)?(?:final\\s+)?class\\s+\\w");
        boolean hasClassDeclaration = classPattern.matcher(trimmedSource).find();
        
        java.util.regex.Pattern interfacePattern = java.util.regex.Pattern.compile(
            "(?:^|\\s)(?:public\\s+)?interface\\s+\\w");
        boolean hasInterfaceDeclaration = interfacePattern.matcher(trimmedSource).find();
        
        java.util.regex.Pattern enumPattern = java.util.regex.Pattern.compile(
            "(?:^|\\s)(?:public\\s+)?enum\\s+\\w");
        boolean hasEnumDeclaration = enumPattern.matcher(trimmedSource).find();
        
        java.util.regex.Pattern annotationPattern = java.util.regex.Pattern.compile(
            "(?:^|\\s)(?:public\\s+)?@interface\\s+\\w");
        boolean hasAnnotationDeclaration = annotationPattern.matcher(trimmedSource).find();
        
        boolean isFullJavaFile = hasClassDeclaration || hasInterfaceDeclaration || 
                                 hasEnumDeclaration || hasAnnotationDeclaration ||
                                 trimmedSource.startsWith("package ");
        
        if (!isFullJavaFile) {
            String imports = "";
            String body = source;
            
            java.util.regex.Pattern importPattern = java.util.regex.Pattern.compile(
                "(import\\s+(?:static\\s+)?[\\w.]+(?:\\.\\*)?\\s*;\\s*)+");
            java.util.regex.Matcher matcher = importPattern.matcher(source);
            if (matcher.find()) {
                imports = matcher.group();
                body = source.substring(matcher.end());
            }
            
            return imports + "\npublic class Script { public static void main(String[] args) throws Exception { " + body + " } }";
        }
        
        return source;
    }
    
    public void registerVariable(String name, Object value) {
        globalEnv.defineVariable(name, value);
    }
    
    public Object getVariable(String name) {
        return globalEnv.getVariable(name);
    }
    
    public void setVariable(String name, Object value) {
        globalEnv.setVariable(name, value);
    }
    
    public boolean hasVariable(String name) {
        return globalEnv.hasVariable(name);
    }
    
    public void registerFunction(String name, Function<Object[], Object> function) {
        globalEnv.defineVariable(name, function);
    }
    
    public Object invokeFunction(String name, Object... args) {
        Object func = globalEnv.getVariable(name);
        if (func instanceof Function) {
            @SuppressWarnings("unchecked")
            Function<Object[], Object> function = (Function<Object[], Object>) func;
            return function.apply(args);
        }
        throw new RuntimeException("Not a function: " + name);
    }
    
    public ScriptClass registerClass(String name) {
        return registerClass(name, Modifier.PUBLIC);
    }
    
    public ScriptClass registerClass(String name, int modifiers) {
        ScriptClass scriptClass = new ScriptClass(name, name, modifiers, null, new ArrayList<>(), null);
        globalEnv.defineClass(name, scriptClass);
        return scriptClass;
    }
    
    public ScriptClass registerClass(String name, int modifiers, ScriptClass superClass) {
        ScriptClass scriptClass = new ScriptClass(name, name, modifiers, superClass, new ArrayList<>(), null);
        globalEnv.defineClass(name, scriptClass);
        return scriptClass;
    }
    
    public ScriptClass registerClass(String name, int modifiers, ScriptClass superClass, List<ScriptClass> interfaces) {
        ScriptClass scriptClass = new ScriptClass(name, name, modifiers, superClass, interfaces, null);
        globalEnv.defineClass(name, scriptClass);
        return scriptClass;
    }
    
    public ScriptClass getClass(String name) {
        return globalEnv.getClass(name);
    }
    
    public void registerMethod(String className, String methodName, Function<Object[], Object> implementation) {
        registerMethod(className, methodName, Modifier.PUBLIC, implementation);
    }
    
    public void registerMethod(String className, String methodName, int modifiers, Function<Object[], Object> implementation) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) {
            throw new RuntimeException("Class not found: " + className);
        }
        
        NativeMethod method = NativeMethod.createVarArgs(methodName, modifiers, "Object", scriptClass, implementation);
        scriptClass.addMethod(method);
    }
    
    public void registerMethod(String className, String methodName, int modifiers, 
                              String returnType, String[] paramTypes, String[] paramNames,
                              Function<Object[], Object> implementation) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) {
            throw new RuntimeException("Class not found: " + className);
        }
        
        NativeMethod method = NativeMethod.create(methodName, modifiers, returnType, paramTypes, paramNames, scriptClass, implementation);
        scriptClass.addMethod(method);
    }
    
    public void registerStaticMethod(String className, String methodName, Function<Object[], Object> implementation) {
        registerMethod(className, methodName, Modifier.PUBLIC | Modifier.STATIC, implementation);
    }
    
    public void registerStaticMethod(String className, String methodName, int modifiers, Function<Object[], Object> implementation) {
        registerMethod(className, methodName, modifiers | Modifier.STATIC, implementation);
    }
    
    public void registerConstructor(String className, Function<Object[], Object> implementation) {
        registerConstructor(className, Modifier.PUBLIC, implementation);
    }
    
    public void registerConstructor(String className, int modifiers, Function<Object[], Object> implementation) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) {
            throw new RuntimeException("Class not found: " + className);
        }
        
        NativeMethod constructor = new NativeMethod(className, modifiers, 
            new Type(null, className, new ArrayList<>(), 0, new ArrayList<>()), 
            new ArrayList<>(), false, scriptClass, true, implementation);
        scriptClass.addConstructor(constructor);
    }
    
    public void registerField(String className, String fieldName, Object value) {
        registerField(className, fieldName, Modifier.PUBLIC, value);
    }
    
    public void registerField(String className, String fieldName, int modifiers, Object value) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) {
            throw new RuntimeException("Class not found: " + className);
        }
        
        Type fieldType = new Type(null, value != null ? value.getClass().getSimpleName() : "Object", 
                                 new ArrayList<>(), 0, new ArrayList<>());
        ScriptField field = new ScriptField(fieldName, modifiers, fieldType, null, scriptClass);
        scriptClass.addField(field);
        
        if ((modifiers & Modifier.STATIC) != 0) {
            globalEnv.defineVariable(className + "." + fieldName, value);
        }
    }
    
    public void registerStaticField(String className, String fieldName, Object value) {
        registerField(className, fieldName, Modifier.PUBLIC | Modifier.STATIC, value);
    }
    
    public void registerStaticField(String className, String fieldName, int modifiers, Object value) {
        registerField(className, fieldName, modifiers | Modifier.STATIC, value);
    }
    
    public Object newInstance(String className, Object... args) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) {
            throw new RuntimeException("Class not found: " + className);
        }
        
        RuntimeObject instance = new RuntimeObject(scriptClass);
        
        ScriptMethod constructor = scriptClass.getMethod(className, Arrays.asList(args));
        if (constructor != null && constructor.isConstructor()) {
            if (constructor instanceof NativeMethod) {
                ((NativeMethod) constructor).getNativeImplementation().apply(new Object[]{instance});
            }
        }
        
        return instance;
    }
    
    public Object invokeMethod(Object target, String methodName, Object... args) {
        if (target instanceof RuntimeObject) {
            RuntimeObject obj = (RuntimeObject) target;
            ScriptMethod method = obj.getScriptClass().getMethod(methodName, Arrays.asList(args));
            if (method instanceof NativeMethod) {
                Object[] fullArgs = new Object[args.length + 1];
                fullArgs[0] = obj;
                System.arraycopy(args, 0, fullArgs, 1, args.length);
                return ((NativeMethod) method).getNativeImplementation().apply(fullArgs);
            }
        }
        throw new RuntimeException("Cannot invoke method: " + methodName);
    }
    
    public Object invokeStaticMethod(String className, String methodName, Object... args) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) {
            throw new RuntimeException("Class not found: " + className);
        }
        
        ScriptMethod method = scriptClass.getMethod(methodName, Arrays.asList(args));
        if (method instanceof NativeMethod) {
            return ((NativeMethod) method).getNativeImplementation().apply(args);
        }
        throw new RuntimeException("Method not found: " + methodName);
    }
    
    public Environment getGlobalEnvironment() {
        return globalEnv;
    }
    
    public void addAnnotationProcessor(cn.langlang.javainterpreter.annotation.AnnotationProcessor processor) {
        interpreter.addAnnotationProcessor(processor);
    }

    public void registerAnnotationProcessor(cn.langlang.javainterpreter.annotation.AbstractAnnotationProcessor processor) {
        cn.langlang.javainterpreter.annotation.ProcessingEnvironment env =
            new cn.langlang.javainterpreter.annotation.ProcessingEnvironment(interpreter, interpreter.getGlobalEnvironment());
        env.registerProcessor(processor);
    }

    public void enableLombokStyleAnnotations() {
        interpreter.addAnnotationProcessor(new cn.langlang.javainterpreter.annotation.DataAnnotationProcessor());
    }
    
    public static class Modifiers {
        public static final int PUBLIC = Modifier.PUBLIC;
        public static final int PRIVATE = Modifier.PRIVATE;
        public static final int PROTECTED = Modifier.PROTECTED;
        public static final int STATIC = Modifier.STATIC;
        public static final int FINAL = Modifier.FINAL;
        public static final int SYNCHRONIZED = Modifier.SYNCHRONIZED;
        public static final int VOLATILE = Modifier.VOLATILE;
        public static final int TRANSIENT = Modifier.TRANSIENT;
        public static final int NATIVE = Modifier.NATIVE;
        public static final int ABSTRACT = Modifier.ABSTRACT;
        public static final int STRICTFP = Modifier.STRICTFP;
        public static final int DEFAULT = Modifier.DEFAULT;
        
        public static String toString(int modifiers) {
            return Modifier.toString(modifiers);
        }
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: JavaInterpreter <file.java>");
            System.exit(1);
        }
        
        JavaInterpreter interpreter = new JavaInterpreter();
        
        try {
            interpreter.executeFile(args[0]);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
