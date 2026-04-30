package cn.langlang.javanter.interpreter;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.declaration.*;
import cn.langlang.javanter.ast.expression.*;
import cn.langlang.javanter.ast.misc.*;
import cn.langlang.javanter.ast.statement.*;
import cn.langlang.javanter.ast.type.*;
import cn.langlang.javanter.interpreter.exception.*;
import cn.langlang.javanter.interpreter.evaluator.ExpressionEvaluator;
import cn.langlang.javanter.interpreter.executor.DeclarationExecutor;
import cn.langlang.javanter.interpreter.executor.StatementExecutor;
import cn.langlang.javanter.lexer.TokenType;
import cn.langlang.javanter.parser.Modifier;
import cn.langlang.javanter.runtime.environment.Environment;
import cn.langlang.javanter.runtime.model.*;
import cn.langlang.javanter.runtime.nativesupport.NativeMethod;
import cn.langlang.javanter.runtime.nativesupport.StandardLibrary;
import java.util.*;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Interpreter implements ASTVisitor<Object>, ExecutionContext {
    private static final Logger LOGGER = Logger.getLogger(Interpreter.class.getName());
    
    private final Environment globalEnv;
    private final ThreadLocal<Environment> currentEnvHolder;
    private final Map<String, ScriptClass> loadedClasses;
    private final StandardLibrary stdLib;
    private final List<cn.langlang.javanter.annotation.AnnotationProcessor> annotationProcessors;
    private final ThreadLocal<List<InterpreterStackTraceElement>> callStack;
    private String currentFileName;
    
    private final DeclarationExecutor declarationExecutor;
    private final StatementExecutor statementExecutor;
    private final ExpressionEvaluator expressionEvaluator;
    
    public Interpreter() {
        RuntimeObject.setCurrentInterpreter(this);
        this.globalEnv = new Environment();
        this.currentEnvHolder = new ThreadLocal<>();
        this.currentEnvHolder.set(globalEnv);
        this.loadedClasses = new HashMap<>();
        this.stdLib = new StandardLibrary(this);
        this.annotationProcessors = new ArrayList<>();
        this.callStack = new ThreadLocal<>();
        this.currentFileName = null;
        
        this.declarationExecutor = new DeclarationExecutor(this);
        this.statementExecutor = new StatementExecutor(this);
        this.expressionEvaluator = new ExpressionEvaluator(this);
        
        initializeBuiltInClasses();
    }
    
    public DeclarationExecutor getDeclarationExecutor() { return declarationExecutor; }
    public StatementExecutor getStatementExecutor() { return statementExecutor; }
    public ExpressionEvaluator getExpressionEvaluator() { return expressionEvaluator; }
    
    @Override
    public Environment getCurrentEnv() {
        Environment env = currentEnvHolder.get();
        return env != null ? env : globalEnv;
    }
    
    @Override
    public void setCurrentEnv(Environment env) {
        currentEnvHolder.set(env);
    }
    
    @Override
    public Environment getGlobalEnv() {
        return globalEnv;
    }
    
    public void setCurrentFileName(String fileName) {
        this.currentFileName = fileName;
    }
    
    public String getCurrentFileName() {
        return currentFileName;
    }
    
    public void pushCallStack(String className, String methodName, int lineNumber) {
        List<InterpreterStackTraceElement> stack = callStack.get();
        if (stack == null) {
            stack = new ArrayList<>();
            callStack.set(stack);
        }
        stack.add(new InterpreterStackTraceElement(className, methodName, currentFileName, lineNumber));
    }
    
    public void popCallStack() {
        List<InterpreterStackTraceElement> stack = callStack.get();
        if (stack != null && !stack.isEmpty()) {
            stack.remove(stack.size() - 1);
        }
    }
    
    public List<InterpreterStackTraceElement> getCallStack() {
        List<InterpreterStackTraceElement> stack = callStack.get();
        return stack != null ? new ArrayList<>(stack) : new ArrayList<>();
    }
    
    @Override
    public InterpreterException createException(String message) {
        InterpreterException ex = new InterpreterException(message);
        List<InterpreterStackTraceElement> stack = getCallStack();
        for (int i = stack.size() - 1; i >= 0; i--) {
            ex.addStackTraceElement(stack.get(i));
        }
        return ex;
    }
    
    @Override
    public InterpreterException createException(String message, Throwable cause) {
        InterpreterException ex = new InterpreterException(message, cause);
        List<InterpreterStackTraceElement> stack = getCallStack();
        for (int i = stack.size() - 1; i >= 0; i--) {
            ex.addStackTraceElement(stack.get(i));
        }
        return ex;
    }
    
    public void addAnnotationProcessor(cn.langlang.javanter.annotation.AnnotationProcessor processor) {
        annotationProcessors.add(processor);
    }
    
    public List<cn.langlang.javanter.annotation.AnnotationProcessor> getAnnotationProcessors() {
        return annotationProcessors;
    }
    
    public Environment getGlobalEnvironment() {
        return globalEnv;
    }
    
    public Map<String, ScriptClass> getLoadedClasses() {
        return loadedClasses;
    }
    
    public StandardLibrary getStdLib() {
        return stdLib;
    }
    
    private void initializeBuiltInClasses() {
        stdLib.initializeStandardClasses(globalEnv);
    }
    
    public Object interpret(CompilationUnit unit) {
        try {
            return unit.accept(this);
        } catch (RuntimeException e) {
            throw e;
        }
    }
    
    public void interpretDeclarations(CompilationUnit unit) {
        for (ImportDeclaration imp : unit.getImports()) {
            imp.accept(this);
        }

        for (TypeDeclaration type : unit.getTypeDeclarations()) {
            type.accept(this);
        }
    }
    
    public Object invokeStaticMethod(ScriptClass scriptClass, String methodName, List<Object> args) {
        ScriptMethod method = scriptClass.getMethod(methodName, args);
        if (method != null) {
            return invokeMethod(null, method, args);
        }
        throw new RuntimeException("Static method not found: " + methodName);
    }
    
    @Override
    public Object visitCompilationUnit(CompilationUnit node) {
        for (ImportDeclaration imp : node.getImports()) {
            imp.accept(this);
        }
        
        for (TypeDeclaration type : node.getTypeDeclarations()) {
            type.accept(this);
        }
        
        ScriptClass mainClass = null;
        for (TypeDeclaration type : node.getTypeDeclarations()) {
            if (type instanceof ClassDeclaration) {
                ClassDeclaration classDecl = (ClassDeclaration) type;
                ScriptMethod mainMethod = findMainMethod(classDecl.getName());
                if (mainMethod != null) {
                    mainClass = globalEnv.getClass(classDecl.getName());
                    break;
                }
            }
        }
        
        if (mainClass != null) {
            initializeClass(mainClass);
            
            ScriptMethod mainMethod = findMainMethod(mainClass.getName());
            if (mainMethod != null) {
                return invokeMethod(null, mainMethod, Arrays.asList(new Object[0]));
            }
        }
        
        return null;
    }
    
    public void initializeClass(ScriptClass scriptClass) {
        if (scriptClass.isInitialized()) return;
        
        if (scriptClass.getSuperClass() != null) {
            initializeClass(scriptClass.getSuperClass());
        }
        
        for (ScriptField field : scriptClass.getFields().values()) {
            if (field.isStatic()) {
                String fieldKey = scriptClass.getName() + "." + field.getName();
                if (!globalEnv.hasVariable(fieldKey)) {
                    Object value = null;
                    if (field.getInitializer() != null) {
                        Environment previous = getCurrentEnv();
                        setCurrentEnv(getCurrentEnv().push());
                        getCurrentEnv().setCurrentClass(scriptClass);
                        try {
                            value = field.getInitializer().accept(expressionEvaluator);
                        } finally {
                            setCurrentEnv(previous);
                        }
                    } else {
                        value = getDefaultValue(field.getType());
                    }
                    globalEnv.defineVariable(fieldKey, value);
                }
            }
        }
        
        for (InitializerBlock init : scriptClass.getStaticInitializers()) {
            Environment previous = getCurrentEnv();
            setCurrentEnv(getCurrentEnv().push());
            getCurrentEnv().setCurrentClass(scriptClass);
            
            try {
                init.accept(statementExecutor);
            } finally {
                setCurrentEnv(previous);
            }
        }
        
        scriptClass.setInitialized(true);
    }
    
    private Object getDefaultValue(cn.langlang.javanter.ast.type.Type type) {
        if (type == null) return null;
        String typeName = type.getName();
        if (typeName.equals("int")) return 0;
        if (typeName.equals("long")) return 0L;
        if (typeName.equals("double")) return 0.0;
        if (typeName.equals("float")) return 0.0f;
        if (typeName.equals("boolean")) return false;
        if (typeName.equals("char")) return '\0';
        if (typeName.equals("byte")) return (byte) 0;
        if (typeName.equals("short")) return (short) 0;
        return null;
    }
    
    private ScriptMethod findMainMethod(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return null;
        
        List<ScriptMethod> methods = scriptClass.getMethods("main");
        for (ScriptMethod method : methods) {
            if (method.isStatic() && 
                method.getParameters().size() == 1) {
                Type paramType = method.getParameters().get(0).getType();
                String typeName = paramType.getName();
                if ((typeName.equals("String") || typeName.equals("java.lang.String")) &&
                    paramType.getArrayDimensions() == 1) {
                    return method;
                }
            }
        }
        return null;
    }
    
    @Override
    public Object visitPackageDeclaration(PackageDeclaration node) {
        return null;
    }
    
    @Override
    public Object visitImportDeclaration(ImportDeclaration node) {
        String importName = node.getName();
        boolean isStatic = node.isStatic();
        boolean isAsterisk = node.isAsterisk();
        
        if (isStatic && !isAsterisk) {
            int lastDot = importName.lastIndexOf('.');
            if (lastDot > 0) {
                String memberName = importName.substring(lastDot + 1);
                Object staticMember = stdLib.resolveStaticImport(memberName);
                if (staticMember != null) {
                    getCurrentEnv().defineVariable(memberName, staticMember);
                }
            }
        } else if (isStatic && isAsterisk) {
            String[] mathFunctions = {"sqrt", "pow", "abs", "max", "min", "sin", "cos", "tan", 
                                      "log", "exp", "floor", "ceil", "round", "random"};
            for (String func : mathFunctions) {
                Object staticMember = stdLib.resolveStaticImport(func);
                if (staticMember != null) {
                    getCurrentEnv().defineVariable(func, staticMember);
                }
            }
            getCurrentEnv().defineVariable("PI", Math.PI);
            getCurrentEnv().defineVariable("E", Math.E);
        } else if (!isStatic && !isAsterisk) {
            int lastDot = importName.lastIndexOf('.');
            if (lastDot > 0) {
                String simpleName = importName.substring(lastDot + 1);
                try {
                    Class<?> clazz = Class.forName(importName);
                    if (getCurrentEnv().hasVariable(simpleName)) {
                        Object existing = getCurrentEnv().getVariable(simpleName);
                        if (existing != clazz && !(existing instanceof StandardLibrary.SystemHolder) && 
                            !(existing instanceof Map) && !isCompatibleImport(existing, clazz)) {
                            throw new RuntimeException("Import conflict: '" + simpleName + "' is already defined");
                        }
                    }
                    getCurrentEnv().defineVariable(simpleName, clazz);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Cannot find class: '" + importName + "'");
                }
            }
        } else if (!isStatic && isAsterisk) {
            registerPackageClasses(importName);
        }
        
        return null;
    }
    
    private void registerPackageClasses(String packageName) {
        String normalizedName = packageName.endsWith(".*") ? 
            packageName.substring(0, packageName.length() - 2) : packageName;
        
        Set<String> classNames = new HashSet<>();
        
        if (isAndroidRuntime()) {
            classNames.addAll(findClassesOnAndroid(normalizedName));
        } else {
            classNames.addAll(findClassesOnJVM(normalizedName));
        }
        
        for (String className : classNames) {
            try {
                String fullName = normalizedName + "." + className;
                Class<?> clazz = Class.forName(fullName);
                getCurrentEnv().defineVariable(className, clazz);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.FINE, "Class not found during package registration: " + normalizedName + "." + className, e);
            } catch (NoClassDefFoundError e) {
                LOGGER.log(Level.WARNING, "NoClassDefFoundError during package registration: " + normalizedName + "." + className, e);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unexpected error during package registration: " + normalizedName + "." + className, e);
            }
        }
    }
    
    private boolean isAndroidRuntime() {
        try {
            Class.forName("android.os.Build");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private Set<String> findClassesOnAndroid(String packageName) {
        Set<String> classNames = new HashSet<>();
        
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            
            try {
                Class<?> dexFileClass = Class.forName("dalvik.system.DexFile");
                java.lang.reflect.Method getSourceMethod = null;
                
                Class<?> pathClassLoaderClass = Class.forName("dalvik.system.PathClassLoader");
                if (pathClassLoaderClass.isInstance(classLoader)) {
                    getSourceMethod = pathClassLoaderClass.getDeclaredMethod("getPath");
                    getSourceMethod.setAccessible(true);
                    Object path = getSourceMethod.invoke(classLoader);
                    if (path instanceof String) {
                        String[] dexPaths = ((String) path).split(java.io.File.pathSeparator);
                        for (String dexPath : dexPaths) {
                            classNames.addAll(scanDexFile(dexPath, packageName, dexFileClass));
                        }
                    }
                }
                
                Class<?> dexClassLoaderClass = Class.forName("dalvik.system.DexClassLoader");
                if (dexClassLoaderClass.isInstance(classLoader)) {
                    java.lang.reflect.Field pathListField = findField(classLoader.getClass(), "pathList");
                    if (pathListField != null) {
                        pathListField.setAccessible(true);
                        Object pathList = pathListField.get(classLoader);
                        if (pathList != null) {
                            java.lang.reflect.Field dexElementsField = findField(pathList.getClass(), "dexElements");
                            if (dexElementsField != null) {
                                dexElementsField.setAccessible(true);
                                Object[] dexElements = (Object[]) dexElementsField.get(pathList);
                                for (Object element : dexElements) {
                                    java.lang.reflect.Field dexFileField = findField(element.getClass(), "dexFile");
                                    if (dexFileField != null) {
                                        dexFileField.setAccessible(true);
                                        Object dexFile = dexFileField.get(element);
                                        if (dexFile != null) {
                                            classNames.addAll(extractClassesFromDexFile(dexFile, packageName));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Error scanning DexClassLoader on Android for package: " + packageName, e);
            }
            
            String path = packageName.replace('.', '/');
            Enumeration<java.net.URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                java.net.URL resource = resources.nextElement();
                classNames.addAll(findClassesInDirectory(new java.io.File(resource.getFile()), packageName));
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding classes on Android for package: " + packageName, e);
        }
        
        return classNames;
    }
    
    private Set<String> scanDexFile(String dexPath, String packageName, Class<?> dexFileClass) {
        Set<String> classNames = new HashSet<>();
        try {
            java.lang.reflect.Constructor<?> constructor = dexFileClass.getConstructor(String.class);
            Object dexFile = constructor.newInstance(dexPath);
            java.lang.reflect.Method entriesMethod = dexFileClass.getMethod("entries");
            @SuppressWarnings("unchecked")
            Enumeration<String> entries = (Enumeration<String>) entriesMethod.invoke(dexFile);
            
            String prefix = packageName + ".";
            while (entries.hasMoreElements()) {
                String className = entries.nextElement();
                if (className.startsWith(prefix)) {
                    String simpleName = className.substring(prefix.length());
                    if (!simpleName.contains(".") && !simpleName.contains("$")) {
                        if (isValidClassName(simpleName)) {
                            classNames.add(simpleName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error scanning dex file: " + dexPath + " for package: " + packageName, e);
        }
        return classNames;
    }
    
    private Set<String> extractClassesFromDexFile(Object dexFile, String packageName) {
        Set<String> classNames = new HashSet<>();
        try {
            java.lang.reflect.Method entriesMethod = dexFile.getClass().getMethod("entries");
            @SuppressWarnings("unchecked")
            Enumeration<String> entries = (Enumeration<String>) entriesMethod.invoke(dexFile);
            
            String prefix = packageName + ".";
            while (entries.hasMoreElements()) {
                String className = entries.nextElement();
                if (className.startsWith(prefix)) {
                    String simpleName = className.substring(prefix.length());
                    if (!simpleName.contains(".") && !simpleName.contains("$")) {
                        if (isValidClassName(simpleName)) {
                            classNames.add(simpleName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error extracting classes from dex file for package: " + packageName, e);
        }
        return classNames;
    }
    
    private java.lang.reflect.Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    
    private Set<String> findClassesOnJVM(String packageName) {
        Set<String> classNames = new HashSet<>();
        String path = packageName.replace('.', '/');
        
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            
            Enumeration<java.net.URL> resources = classLoader.getResources(path);
            
            while (resources.hasMoreElements()) {
                java.net.URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    classNames.addAll(findClassesInDirectory(new java.io.File(resource.getFile()), packageName));
                } else if (resource.getProtocol().equals("jar")) {
                    classNames.addAll(findClassesInJar(resource, path));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding classes on JVM for package: " + packageName, e);
        }
        
        return classNames;
    }
    
    private Set<String> findClassesInDirectory(java.io.File directory, String packageName) {
        Set<String> classNames = new HashSet<>();
        if (!directory.exists()) {
            return classNames;
        }
        
        try {
            String canonicalPath = directory.getCanonicalPath();
            String absolutePath = directory.getAbsolutePath();
            if (!canonicalPath.equals(absolutePath) && !canonicalPath.equals(new java.io.File(absolutePath).getCanonicalPath())) {
                LOGGER.log(Level.WARNING, "Potential path traversal detected for directory: " + absolutePath);
                return classNames;
            }
        } catch (java.io.IOException e) {
            LOGGER.log(Level.WARNING, "Failed to validate directory path: " + directory.getPath(), e);
            return classNames;
        }
        
        java.io.File[] files = directory.listFiles();
        if (files == null) {
            return classNames;
        }
        
        for (java.io.File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            
            String name = file.getName();
            if (name.endsWith(".class")) {
                String className = name.substring(0, name.length() - 6);
                if (isValidClassName(className)) {
                    classNames.add(className);
                }
            }
        }
        
        return classNames;
    }
    
    private Set<String> findClassesInJar(java.net.URL jarUrl, String packagePath) {
        Set<String> classNames = new HashSet<>();
        
        try {
            String jarPath = jarUrl.getPath();
            int bangIndex = jarPath.indexOf('!');
            if (bangIndex == -1) {
                return classNames;
            }
            
            String filePath = jarPath.substring(0, bangIndex);
            if (filePath.startsWith("file:")) {
                filePath = filePath.substring(5);
            }
            
            String decodedPath = java.net.URLDecoder.decode(filePath, "UTF-8");
            java.io.File jarFile = new java.io.File(decodedPath);
            
            try {
                String canonicalPath = jarFile.getCanonicalPath();
                String absolutePath = jarFile.getAbsolutePath();
                if (!canonicalPath.equals(absolutePath)) {
                    LOGGER.log(Level.WARNING, "Potential path traversal detected for jar file: " + absolutePath);
                    return classNames;
                }
            } catch (java.io.IOException e) {
                LOGGER.log(Level.WARNING, "Failed to validate jar file path: " + decodedPath, e);
                return classNames;
            }
            
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
                Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    
                    if (entryName.contains("..")) {
                        continue;
                    }
                    
                    if (entryName.startsWith(packagePath + "/") && entryName.endsWith(".class")) {
                        String className = entryName.substring(packagePath.length() + 1, entryName.length() - 6);
                        if (!className.contains("/") && !className.contains("$")) {
                            if (isValidClassName(className)) {
                                classNames.add(className);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding classes in jar: " + jarUrl.getPath(), e);
        }
        
        return classNames;
    }
    
    private boolean isValidClassName(String name) {
        if (name.isEmpty()) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isCompatibleImport(Object existing, Class<?> newClass) {
        if (existing instanceof Class) {
            return existing.equals(newClass);
        }
        if (existing instanceof ScriptClass) {
            return ((ScriptClass) existing).getQualifiedName().equals(newClass.getName());
        }
        return false;
    }
    
    @Override
    public Object visitClassDeclaration(ClassDeclaration node) {
        return node.accept(declarationExecutor);
    }
    
    @Override
    public Object visitInterfaceDeclaration(InterfaceDeclaration node) {
        return node.accept(declarationExecutor);
    }
    
    @Override
    public Object visitEnumDeclaration(EnumDeclaration node) {
        return node.accept(declarationExecutor);
    }
    
    @Override
    public Object visitAnnotationDeclaration(AnnotationDeclaration node) {
        return node.accept(declarationExecutor);
    }
    
    @Override
    public Object visitFieldDeclaration(FieldDeclaration node) {
        return node.accept(declarationExecutor);
    }
    
    @Override
    public Object visitMethodDeclaration(MethodDeclaration node) {
        return node.accept(declarationExecutor);
    }
    
    @Override
    public Object visitConstructorDeclaration(ConstructorDeclaration node) {
        return node.accept(declarationExecutor);
    }
    
    @Override
    public Object visitInitializerBlock(InitializerBlock node) {
        return node.accept(declarationExecutor);
    }
    
    @Override
    public Object visitParameterDeclaration(ParameterDeclaration node) {
        return node.accept(declarationExecutor);
    }
    
    @Override
    public Object visitLocalVariableDeclaration(LocalVariableDeclaration node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitBlockStatement(BlockStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitIfStatement(IfStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitWhileStatement(WhileStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitDoStatement(DoStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitForStatement(ForStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitForEachStatement(ForEachStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitSwitchStatement(SwitchStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitCaseLabel(CaseLabel node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitReturnStatement(ReturnStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitThrowStatement(ThrowStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitTryStatement(TryStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitCatchClause(CatchClause node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitSynchronizedStatement(SynchronizedStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitAssertStatement(AssertStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitBreakStatement(BreakStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitContinueStatement(ContinueStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitLabelStatement(LabelStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitExpressionStatement(ExpressionStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitEmptyStatement(EmptyStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitLocalClassDeclarationStatement(LocalClassDeclarationStatement node) {
        return node.accept(statementExecutor);
    }
    
    @Override
    public Object visitLiteralExpression(LiteralExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitIdentifierExpression(IdentifierExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitBinaryExpression(BinaryExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitUnaryExpression(UnaryExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitTernaryExpression(TernaryExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitAssignmentExpression(AssignmentExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitMethodInvocationExpression(MethodInvocationExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitFieldAccessExpression(FieldAccessExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitArrayAccessExpression(ArrayAccessExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitNewObjectExpression(NewObjectExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitNewArrayExpression(NewArrayExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitArrayInitializerExpression(ArrayInitializerExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitCastExpression(CastExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitInstanceOfExpression(InstanceOfExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitThisExpression(ThisExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitSuperExpression(SuperExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitClassLiteralExpression(ClassLiteralExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitLambdaExpression(LambdaExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitMethodReferenceExpression(MethodReferenceExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitParenthesizedExpression(ParenthesizedExpression node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitTypeParameter(TypeParameter node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitTypeArgument(TypeArgument node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitAnnotation(Annotation node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public Object visitEnumConstant(EnumConstant node) {
        return node.accept(expressionEvaluator);
    }
    
    @Override
    public ScriptClass resolveClass(Type type) {
        String name = type.getName();
        
        if (name == null) {
            return null;
        }
        
        if (name.equals("int") || name.equals("long") || name.equals("short") ||
            name.equals("byte") || name.equals("char") || name.equals("boolean") ||
            name.equals("float") || name.equals("double") || name.equals("void")) {
            return null;
        }
        
        ScriptClass scriptClass = globalEnv.getClass(name);
        if (scriptClass == null) {
            scriptClass = stdLib.getStandardClass(name);
        }
        
        if (scriptClass == null && getCurrentEnv().getCurrentClass() != null) {
            String currentClassName = getCurrentEnv().getCurrentClass().getName();
            scriptClass = globalEnv.getClass(currentClassName + "." + name);
        }
        
        return scriptClass;
    }
    
    @Override
    public Object invokeMethod(Object target, ScriptMethod method, List<Object> args) {
        if (method instanceof NativeMethod) {
            NativeMethod nativeMethod = (NativeMethod) method;
            Object[] fullArgs;
            if (method.isStatic()) {
                fullArgs = args.toArray();
            } else {
                fullArgs = new Object[args.size() + 1];
                fullArgs[0] = target;
                for (int i = 0; i < args.size(); i++) {
                    fullArgs[i + 1] = args.get(i);
                }
            }
            return nativeMethod.getNativeImplementation().apply(fullArgs);
        }
        
        if (method.getNativeImplementation() != null) {
            Object[] fullArgs;
            if (method.isStatic()) {
                fullArgs = args.toArray();
            } else {
                fullArgs = new Object[args.size() + 1];
                fullArgs[0] = target;
                for (int i = 0; i < args.size(); i++) {
                    fullArgs[i + 1] = args.get(i);
                }
            }
            return method.getNativeImplementation().apply(fullArgs);
        }
        
        String className = method.getDeclaringClass() != null ? method.getDeclaringClass().getName() : "Unknown";
        int lineNumber = method.getBody() != null ? method.getBody().getLine() : 0;
        pushCallStack(className, method.getName(), lineNumber);
        
        Environment previous = getCurrentEnv();
        setCurrentEnv(new Environment(getCurrentEnv()));
        
        try {
            if (target != null) {
                getCurrentEnv().setThisObject((RuntimeObject) target);
                
                if (((RuntimeObject) target).hasCapturedVariable("this") || ((RuntimeObject) target).getCapturedVariables().size() > 0) {
                    for (Map.Entry<String, Object> entry : ((RuntimeObject) target).getCapturedVariables().entrySet()) {
                        getCurrentEnv().defineVariable(entry.getKey(), entry.getValue());
                    }
                }
            }
            getCurrentEnv().setCurrentClass(method.getDeclaringClass());
            
            List<ParameterDeclaration> params = method.getParameters();
            for (int i = 0; i < args.size(); i++) {
                String paramName;
                if (i < params.size()) {
                    paramName = params.get(i).getName();
                } else {
                    paramName = params.get(params.size() - 1).getName() + "_" + (i - params.size() + 1);
                }
                getCurrentEnv().defineVariable(paramName, args.get(i));
            }
            
            if (method.getBody() != null) {
                method.getBody().accept(statementExecutor);
            }
            
            return null;
        } catch (ReturnException e) {
            return e.getValue();
        } catch (InterpreterException e) {
            throw e;
        } catch (RuntimeException e) {
            throw createException(e.getMessage(), e);
        } finally {
            setCurrentEnv(previous);
            popCallStack();
        }
    }
    
    @Override
    public Object evaluateExpression(Expression expr) {
        return expr.accept(expressionEvaluator);
    }
    
    @Override
    public Object executeStatement(Statement stmt) {
        return stmt.accept(statementExecutor);
    }
    
    public ScriptMethod findConstructor(ScriptClass scriptClass, List<Object> args) {
        ScriptMethod bestMatch = null;
        int bestScore = -1;
        
        for (ScriptMethod constructor : scriptClass.getConstructors()) {
            int score = computeConstructorScore(constructor, args);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = constructor;
            }
        }
        
        return bestMatch;
    }
    
    private int computeConstructorScore(ScriptMethod constructor, List<Object> args) {
        List<ParameterDeclaration> params = constructor.getParameters();
        if (params.size() != args.size() && !constructor.isVarArgs()) {
            return -1;
        }
        
        return args.size();
    }
    
    public void initializeFields(ScriptClass scriptClass, RuntimeObject instance) {
        if (scriptClass.getSuperClass() != null) {
            initializeFields(scriptClass.getSuperClass(), instance);
        }
        
        for (ScriptField field : scriptClass.getFields().values()) {
            if (!field.isStatic()) {
                Object value = null;
                if (field.getInitializer() != null) {
                    value = field.getInitializer().accept(expressionEvaluator);
                }
                instance.setField(field.getName(), value);
            }
        }
    }
    
    public void runInstanceInitializers(ScriptClass scriptClass, RuntimeObject instance) {
        if (scriptClass.getSuperClass() != null) {
            runInstanceInitializers(scriptClass.getSuperClass(), instance);
        }
        
        Environment previous = getCurrentEnv();
        setCurrentEnv(getCurrentEnv().push());
        getCurrentEnv().setThisObject(instance);
        getCurrentEnv().setCurrentClass(scriptClass);
        
        try {
            for (InitializerBlock init : scriptClass.getInstanceInitializers()) {
                init.accept(statementExecutor);
            }
        } finally {
            setCurrentEnv(previous);
        }
    }
    
    public boolean toBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        return value != null;
    }
    
    public int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (Character) value;
        throw new RuntimeException("Cannot convert to int: " + value);
    }
    
    public long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof Character) return (Character) value;
        throw new RuntimeException("Cannot convert to long: " + value);
    }
    
    public float toFloat(Object value) {
        if (value instanceof Number) return ((Number) value).floatValue();
        throw new RuntimeException("Cannot convert to float: " + value);
    }
    
    public double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof Character) return (Character) value;
        throw new RuntimeException("Cannot convert to double: " + value);
    }
    
    public char toChar(Object value) {
        if (value instanceof Character) return (Character) value;
        if (value instanceof Number) return (char) ((Number) value).intValue();
        if (value instanceof String && ((String) value).length() == 1) return ((String) value).charAt(0);
        return '\0';
    }
    
    public byte toByte(Object value) {
        if (value instanceof Byte) return (Byte) value;
        if (value instanceof Number) return ((Number) value).byteValue();
        return (byte) 0;
    }
    
    public short toShort(Object value) {
        if (value instanceof Short) return (Short) value;
        if (value instanceof Number) return ((Number) value).shortValue();
        return (short) 0;
    }
}
