package cn.langlang.javanter.api;

import cn.langlang.javanter.lexer.*;
import cn.langlang.javanter.parser.*;
import cn.langlang.javanter.parser.Modifier;
import cn.langlang.javanter.ast.misc.CompilationUnit;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.interpreter.*;
import cn.langlang.javanter.interpreter.exception.InterpreterException;
import cn.langlang.javanter.analyzer.StaticAnalyzer;
import cn.langlang.javanter.runtime.environment.Environment;
import cn.langlang.javanter.runtime.model.*;
import cn.langlang.javanter.runtime.nativesupport.NativeMethod;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/**
 * Main API class for the JavaInterpreter runtime.
 * This class provides a high-level interface for loading, analyzing, and executing
 * Java source code programmatically.
 *
 * <p>JavaInterpreter supports the following features:</p>
 * <ul>
 *   <li>Loading and executing Java source code from strings or files</li>
 *   <li>Static analysis (linting) of Java code before execution</li>
 *   <li>Script mode - executing Java-like code snippets without full class declarations</li>
 *   <li>Registration of native Java methods and fields for interop</li>
 *   <li>Integration with Java reflection API</li>
 *   <li>Support for Lombok-style annotations via {@code @Data}</li>
 * </ul>
 *
 * <p>Basic usage example:</p>
 * <pre>
 * {@code
 * JavaInterpreter interpreter = new JavaInterpreter();
 * interpreter.enableLombokStyleAnnotations();
 *
 * // Execute a script
 * String script = "int x = 10; int y = 20; x + y";
 * Object result = interpreter.execute(script);
 * System.out.println("Result: " + result);  // Output: Result: 30
 *
 * // Load and run a full Java file
 * interpreter.load("public class Main { public static void main(String[] args) { System.out.println(\"Hello!\"); } }");
 * interpreter.runMain();
 * }</pre>
 *
 * <p>Thread safety: This class is not thread-safe. Each thread should use its own
 * JavaInterpreter instance, or proper synchronization should be applied.</p>
 *
 * @see Interpreter
 * @see StaticAnalyzer
 * @author Javanter Development Team
 */
public class JavaInterpreter {
    private final Interpreter interpreter;
    private final Environment globalEnv;
    private final Map<String, CompilationUnit> loadedUnits;
    private final StaticAnalyzer staticAnalyzer;
    private String mainClassName;

    /**
     * Constructs a new JavaInterpreter with default settings.
     * Initializes the internal interpreter, static analyzer, and global environment.
     * Standard library classes (System, Math, etc.) are automatically available.
     */
    public JavaInterpreter() {
        this.interpreter = new Interpreter();
        this.globalEnv = interpreter.getGlobalEnvironment();
        this.loadedUnits = new HashMap<>();
        this.staticAnalyzer = new StaticAnalyzer();
    }

    /**
     * Performs static analysis (linting) on the given Java source code.
     * This method checks for common errors without executing the code.
     *
     * @param source The Java source code to analyze
     * @return AnalysisResult containing any errors or warnings found
     */
    public StaticAnalyzer.AnalysisResult lint(String source) {
        return lint(source, null);
    }

    /**
     * Performs static analysis (linting) on the given Java source code with a filename.
     * This method checks for common errors without executing the code.
     * The filename is used for better error reporting.
     *
     * @param source The Java source code to analyze
     * @param fileName The name of the source file (for error reporting)
     * @return AnalysisResult containing any errors or warnings found
     */
    public StaticAnalyzer.AnalysisResult lint(String source, String fileName) {
        String processedSource = preprocessSource(source);
        
        if (fileName != null) {
            staticAnalyzer.setFileName(fileName);
        }
        
        Lexer lexer = new Lexer(processedSource);
        List<Token> tokens = lexer.scanTokens();
        
        Parser parser = new Parser(tokens);
        CompilationUnit ast = parser.parseCompilationUnit();
        
        return staticAnalyzer.analyze(ast);
    }

