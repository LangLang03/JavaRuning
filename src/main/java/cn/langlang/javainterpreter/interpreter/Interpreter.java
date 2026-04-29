package cn.langlang.javainterpreter.interpreter;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.declaration.*;
import cn.langlang.javainterpreter.ast.expression.*;
import cn.langlang.javainterpreter.ast.misc.*;
import cn.langlang.javainterpreter.ast.statement.*;
import cn.langlang.javainterpreter.ast.type.*;
import cn.langlang.javainterpreter.interpreter.exception.*;
import cn.langlang.javainterpreter.interpreter.evaluator.ExpressionEvaluator;
import cn.langlang.javainterpreter.interpreter.executor.DeclarationExecutor;
import cn.langlang.javainterpreter.interpreter.executor.StatementExecutor;
import cn.langlang.javainterpreter.lexer.TokenType;
import cn.langlang.javainterpreter.parser.Modifier;
import cn.langlang.javainterpreter.runtime.environment.Environment;
import cn.langlang.javainterpreter.runtime.model.*;
import cn.langlang.javainterpreter.runtime.nativesupport.NativeMethod;
import cn.langlang.javainterpreter.runtime.nativesupport.StandardLibrary;
import java.util.*;
import java.util.function.*;

public class Interpreter implements ASTVisitor<Object>, ExecutionContext {
    private final Environment globalEnv;
    private final ThreadLocal<Environment> currentEnvHolder;
    private final Map<String, ScriptClass> loadedClasses;
    private final StandardLibrary stdLib;
    private final List<cn.langlang.javainterpreter.annotation.AnnotationProcessor> annotationProcessors;
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
    
    public void addAnnotationProcessor(cn.langlang.javainterpreter.annotation.AnnotationProcessor processor) {
        annotationProcessors.add(processor);
    }
    
    public List<cn.langlang.javainterpreter.annotation.AnnotationProcessor> getAnnotationProcessors() {
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
    
    private Object getDefaultValue(cn.langlang.javainterpreter.ast.type.Type type) {
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
                    getCurrentEnv().defineVariable(simpleName, clazz);
                } catch (ClassNotFoundException e) {
                }
            }
        } else if (!isStatic && isAsterisk) {
            String[] commonPackages = {
                "java.lang.", "java.util.", "java.io.", "java.net.",
                "java.util.regex.", "java.util.stream.", "java.util.function.",
                "java.text.", "java.lang.reflect.", "java.nio.", "java.nio.file.",
                "java.math.", "java.time.", "java.util.concurrent."
            };
            for (String pkg : commonPackages) {
                if (importName.equals(pkg.substring(0, pkg.length() - 1)) || 
                    importName.equals(pkg.substring(0, pkg.length() - 1).replace(".", ""))) {
                }
            }
        }
        
        return null;
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