    /**
     * Performs static analysis on a Java source file.
     *
     * @param filePath Path to the Java source file
     * @return AnalysisResult containing any errors or warnings found
     * @throws IOException if the file cannot be read
     */
    public StaticAnalyzer.AnalysisResult lintFile(String filePath) throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(filePath)));
        String fileName = new File(filePath).getName();
        return lint(source, fileName);
    }
    
    public void setMainClassName(String className) {
        this.mainClassName = className;
    }

    /**
     * Loads Java source code into the interpreter without executing it.
     * The source can be a complete Java class or a script snippet (no class declaration needed).
     *
     * @param source The Java source code to load
     */
    public void load(String source) {
        load(source, null);
    }

    /**
     * Loads Java source code into the interpreter without executing it.
     * The source can be a complete Java class or a script snippet (no class declaration needed).
     * If the source doesn't contain a class/interface/enum/annotation declaration, it is
     * automatically wrapped in a public class named "Script" with a main method.
     *
     * @param source The Java source code to load
     * @param fileName Optional filename for error reporting
     */
    public void load(String source, String fileName) {
        String trimmedSource = source.trim();

        String processedSource = preprocessSource(source);
        
        java.util.regex.Pattern classPattern = java.util.regex.Pattern.compile(
            "(?:^|\\s)(?:public\\s+)?(?:abstract\\s+)?(?:final\\s+)?class\\s+(\\w+)");
        java.util.regex.Matcher classMatcher = classPattern.matcher(processedSource);
        if (classMatcher.find()) {
            mainClassName = classMatcher.group(1);
        }
        
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

    /**
     * Loads and executes Java source code, returning the result of running main().
     * This is a convenience method that combines load() and runMain().
     *
     * @param source The Java source code to execute
     * @return The result of the main method execution, or null if no main method
     */
    public Object execute(String source) {
        load(source);
        return runMain();
    }

    /**
     * Loads and executes a Java source file, returning the result of running main().
     *
     * @param filePath Path to the Java source file
     * @return The result of the main method execution, or null if no main method
     * @throws IOException if the file cannot be read
     */
    public Object executeFile(String filePath) throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(filePath)));
        interpreter.setCurrentFileName(new File(filePath).getName());
        return execute(source);
    }

    /**
     * Executes the main method of the previously loaded source code.
     * The main class is determined by either:
     * <ul>
     *   <li>The -main command line option</li>
     *   <li>The first class declaration in the loaded source</li>
     *   <li>Explicitly set via {@link #setMainClassName(String)}</li>
     * </ul>
     *
     * @return The return value of the main method, or null if main returns void
     * @throws RuntimeException if no main class is found
     */
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

    /**
     * Preprocesses the source code to handle script mode.
     * In script mode, code snippets that don't contain a class/interface/enum/annotation
     * declaration are automatically wrapped in a public class "Script" with a main method.
     *
     * <p>This method also extracts import statements from scripts and preserves them
     * when wrapping the script body.</p>
     *
     * @param source The original Java source code
     * @return Preprocessed source with proper class declaration if needed
     */
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
    
    public ScriptClass getClass(String name) {
        return globalEnv.getClass(name);
    }
    
    public ScriptField getField(String className, String fieldName) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return null;
        return scriptClass.getField(fieldName);
    }
    
    public Collection<ScriptField> getFields(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return java.util.Collections.emptyList();
        return scriptClass.getAllFields();
    }
    
    public List<ScriptMethod> getMethods(String className, String methodName) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return new ArrayList<>();
        return scriptClass.getMethods(methodName);
    }
    
    public Map<String, List<ScriptMethod>> getAllMethods(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return new LinkedHashMap<>();
        return scriptClass.getMethods();
    }
    
    public List<ScriptMethod> getConstructors(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return new ArrayList<>();
        return scriptClass.getConstructors();
    }
    
    public List<ScriptClass> getNestedTypes(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return new ArrayList<>();
        return new ArrayList<>(scriptClass.getNestedTypes());
    }
    
    public List<Type> getPermittedSubtypes(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return new ArrayList<>();
        return scriptClass.getPermittedSubtypes();
    }
    
    public int getModifiers(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return 0;
        return scriptClass.getModifiers();
    }
    
    public boolean isSealed(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return false;
        return scriptClass.isSealed();
    }
    
    public boolean isRecord(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return false;
        return scriptClass.isRecord();
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
    
    public Interpreter getInterpreter() {
        return interpreter;
    }
    
    public void addAnnotationProcessor(cn.langlang.javanter.annotation.AnnotationProcessor processor) {
        interpreter.addAnnotationProcessor(processor);
    }

    public void registerAnnotationProcessor(cn.langlang.javanter.annotation.AbstractAnnotationProcessor processor) {
        cn.langlang.javanter.annotation.ProcessingEnvironment env =
            new cn.langlang.javanter.annotation.ProcessingEnvironment(interpreter, interpreter.getGlobalEnvironment());
        env.registerProcessor(processor);
    }

    public void enableLombokStyleAnnotations() {
        interpreter.addAnnotationProcessor(new cn.langlang.javanter.annotation.DataAnnotationProcessor());
    }

    public ScriptResult executeScript(String code) {
        return executeScript(code, Collections.emptyMap());
    }

    public ScriptResult executeScript(String code, Map<String, Object> context) {
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            globalEnv.defineVariable(entry.getKey(), entry.getValue());
        }
        
        try {
            Object result = execute(code);
            
            Map<String, Object> modifiedVars = new HashMap<>();
            for (String name : context.keySet()) {
                modifiedVars.put(name, globalEnv.getVariable(name));
            }
            
            return new ScriptResult(result, modifiedVars, true, null);
        } catch (Exception e) {
            return new ScriptResult(null, context, false, e);
        }
    }

    public ScriptResult executeScript(String code, List<ScriptField> fields, Map<String, Object> values) {
        for (ScriptField field : fields) {
            String name = field.getName();
            Object value = values.get(name);
            globalEnv.defineVariable(name, value);
        }
        
        try {
            Object result = execute(code);
            
            Map<String, Object> modifiedVars = new HashMap<>();
            for (ScriptField field : fields) {
                String name = field.getName();
                if (!field.isFinal()) {
                    modifiedVars.put(name, globalEnv.getVariable(name));
                } else {
                    modifiedVars.put(name, values.get(name));
                }
            }
            
            return new ScriptResult(result, modifiedVars, true, null);
        } catch (Exception e) {
            return new ScriptResult(null, values, false, e);
        }
    }

    public Object executeBlock(String code) {
        return execute(code);
    }

    public Object executeBlock(String code, Map<String, Object> variables) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            registerVariable(entry.getKey(), entry.getValue());
        }
        return execute(code);
    }

    public Object executeBlock(String code, List<ScriptField> fields, Map<String, Object> values) {
        for (ScriptField field : fields) {
            String name = field.getName();
            Object value = values.get(name);
            registerVariable(name, value);
        }
        return execute(code);
    }

    public static ScriptResult run(String code, Map<String, Object> context) {
        JavaInterpreter interpreter = new JavaInterpreter();
        return interpreter.executeScript(code, context);
    }

    public static Object eval(String code) {
        JavaInterpreter interpreter = new JavaInterpreter();
        return interpreter.execute(code);
    }

    /**
     * Helper class providing static constants for Java access modifiers.
     * These constants mirror the {@link Modifier} class and can be used when
     * registering native methods, constructors, and fields.
     *
     * @see Modifier
     */
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
        public static final int ENUM = Modifier.ENUM;
        public static final int SEALED = Modifier.SEALED;
        public static final int NON_SEALED = Modifier.NON_SEALED;
        
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
