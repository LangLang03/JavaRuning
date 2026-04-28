package cn.langlang.javainterpreter.interpreter;

import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.lexer.TokenType;
import cn.langlang.javainterpreter.parser.Modifier;
import cn.langlang.javainterpreter.runtime.*;
import java.util.*;
import java.util.function.*;

public class Interpreter implements ASTVisitor<Object> {
    private final Environment globalEnv;
    private Environment currentEnv;
    private final Map<String, ScriptClass> loadedClasses;
    private final StandardLibrary stdLib;
    private final List<cn.langlang.javainterpreter.annotation.AnnotationProcessor> annotationProcessors;
    
    public Interpreter() {
        this.globalEnv = new Environment();
        this.currentEnv = globalEnv;
        this.loadedClasses = new HashMap<>();
        this.stdLib = new StandardLibrary(this);
        this.annotationProcessors = new ArrayList<>();
        initializeBuiltInClasses();
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
    
    private void initializeClass(ScriptClass scriptClass) {
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
                        Environment previous = currentEnv;
                        currentEnv = currentEnv.push();
                        currentEnv.setCurrentClass(scriptClass);
                        try {
                            value = field.getInitializer().accept(this);
                        } finally {
                            currentEnv = previous;
                        }
                    } else {
                        value = getDefaultValue(field.getType());
                    }
                    globalEnv.defineVariable(fieldKey, value);
                }
            }
        }
        
        for (InitializerBlock init : scriptClass.getStaticInitializers()) {
            Environment previous = currentEnv;
            currentEnv = currentEnv.push();
            currentEnv.setCurrentClass(scriptClass);
            
            try {
                init.accept(this);
            } finally {
                currentEnv = previous;
            }
        }
        
        scriptClass.setInitialized(true);
    }
    
    private Object getDefaultValue(cn.langlang.javainterpreter.ast.Type type) {
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
                    currentEnv.defineVariable(memberName, staticMember);
                }
            }
        } else if (isStatic && isAsterisk) {
            // Handle static wildcard imports like: import static java.lang.Math.*;
            // Common Math functions
            String[] mathFunctions = {"sqrt", "pow", "abs", "max", "min", "sin", "cos", "tan", 
                                      "log", "exp", "floor", "ceil", "round", "random"};
            for (String func : mathFunctions) {
                Object staticMember = stdLib.resolveStaticImport(func);
                if (staticMember != null) {
                    currentEnv.defineVariable(func, staticMember);
                }
            }
            currentEnv.defineVariable("PI", Math.PI);
            currentEnv.defineVariable("E", Math.E);
        }
        
        return null;
    }
    
    @Override
    public Object visitClassDeclaration(ClassDeclaration node) {
        String name = node.getName();
        ScriptClass superClass = null;
        
        if (node.getSuperClass() != null) {
            superClass = resolveClass(node.getSuperClass());
        }
        
        List<ScriptClass> interfaces = new ArrayList<>();
        for (Type iface : node.getInterfaces()) {
            interfaces.add(resolveClass(iface));
        }
        
        ScriptClass scriptClass = new ScriptClass(name, name, node.getModifiers(),
                                                  superClass, interfaces, node);
        
        globalEnv.defineClass(name, scriptClass);
        loadedClasses.put(name, scriptClass);
        
        for (FieldDeclaration field : node.getFields()) {
            ScriptField scriptField = new ScriptField(
                field.getName(), field.getModifiers(), field.getType(),
                field.getInitializer(), scriptClass, field.getAnnotations());
            scriptClass.addField(scriptField);
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            if ((method.getModifiers() & Modifier.NATIVE) != 0) {
                throw new RuntimeException("Native methods are not supported: " + 
                    node.getName() + "." + method.getName() + "()");
            }
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), method.getModifiers(), method.getReturnType(),
                method.getParameters(), method.isVarArgs(), method.getBody(),
                scriptClass, false, method.isDefault(), method.getAnnotations());
            scriptClass.addMethod(scriptMethod);
        }
        
        for (ConstructorDeclaration constructor : node.getConstructors()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                constructor.getName(), constructor.getModifiers(), null,
                constructor.getParameters(), false, constructor.getBody(),
                scriptClass, true, false, constructor.getAnnotations());
            scriptClass.addConstructor(scriptMethod);
        }
        
        for (InitializerBlock init : node.getInitializers()) {
            if (init.isStatic()) {
                scriptClass.addStaticInitializer(init);
            } else {
                scriptClass.addInstanceInitializer(init);
            }
        }
        
        for (TypeDeclaration nested : node.getNestedTypes()) {
            String nestedFullName = name + "." + nested.getName();
            registerNestedType(nested, nestedFullName);
        }
        
        processAnnotations(node, scriptClass);

        return null;
    }

    private void processAnnotations(ClassDeclaration classDecl, ScriptClass scriptClass) {
        cn.langlang.javainterpreter.annotation.ProcessingEnvironment env =
            new cn.langlang.javainterpreter.annotation.ProcessingEnvironment(this, globalEnv);
        
        for (cn.langlang.javainterpreter.annotation.AnnotationProcessor processor : annotationProcessors) {
            if (processor instanceof cn.langlang.javainterpreter.annotation.AbstractAnnotationProcessor) {
                env.registerProcessor((cn.langlang.javainterpreter.annotation.AbstractAnnotationProcessor) processor);
            }
        }
        
        env.invokeProcessorsForClass(classDecl, scriptClass);
    }
    
    private void registerNestedType(TypeDeclaration nested, String fullName) {
        if (nested instanceof ClassDeclaration) {
            ClassDeclaration classDecl = (ClassDeclaration) nested;
            ScriptClass superClass = null;
            if (classDecl.getSuperClass() != null) {
                superClass = resolveClass(classDecl.getSuperClass());
            }
            
            List<ScriptClass> interfaces = new ArrayList<>();
            for (Type iface : classDecl.getInterfaces()) {
                interfaces.add(resolveClass(iface));
            }
            
            ScriptClass scriptClass = new ScriptClass(fullName, fullName, classDecl.getModifiers(),
                                                      superClass, interfaces, classDecl);
            
            globalEnv.defineClass(fullName, scriptClass);
            loadedClasses.put(fullName, scriptClass);
            
            for (FieldDeclaration field : classDecl.getFields()) {
                ScriptField scriptField = new ScriptField(
                    field.getName(), field.getModifiers(), field.getType(),
                    field.getInitializer(), scriptClass, field.getAnnotations());
                scriptClass.addField(scriptField);
            }
            
            for (MethodDeclaration method : classDecl.getMethods()) {
                ScriptMethod scriptMethod = new ScriptMethod(
                    method.getName(), method.getModifiers(), method.getReturnType(),
                    method.getParameters(), method.isVarArgs(), method.getBody(),
                    scriptClass, false, method.isDefault(), method.getAnnotations());
                scriptClass.addMethod(scriptMethod);
            }
            
            for (ConstructorDeclaration constructor : classDecl.getConstructors()) {
                ScriptMethod scriptMethod = new ScriptMethod(
                    constructor.getName(), constructor.getModifiers(), null,
                    constructor.getParameters(), false, constructor.getBody(),
                    scriptClass, true, false, constructor.getAnnotations());
                scriptClass.addConstructor(scriptMethod);
            }
            
            for (TypeDeclaration nestedNested : classDecl.getNestedTypes()) {
                registerNestedType(nestedNested, fullName + "." + nestedNested.getName());
            }
        }
    }
    
    @Override
    public Object visitInterfaceDeclaration(InterfaceDeclaration node) {
        String name = node.getName();
        
        List<ScriptClass> extendsInterfaces = new ArrayList<>();
        for (Type iface : node.getExtendsInterfaces()) {
            extendsInterfaces.add(resolveClass(iface));
        }
        
        ScriptClass scriptClass = new ScriptClass(name, name, node.getModifiers(),
                                                  null, extendsInterfaces, node);
        
        globalEnv.defineClass(name, scriptClass);
        loadedClasses.put(name, scriptClass);
        
        for (MethodDeclaration method : node.getMethods()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), method.getModifiers(), method.getReturnType(),
                method.getParameters(), method.isVarArgs(), method.getBody(),
                scriptClass, false, method.isDefault(), method.getAnnotations());
            scriptClass.addMethod(scriptMethod);
        }
        
        for (FieldDeclaration constant : node.getConstants()) {
            ScriptField scriptField = new ScriptField(
                constant.getName(), constant.getModifiers(), constant.getType(),
                constant.getInitializer(), scriptClass, constant.getAnnotations());
            scriptClass.addField(scriptField);
        }
        
        boolean hasFunctionalInterface = false;
        for (Annotation ann : node.getAnnotations()) {
            if (ann.getTypeName().equals("FunctionalInterface") || 
                ann.getTypeName().endsWith(".FunctionalInterface")) {
                hasFunctionalInterface = true;
                break;
            }
        }
        
        if (hasFunctionalInterface) {
            int abstractMethodCount = 0;
            for (MethodDeclaration method : node.getMethods()) {
                if ((method.getModifiers() & Modifier.DEFAULT) == 0 && 
                    (method.getModifiers() & Modifier.STATIC) == 0) {
                    abstractMethodCount++;
                }
            }
            if (abstractMethodCount != 1) {
                throw new RuntimeException("@FunctionalInterface annotation requires exactly one abstract method, but " + 
                    name + " has " + abstractMethodCount + " abstract methods");
            }
        }
        
        return null;
    }
    
    @Override
    public Object visitEnumDeclaration(EnumDeclaration node) {
        String name = node.getName();
        
        List<ScriptClass> interfaces = new ArrayList<>();
        for (Type iface : node.getInterfaces()) {
            interfaces.add(resolveClass(iface));
        }
        
        ScriptClass scriptClass = new ScriptClass(name, name, node.getModifiers(),
                                                  null, interfaces, node);
        
        globalEnv.defineClass(name, scriptClass);
        loadedClasses.put(name, scriptClass);
        
        for (FieldDeclaration field : node.getFields()) {
            ScriptField scriptField = new ScriptField(
                field.getName(), field.getModifiers(), field.getType(),
                field.getInitializer(), scriptClass, field.getAnnotations());
            scriptClass.addField(scriptField);
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), method.getModifiers(), method.getReturnType(),
                method.getParameters(), method.isVarArgs(), method.getBody(),
                scriptClass, false, method.isDefault(), method.getAnnotations());
            scriptClass.addMethod(scriptMethod);
        }
        
        for (ConstructorDeclaration constructor : node.getConstructors()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                constructor.getName(), constructor.getModifiers(), null,
                constructor.getParameters(), false, constructor.getBody(),
                scriptClass, true, false, constructor.getAnnotations());
            scriptClass.addConstructor(scriptMethod);
        }
        
        int ordinal = 0;
        for (EnumConstant constant : node.getConstants()) {
            ScriptClass constantClass = scriptClass;
            
            if (constant.getAnonymousClass() != null) {
                ClassDeclaration anonClass = constant.getAnonymousClass();
                constantClass = new ScriptClass(name + "$" + constant.getName(), name + "$" + constant.getName(),
                    0, scriptClass, new ArrayList<>(), anonClass);
                
                for (FieldDeclaration field : anonClass.getFields()) {
                    ScriptField scriptField = new ScriptField(
                        field.getName(), field.getModifiers(), field.getType(),
                        field.getInitializer(), constantClass, field.getAnnotations());
                    constantClass.addField(scriptField);
                }
                
                for (MethodDeclaration method : anonClass.getMethods()) {
                    ScriptMethod scriptMethod = new ScriptMethod(
                        method.getName(), method.getModifiers(), method.getReturnType(),
                        method.getParameters(), method.isVarArgs(), method.getBody(),
                        constantClass, false, method.isDefault(), method.getAnnotations());
                    constantClass.addMethod(scriptMethod);
                }
            }
            
            RuntimeObject enumObj = new RuntimeObject(constantClass);
            enumObj.setField("name", constant.getName());
            enumObj.setField("ordinal", ordinal);
            
            for (ScriptField field : scriptClass.getFields().values()) {
                if (!field.isStatic()) {
                    Object value = null;
                    if (field.getInitializer() != null) {
                        value = field.getInitializer().accept(this);
                    }
                    enumObj.setField(field.getName(), value);
                }
            }
            
            if (!constant.getArguments().isEmpty()) {
                List<Object> args = new ArrayList<>();
                for (Expression arg : constant.getArguments()) {
                    args.add(arg.accept(this));
                }
                ScriptMethod constructor = findConstructor(scriptClass, args);
                if (constructor != null) {
                    invokeMethod(enumObj, constructor, args);
                }
            }
            
            globalEnv.defineVariable(constant.getName(), enumObj);
            ordinal++;
        }
        
        return null;
    }
    
    @Override
    public Object visitAnnotationDeclaration(AnnotationDeclaration node) {
        return null;
    }
    
    @Override
    public Object visitFieldDeclaration(FieldDeclaration node) {
        return null;
    }
    
    @Override
    public Object visitMethodDeclaration(MethodDeclaration node) {
        return null;
    }
    
    @Override
    public Object visitConstructorDeclaration(ConstructorDeclaration node) {
        return null;
    }
    
    @Override
    public Object visitInitializerBlock(InitializerBlock node) {
        return node.getBody().accept(this);
    }
    
    @Override
    public Object visitParameterDeclaration(ParameterDeclaration node) {
        return null;
    }
    
    @Override
    public Object visitLocalVariableDeclaration(LocalVariableDeclaration node) {
        for (LocalVariableDeclaration.VariableDeclarator declarator : node.getDeclarators()) {
            Object value = null;
            if (declarator.getInitializer() != null) {
                value = declarator.getInitializer().accept(this);
            }
            currentEnv.defineVariable(declarator.getName(), value);
        }
        return null;
    }
    
    @Override
    public Object visitBlockStatement(BlockStatement node) {
        Environment previous = currentEnv;
        currentEnv = currentEnv.push();
        
        try {
            for (Statement stmt : node.getStatements()) {
                stmt.accept(this);
            }
        } finally {
            currentEnv = previous;
        }
        
        return null;
    }
    
    @Override
    public Object visitIfStatement(IfStatement node) {
        Object condition = node.getCondition().accept(this);
        
        if (toBoolean(condition)) {
            return node.getThenStatement().accept(this);
        } else if (node.getElseStatement() != null) {
            return node.getElseStatement().accept(this);
        }
        
        return null;
    }
    
    @Override
    public Object visitWhileStatement(WhileStatement node) {
        while (toBoolean(node.getCondition().accept(this))) {
            try {
                node.getBody().accept(this);
            } catch (BreakException e) {
                if (e.getLabel() == null) break;
                throw e;
            } catch (ContinueException e) {
                if (e.getLabel() == null) continue;
                throw e;
            }
        }
        return null;
    }
    
    @Override
    public Object visitDoStatement(DoStatement node) {
        do {
            try {
                node.getBody().accept(this);
            } catch (BreakException e) {
                if (e.getLabel() == null) break;
                throw e;
            } catch (ContinueException e) {
                if (e.getLabel() == null) continue;
                throw e;
            }
        } while (toBoolean(node.getCondition().accept(this)));
        return null;
    }
    
    @Override
    public Object visitForStatement(ForStatement node) {
        Environment previous = currentEnv;
        currentEnv = currentEnv.push();
        
        try {
            if (node.getInit() != null) {
                node.getInit().accept(this);
            }
            
            while (node.getCondition() == null || toBoolean(node.getCondition().accept(this))) {
                try {
                    node.getBody().accept(this);
                } catch (BreakException e) {
                    if (e.getLabel() == null) break;
                    throw e;
                } catch (ContinueException e) {
                    if (e.getLabel() == null) {
                        if (node.getUpdate() != null) {
                            node.getUpdate().accept(this);
                        }
                        continue;
                    }
                    throw e;
                }
                
                if (node.getUpdate() != null) {
                    node.getUpdate().accept(this);
                }
            }
        } finally {
            currentEnv = previous;
        }
        
        return null;
    }
    
    @Override
    public Object visitForEachStatement(ForEachStatement node) {
        Object iterable = node.getIterable().accept(this);
        
        if (iterable instanceof Object[]) {
            Object[] array = (Object[]) iterable;
            for (Object element : array) {
                Environment previous = currentEnv;
                currentEnv = currentEnv.push();
                
                try {
                    LocalVariableDeclaration.VariableDeclarator declarator = 
                        node.getVariable().getDeclarators().get(0);
                    currentEnv.defineVariable(declarator.getName(), element);
                    
                    try {
                        node.getBody().accept(this);
                    } catch (BreakException e) {
                        if (e.getLabel() == null) break;
                        throw e;
                    } catch (ContinueException e) {
                        if (e.getLabel() == null) continue;
                        throw e;
                    }
                } finally {
                    currentEnv = previous;
                }
            }
        } else if (iterable instanceof Iterable) {
            for (Object element : (Iterable<?>) iterable) {
                Environment previous = currentEnv;
                currentEnv = currentEnv.push();
                
                try {
                    LocalVariableDeclaration.VariableDeclarator declarator = 
                        node.getVariable().getDeclarators().get(0);
                    currentEnv.defineVariable(declarator.getName(), element);
                    
                    try {
                        node.getBody().accept(this);
                    } catch (BreakException e) {
                        if (e.getLabel() == null) break;
                        throw e;
                    } catch (ContinueException e) {
                        if (e.getLabel() == null) continue;
                        throw e;
                    }
                } finally {
                    currentEnv = previous;
                }
            }
        }
        
        return null;
    }
    
    @Override
    public Object visitSwitchStatement(SwitchStatement node) {
        Object value = node.getExpression().accept(this);
        
        SwitchStatement.SwitchCase defaultCase = null;
        SwitchStatement.SwitchCase matchedCase = null;
        
        for (SwitchStatement.SwitchCase switchCase : node.getCases()) {
            if (switchCase.getLabel().isDefault()) {
                defaultCase = switchCase;
            } else {
                for (Expression caseValue : switchCase.getLabel().getValues()) {
                    Object caseVal = caseValue.accept(this);
                    if (Objects.equals(value, caseVal)) {
                        matchedCase = switchCase;
                        break;
                    }
                }
            }
        }
        
        if (matchedCase == null) {
            matchedCase = defaultCase;
        }
        
        if (matchedCase != null) {
            boolean shouldExecute = false;
            for (SwitchStatement.SwitchCase switchCase : node.getCases()) {
                if (switchCase == matchedCase) {
                    shouldExecute = true;
                }
                
                if (shouldExecute) {
                    try {
                        for (Statement stmt : switchCase.getStatements()) {
                            stmt.accept(this);
                        }
                    } catch (BreakException e) {
                        if (e.getLabel() == null) break;
                        throw e;
                    }
                }
            }
        }
        
        return null;
    }
    
    @Override
    public Object visitCaseLabel(CaseLabel node) {
        return null;
    }
    
    @Override
    public Object visitReturnStatement(ReturnStatement node) {
        Object value = null;
        if (node.getExpression() != null) {
            value = node.getExpression().accept(this);
        }
        throw new ReturnException(value);
    }
    
    @Override
    public Object visitThrowStatement(ThrowStatement node) {
        Object exception = node.getExpression().accept(this);
        if (exception instanceof Throwable) {
            throw new RuntimeException((Throwable) exception);
        }
        throw new RuntimeException("Thrown object is not a Throwable");
    }
    
    @Override
    public Object visitTryStatement(TryStatement node) {
        List<Object> resources = new ArrayList<>();
        List<TryStatement.ResourceDeclaration> resourceDecls = node.getResources();
        Throwable primaryException = null;
        
        try {
            for (TryStatement.ResourceDeclaration resource : resourceDecls) {
                Object res = resource.getExpression().accept(this);
                resources.add(res);
                currentEnv.defineVariable(resource.getName(), res);
            }
            
            node.getTryBlock().accept(this);
        } catch (ReturnException | BreakException | ContinueException e) {
            throw e;
        } catch (RuntimeException e) {
            primaryException = e.getCause() != null ? e.getCause() : e;
            boolean caught = false;
            
            for (CatchClause catchClause : node.getCatchClauses()) {
                if (matchesException(e, catchClause.getExceptionTypes())) {
                    Environment previous = currentEnv;
                    currentEnv = currentEnv.push();
                    
                    try {
                        Throwable actualException = e.getCause() != null ? e.getCause() : e;
                        currentEnv.defineVariable(catchClause.getExceptionName(), actualException);
                        catchClause.getBody().accept(this);
                        caught = true;
                        primaryException = null;
                        break;
                    } finally {
                        currentEnv = previous;
                    }
                }
            }
            
            if (!caught) {
                primaryException = e;
            }
        }
        
        for (int i = resources.size() - 1; i >= 0; i--) {
            Object resource = resources.get(i);
            try {
                if (resource instanceof AutoCloseable) {
                    ((AutoCloseable) resource).close();
                } else if (resource instanceof RuntimeObject) {
                    RuntimeObject runtimeObj = (RuntimeObject) resource;
                    ScriptMethod closeMethod = runtimeObj.getScriptClass().getMethod("close", new ArrayList<>());
                    if (closeMethod != null) {
                        invokeMethod(runtimeObj, closeMethod, new ArrayList<>());
                    }
                }
            } catch (Exception e) {
                if (primaryException != null) {
                    primaryException.addSuppressed(e);
                } else {
                    primaryException = e;
                }
            }
        }
        
        if (node.getFinallyBlock() != null) {
            try {
                node.getFinallyBlock().accept(this);
            } catch (RuntimeException e) {
                if (primaryException != null) {
                    e.getCause().addSuppressed(primaryException);
                }
                throw e;
            }
        }
        
        if (primaryException instanceof RuntimeException) {
            throw (RuntimeException) primaryException;
        } else if (primaryException != null) {
            throw new RuntimeException(primaryException);
        }
        
        return null;
    }
    
    private boolean matchesException(RuntimeException e, List<Type> exceptionTypes) {
        Throwable actualException = e.getCause() != null ? e.getCause() : e;
        
        for (Type type : exceptionTypes) {
            String typeName = type.getName();
            if (typeName.equals("Exception") || typeName.equals("RuntimeException") ||
                typeName.equals("Throwable")) {
                return true;
            }
            
            if (actualException.getClass().getSimpleName().equals(typeName)) {
                return true;
            }
            
            if (actualException.getClass().getName().contains(typeName)) {
                return true;
            }
            
            if (typeName.equals("IOException") && actualException instanceof java.io.IOException) {
                return true;
            }
            
            if (typeName.equals("NullPointerException") && actualException instanceof NullPointerException) {
                return true;
            }
            
            if (typeName.equals("ArithmeticException") && actualException instanceof ArithmeticException) {
                return true;
            }
            
            if (typeName.equals("IllegalArgumentException") && actualException instanceof IllegalArgumentException) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public Object visitCatchClause(CatchClause node) {
        return null;
    }
    
    @Override
    public Object visitSynchronizedStatement(SynchronizedStatement node) {
        Object lock = node.getLock().accept(this);
        synchronized (lock != null ? lock : new Object()) {
            node.getBody().accept(this);
        }
        return null;
    }
    
    @Override
    public Object visitAssertStatement(AssertStatement node) {
        Object condition = node.getCondition().accept(this);
        if (!toBoolean(condition)) {
            String message = "";
            if (node.getMessage() != null) {
                message = String.valueOf(node.getMessage().accept(this));
            }
            throw new AssertionError(message);
        }
        return null;
    }
    
    @Override
    public Object visitBreakStatement(BreakStatement node) {
        throw new BreakException(node.getLabel());
    }
    
    @Override
    public Object visitContinueStatement(ContinueStatement node) {
        throw new ContinueException(node.getLabel());
    }
    
    @Override
    public Object visitLabelStatement(LabelStatement node) {
        try {
            return node.getStatement().accept(this);
        } catch (BreakException e) {
            if (e.getLabel() != null && e.getLabel().equals(node.getLabel())) {
                return null;
            }
            throw e;
        } catch (ContinueException e) {
            if (e.getLabel() != null && e.getLabel().equals(node.getLabel())) {
                throw new ContinueException(null);
            }
            throw e;
        }
    }
    
    @Override
    public Object visitExpressionStatement(ExpressionStatement node) {
        return node.getExpression().accept(this);
    }
    
    @Override
    public Object visitEmptyStatement(EmptyStatement node) {
        return null;
    }
    
    @Override
    public Object visitLocalClassDeclarationStatement(LocalClassDeclarationStatement node) {
        return node.getClassDeclaration().accept(this);
    }
    
    @Override
    public Object visitLiteralExpression(LiteralExpression node) {
        return node.getValue();
    }
    
    @Override
    public Object visitIdentifierExpression(IdentifierExpression node) {
        String name = node.getName();
        
        ScriptClass currentClass = currentEnv.getCurrentClass();
        while (currentClass != null) {
            ScriptField field = currentClass.getField(name);
            if (field != null && field.isStatic()) {
                initializeClass(currentClass);
                String fieldKey = currentClass.getName() + "." + name;
                if (globalEnv.hasVariable(fieldKey)) {
                    return globalEnv.getVariable(fieldKey);
                }
            }
            currentClass = currentClass.getEnclosingClass();
        }
        
        if (currentEnv.hasVariable(name)) {
            return currentEnv.getVariable(name);
        }
        
        if (currentEnv.hasClass(name)) {
            return currentEnv.getClass(name);
        }
        
        RuntimeObject thisObj = currentEnv.getThisObject();
        if (thisObj != null && thisObj.hasField(name)) {
            return thisObj.getField(name);
        }
        
        return stdLib.resolveStaticImport(name);
    }
    
    @Override
    public Object visitBinaryExpression(BinaryExpression node) {
        Object left = node.getLeft().accept(this);
        
        if (node.getOperator() == TokenType.AND) {
            if (!toBoolean(left)) return false;
            return toBoolean(node.getRight().accept(this));
        }
        
        if (node.getOperator() == TokenType.OR) {
            if (toBoolean(left)) return true;
            return toBoolean(node.getRight().accept(this));
        }
        
        Object right = node.getRight().accept(this);
        
        return evaluateBinaryOperator(node.getOperator(), left, right);
    }
    
    private Object evaluateBinaryOperator(TokenType op, Object left, Object right) {
        switch (op) {
            case PLUS:
                if (left instanceof String || right instanceof String) {
                    return String.valueOf(left) + String.valueOf(right);
                }
                if (left instanceof Double || right instanceof Double) {
                    return toDouble(left) + toDouble(right);
                }
                if (left instanceof Float || right instanceof Float) {
                    return toFloat(left) + toFloat(right);
                }
                if (left instanceof Long || right instanceof Long) {
                    return toLong(left) + toLong(right);
                }
                return toInt(left) + toInt(right);
            case MINUS:
                if (left instanceof Double || right instanceof Double) {
                    return toDouble(left) - toDouble(right);
                }
                if (left instanceof Float || right instanceof Float) {
                    return toFloat(left) - toFloat(right);
                }
                if (left instanceof Long || right instanceof Long) {
                    return toLong(left) - toLong(right);
                }
                return toInt(left) - toInt(right);
            case STAR:
                if (left instanceof Double || right instanceof Double) {
                    return toDouble(left) * toDouble(right);
                }
                if (left instanceof Float || right instanceof Float) {
                    return toFloat(left) * toFloat(right);
                }
                if (left instanceof Long || right instanceof Long) {
                    return toLong(left) * toLong(right);
                }
                return toInt(left) * toInt(right);
            case SLASH:
                if (left instanceof Double || right instanceof Double) {
                    return toDouble(left) / toDouble(right);
                }
                if (left instanceof Float || right instanceof Float) {
                    return toFloat(left) / toFloat(right);
                }
                if (left instanceof Long || right instanceof Long) {
                    return toLong(left) / toLong(right);
                }
                return toInt(left) / toInt(right);
            case PERCENT:
                if (left instanceof Double || right instanceof Double) {
                    return toDouble(left) % toDouble(right);
                }
                if (left instanceof Float || right instanceof Float) {
                    return toFloat(left) % toFloat(right);
                }
                if (left instanceof Long || right instanceof Long) {
                    return toLong(left) % toLong(right);
                }
                return toInt(left) % toInt(right);
            case LT:
                return compareNumbers(left, right) < 0;
            case GT:
                return compareNumbers(left, right) > 0;
            case LE:
                return compareNumbers(left, right) <= 0;
            case GE:
                return compareNumbers(left, right) >= 0;
            case EQ:
                return Objects.equals(left, right);
            case NE:
                return !Objects.equals(left, right);
            case AMPERSAND:
                if (left instanceof Integer && right instanceof Integer) {
                    return (Integer) left & (Integer) right;
                }
                if (left instanceof Long || right instanceof Long) {
                    return toLong(left) & toLong(right);
                }
                return toInt(left) & toInt(right);
            case PIPE:
                if (left instanceof Integer && right instanceof Integer) {
                    return (Integer) left | (Integer) right;
                }
                if (left instanceof Long || right instanceof Long) {
                    return toLong(left) | toLong(right);
                }
                return toInt(left) | toInt(right);
            case CARET:
                if (left instanceof Integer && right instanceof Integer) {
                    return (Integer) left ^ (Integer) right;
                }
                if (left instanceof Long || right instanceof Long) {
                    return toLong(left) ^ toLong(right);
                }
                return toInt(left) ^ toInt(right);
            case LSHIFT:
                return toInt(left) << toInt(right);
            case RSHIFT:
                return toInt(left) >> toInt(right);
            case URSHIFT:
                return toInt(left) >>> toInt(right);
            default:
                throw new RuntimeException("Unknown binary operator: " + op);
        }
    }
    
    @Override
    public Object visitUnaryExpression(UnaryExpression node) {
        Object operand = node.getOperand().accept(this);
        
        switch (node.getOperator()) {
            case MINUS:
                if (operand instanceof Double) return -(Double) operand;
                if (operand instanceof Float) return -(Float) operand;
                if (operand instanceof Long) return -(Long) operand;
                return -(Integer) operand;
            case PLUS:
                return operand;
            case NOT:
                return !toBoolean(operand);
            case TILDE:
                if (operand instanceof Long) return ~(Long) operand;
                return ~(Integer) operand;
            case PLUSPLUS:
                if (node.isPrefix()) {
                    Object newValue = increment(operand);
                    assignToTarget(node.getOperand(), newValue);
                    return newValue;
                } else {
                    Object oldValue = operand;
                    assignToTarget(node.getOperand(), increment(operand));
                    return oldValue;
                }
            case MINUSMINUS:
                if (node.isPrefix()) {
                    Object newValue = decrement(operand);
                    assignToTarget(node.getOperand(), newValue);
                    return newValue;
                } else {
                    Object oldValue = operand;
                    assignToTarget(node.getOperand(), decrement(operand));
                    return oldValue;
                }
            default:
                throw new RuntimeException("Unknown unary operator: " + node.getOperator());
        }
    }
    
    private Object increment(Object value) {
        if (value instanceof Integer) return (Integer) value + 1;
        if (value instanceof Long) return (Long) value + 1;
        if (value instanceof Double) return (Double) value + 1;
        if (value instanceof Float) return (Float) value + 1;
        throw new RuntimeException("Cannot increment non-numeric value");
    }
    
    private Object decrement(Object value) {
        if (value instanceof Integer) return (Integer) value - 1;
        if (value instanceof Long) return (Long) value - 1;
        if (value instanceof Double) return (Double) value - 1;
        if (value instanceof Float) return (Float) value - 1;
        throw new RuntimeException("Cannot decrement non-numeric value");
    }
    
    private void assignToTarget(Expression target, Object value) {
        if (target instanceof IdentifierExpression) {
            String name = ((IdentifierExpression) target).getName();
            
            ScriptClass currentClass = currentEnv.getCurrentClass();
            while (currentClass != null) {
                ScriptField field = currentClass.getField(name);
                if (field != null && field.isStatic()) {
                    String fieldKey = currentClass.getName() + "." + name;
                    globalEnv.defineVariable(fieldKey, value);
                    return;
                }
                currentClass = currentClass.getEnclosingClass();
            }
            
            currentEnv.setVariable(name, value);
        } else if (target instanceof FieldAccessExpression) {
            FieldAccessExpression fieldAccess = (FieldAccessExpression) target;
            Object obj = fieldAccess.getTarget().accept(this);
            if (obj instanceof RuntimeObject) {
                ((RuntimeObject) obj).setField(fieldAccess.getFieldName(), value);
            } else if (obj instanceof ScriptClass) {
                ScriptClass scriptClass = (ScriptClass) obj;
                String fieldKey = scriptClass.getName() + "." + fieldAccess.getFieldName();
                globalEnv.defineVariable(fieldKey, value);
            }
        } else if (target instanceof ArrayAccessExpression) {
            ArrayAccessExpression arrayAccess = (ArrayAccessExpression) target;
            Object array = arrayAccess.getArray().accept(this);
            Object index = arrayAccess.getIndex().accept(this);
            if (array instanceof Object[]) {
                ((Object[]) array)[toInt(index)] = value;
            }
        }
    }
    
    @Override
    public Object visitTernaryExpression(TernaryExpression node) {
        Object condition = node.getCondition().accept(this);
        if (toBoolean(condition)) {
            return node.getTrueExpression().accept(this);
        } else {
            return node.getFalseExpression().accept(this);
        }
    }
    
    @Override
    public Object visitAssignmentExpression(AssignmentExpression node) {
        Object value = node.getValue().accept(this);
        
        if (node.getOperator() != TokenType.ASSIGN) {
            Object oldValue = node.getTarget().accept(this);
            value = evaluateCompoundAssignment(node.getOperator(), oldValue, value);
        }
        
        assignToTarget(node.getTarget(), value);
        return value;
    }
    
    private Object evaluateCompoundAssignment(TokenType op, Object oldValue, Object newValue) {
        switch (op) {
            case PLUS_ASSIGN:
                if (oldValue instanceof String || newValue instanceof String) {
                    return String.valueOf(oldValue) + String.valueOf(newValue);
                }
                if (oldValue instanceof Double || newValue instanceof Double) {
                    return toDouble(oldValue) + toDouble(newValue);
                }
                return toInt(oldValue) + toInt(newValue);
            case MINUS_ASSIGN:
                return toInt(oldValue) - toInt(newValue);
            case STAR_ASSIGN:
                return toInt(oldValue) * toInt(newValue);
            case SLASH_ASSIGN:
                return toInt(oldValue) / toInt(newValue);
            case PERCENT_ASSIGN:
                return toInt(oldValue) % toInt(newValue);
            case AND_ASSIGN:
                return toInt(oldValue) & toInt(newValue);
            case OR_ASSIGN:
                return toInt(oldValue) | toInt(newValue);
            case XOR_ASSIGN:
                return toInt(oldValue) ^ toInt(newValue);
            case LSHIFT_ASSIGN:
                return toInt(oldValue) << toInt(newValue);
            case RSHIFT_ASSIGN:
                return toInt(oldValue) >> toInt(newValue);
            case URSHIFT_ASSIGN:
                return toInt(oldValue) >>> toInt(newValue);
            default:
                throw new RuntimeException("Unknown compound assignment operator: " + op);
        }
    }
    
    @Override
    public Object visitMethodInvocationExpression(MethodInvocationExpression node) {
        Object target = null;
        if (node.getTarget() != null) {
            target = node.getTarget().accept(this);
        }
        
        List<Object> args = new ArrayList<>();
        for (Expression arg : node.getArguments()) {
            args.add(arg.accept(this));
        }
        
        if (target == null) {
            Object var = currentEnv.getVariable(node.getMethodName());
            if (var instanceof ScriptClass) {
                target = var;
            } else if (var instanceof StandardLibrary.StaticMethodHolder) {
                return ((StandardLibrary.StaticMethodHolder) var).invoke(args);
            }
        }

        if (target == null && node.getTarget() == null) {
            RuntimeObject thisObj = currentEnv.getThisObject();
            if (thisObj != null) {
                target = thisObj;
            }
        }
        
        if (target == null && node.getTarget() instanceof IdentifierExpression) {
            String varName = ((IdentifierExpression) node.getTarget()).getName();
            target = currentEnv.getVariable(varName);
            
            if (target == null) {
                ScriptClass scriptClass = globalEnv.getClass(varName);
                if (scriptClass != null) {
                    target = scriptClass;
                }
            }
        }
        
        if (target instanceof Class) {
            return stdLib.invokeMethod(target, node.getMethodName(), args);
        }
        
        if (target instanceof java.lang.reflect.Method) {
            return stdLib.invokeMethod(target, node.getMethodName(), args);
        }
        
        if (target instanceof java.lang.reflect.Field) {
            return stdLib.invokeMethod(target, node.getMethodName(), args);
        }
        
        if (target instanceof java.lang.reflect.Constructor) {
            return stdLib.invokeMethod(target, node.getMethodName(), args);
        }
        
        if (target instanceof ScriptMethod) {
            return stdLib.invokeMethod(target, node.getMethodName(), args);
        }
        
        if (target instanceof ScriptField) {
            return stdLib.invokeMethod(target, node.getMethodName(), args);
        }
        
        if (target instanceof ScriptClass) {
            ScriptClass scriptClass = (ScriptClass) target;
            initializeClass(scriptClass);
            ScriptMethod method = scriptClass.getMethod(node.getMethodName(), args);
            if (method != null && method.isStatic()) {
                return invokeMethod(null, method, args);
            }
        }
        
        if (target instanceof RuntimeObject) {
            RuntimeObject obj = (RuntimeObject) target;
            
            if (node.getMethodName().equals("getClass") && args.isEmpty()) {
                return obj.getScriptClass();
            }
            
            ScriptMethod method = obj.getScriptClass().getMethod(node.getMethodName(), args);
            if (method != null) {
                return invokeMethod(obj, method, args);
            }
        }
        
        if (target instanceof InterfaceSuperObject) {
            InterfaceSuperObject iso = (InterfaceSuperObject) target;
            ScriptMethod method = iso.getInterfaceClass().getMethod(node.getMethodName(), args);
            if (method != null && method.isDefault()) {
                return invokeMethod(iso.getThisObject(), method, args);
            }
        }
        
        if (target instanceof SuperObject) {
            SuperObject superObj = (SuperObject) target;
            ScriptMethod method = superObj.getSuperClass().getMethod(node.getMethodName(), args);
            if (method != null) {
                return invokeMethod(superObj.getTarget(), method, args);
            }
        }
        
        if (target instanceof LambdaObject) {
            return invokeLambda((LambdaObject) target, args);
        }
        
        if (target instanceof MethodReferenceObject) {
            return invokeMethodReference((MethodReferenceObject) target, args);
        }
        
        if (target instanceof StandardLibrary.StaticMethodHolder) {
            return stdLib.invokeMethod(target, node.getMethodName(), args);
        }
        
        return stdLib.invokeMethod(target, node.getMethodName(), args);
    }
    
    private Object invokeLambda(LambdaObject lambda, List<Object> args) {
        LambdaExpression lambdaExpr = lambda.getLambda();
        Environment closureEnv = lambda.getClosureEnv();
        ScriptClass closureClass = lambda.getClosureClass();
        Environment previous = currentEnv;
        currentEnv = new Environment(closureEnv != null ? closureEnv : globalEnv);
        
        if (closureClass != null) {
            currentEnv.setCurrentClass(closureClass);
        }
        
        try {
            List<LambdaExpression.LambdaParameter> params = lambdaExpr.getParameters();
            for (int i = 0; i < args.size() && i < params.size(); i++) {
                LambdaExpression.LambdaParameter param = params.get(i);
                currentEnv.defineVariable(param.getName(), args.get(i));
            }
            
            ASTNode body = lambdaExpr.getBody();
            if (body instanceof Expression) {
                return ((Expression) body).accept(this);
            } else if (body instanceof BlockStatement) {
                try {
                    ((BlockStatement) body).accept(this);
                    return null;
                } catch (ReturnException e) {
                    return e.getValue();
                }
            }
        } finally {
            currentEnv = previous;
        }
        
        return null;
    }
    
    private Object invokeMethodReference(MethodReferenceObject methodRefObj, List<Object> args) {
        MethodReferenceExpression methodRef = methodRefObj.getMethodRef();
        Expression targetExpr = methodRef.getTarget();
        String methodName = methodRef.getMethodName();
        
        if (methodName.equals("new")) {
            if (targetExpr instanceof ClassLiteralExpression) {
                Type type = ((ClassLiteralExpression) targetExpr).getType();
                if (type.getArrayDimensions() > 0 || type.getName().equals("int") || 
                    type.getName().equals("long") || type.getName().equals("double")) {
                    int size = args.isEmpty() ? 0 : toInt(args.get(0));
                    return createArray(type, size);
                }
            }
            ScriptClass scriptClass = resolveClassFromExpression(targetExpr);
            if (scriptClass != null) {
                ScriptMethod constructor = findConstructor(scriptClass, args);
                if (constructor != null) {
                    RuntimeObject instance = new RuntimeObject(scriptClass);
                    initializeFields(scriptClass, instance);
                    runInstanceInitializers(scriptClass, instance);
                    invokeMethod(instance, constructor, args);
                    return instance;
                }
            }
        }
        
        if (targetExpr != null) {
            Object targetObj = targetExpr.accept(this);
            if (targetObj instanceof ScriptClass) {
                ScriptClass scriptClass = (ScriptClass) targetObj;
                ScriptMethod method = scriptClass.getMethod(methodName, args);
                if (method != null && method.isStatic()) {
                    return invokeMethod(null, method, args);
                }
            }
            if (targetObj instanceof RuntimeObject) {
                RuntimeObject runtimeObj = (RuntimeObject) targetObj;
                ScriptMethod method = runtimeObj.getScriptClass().getMethod(methodName, args);
                if (method != null) {
                    return invokeMethod(runtimeObj, method, args);
                }
            }
            return stdLib.invokeMethod(targetObj, methodName, args);
        }
        
        return null;
    }
    
    private ScriptClass resolveClassFromExpression(Expression expr) {
        if (expr instanceof ClassLiteralExpression) {
            return resolveClass(((ClassLiteralExpression) expr).getType());
        }
        if (expr instanceof IdentifierExpression) {
            return globalEnv.getClass(((IdentifierExpression) expr).getName());
        }
        return null;
    }
    
    private Object createArray(Type type, int size) {
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
    
    public Object invokeMethod(RuntimeObject target, ScriptMethod method, List<Object> args) {
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
        
        Environment previous = currentEnv;
        currentEnv = new Environment(globalEnv);
        
        try {
            if (target != null) {
                currentEnv.setThisObject(target);
            }
            currentEnv.setCurrentClass(method.getDeclaringClass());
            
            List<ParameterDeclaration> params = method.getParameters();
            for (int i = 0; i < args.size(); i++) {
                String paramName;
                if (i < params.size()) {
                    paramName = params.get(i).getName();
                } else {
                    paramName = params.get(params.size() - 1).getName() + "_" + (i - params.size() + 1);
                }
                currentEnv.defineVariable(paramName, args.get(i));
            }
            
            if (method.getBody() != null) {
                method.getBody().accept(this);
            }
            
            return null;
        } catch (ReturnException e) {
            return e.getValue();
        } finally {
            currentEnv = previous;
        }
    }
    
    @Override
    public Object visitFieldAccessExpression(FieldAccessExpression node) {
        Object target = node.getTarget().accept(this);
        
        if (target instanceof ScriptClass) {
            ScriptClass scriptClass = (ScriptClass) target;
            ScriptField field = scriptClass.getField(node.getFieldName());
            if (field != null && field.isStatic()) {
                String varName = scriptClass.getName() + "." + node.getFieldName();
                if (currentEnv.hasVariable(varName)) {
                    return currentEnv.getVariable(varName);
                }
                if (globalEnv.hasVariable(varName)) {
                    return globalEnv.getVariable(varName);
                }
                return null;
            }
            
            if (currentEnv.hasVariable(node.getFieldName())) {
                return currentEnv.getVariable(node.getFieldName());
            }
        }
        
        if (target instanceof RuntimeObject) {
            RuntimeObject obj = (RuntimeObject) target;
            return obj.getField(node.getFieldName());
        }
        
        if (target instanceof Object[]) {
            if (node.getFieldName().equals("length")) {
                return ((Object[]) target).length;
            }
        }
        
        if (target instanceof StandardLibrary.SystemHolder) {
            return ((StandardLibrary.SystemHolder) target).getField(node.getFieldName());
        }
        
        return stdLib.getField(target, node.getFieldName());
    }
    
    @Override
    public Object visitArrayAccessExpression(ArrayAccessExpression node) {
        Object array = node.getArray().accept(this);
        Object index = node.getIndex().accept(this);
        
        if (array instanceof Object[]) {
            return ((Object[]) array)[toInt(index)];
        } else if (array instanceof int[]) {
            return ((int[]) array)[toInt(index)];
        } else if (array instanceof long[]) {
            return ((long[]) array)[toInt(index)];
        } else if (array instanceof double[]) {
            return ((double[]) array)[toInt(index)];
        } else if (array instanceof float[]) {
            return ((float[]) array)[toInt(index)];
        } else if (array instanceof boolean[]) {
            return ((boolean[]) array)[toInt(index)];
        } else if (array instanceof char[]) {
            return ((char[]) array)[toInt(index)];
        } else if (array instanceof byte[]) {
            return ((byte[]) array)[toInt(index)];
        } else if (array instanceof short[]) {
            return ((short[]) array)[toInt(index)];
        }
        
        throw new RuntimeException("Cannot access array element on non-array type");
    }
    
    @Override
    public Object visitNewObjectExpression(NewObjectExpression node) {
        List<ASTNode> anonymousClassBody = node.getAnonymousClassBody();
        
        if (anonymousClassBody != null && !anonymousClassBody.isEmpty()) {
            return createAnonymousClassInstance(node);
        }
        
        ScriptClass scriptClass = resolveClass(node.getType());
        
        if (scriptClass == null) {
            return stdLib.createObject(node.getType().getName(), 
                convertArgs(node.getArguments()));
        }
        
        RuntimeObject instance = new RuntimeObject(scriptClass);
        
        initializeFields(scriptClass, instance);
        
        runInstanceInitializers(scriptClass, instance);
        
        List<Object> args = new ArrayList<>();
        for (Expression arg : node.getArguments()) {
            args.add(arg.accept(this));
        }
        
        ScriptMethod constructor = findConstructor(scriptClass, args);
        if (constructor != null) {
            invokeMethod(instance, constructor, args);
        }
        
        return instance;
    }
    
    private Object createAnonymousClassInstance(NewObjectExpression node) {
        Type baseType = node.getType();
        List<ASTNode> anonymousClassBody = node.getAnonymousClassBody();
        
        ScriptClass baseClass = resolveClass(baseType);
        ScriptClass superClass = null;
        List<ScriptClass> interfaces = new ArrayList<>();
        
        if (baseClass != null) {
            if (baseClass.getAstNode() instanceof InterfaceDeclaration) {
                interfaces.add(baseClass);
            } else {
                superClass = baseClass;
            }
        } else {
            String typeName = baseType.getName();
            if (typeName.equals("Runnable") || typeName.equals("java.lang.Runnable")) {
                ScriptClass runnableInterface = createRunnableInterface();
                interfaces.add(runnableInterface);
            } else if (typeName.equals("Thread") || typeName.equals("java.lang.Thread")) {
                superClass = createClassFromJavaClass(Thread.class);
            } else {
                try {
                    String fullClassName = typeName;
                    if (!typeName.contains(".")) {
                        fullClassName = "java.lang." + typeName;
                    }
                    Class<?> clazz = Class.forName(fullClassName);
                    if (clazz.isInterface()) {
                        ScriptClass iface = createInterfaceFromClass(clazz);
                        interfaces.add(iface);
                    } else {
                        superClass = createClassFromJavaClass(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Cannot resolve type for anonymous class: " + typeName);
                }
            }
        }
        
        String anonymousClassName = "AnonymousClass_" + System.nanoTime();
        ScriptClass anonymousClass = new ScriptClass(
            anonymousClassName, anonymousClassName, 0, 
            superClass, interfaces, null
        );
        
        ScriptClass enclosingClass = currentEnv.getCurrentClass();
        if (enclosingClass != null) {
            anonymousClass.setEnclosingClass(enclosingClass);
        }
        
        for (ASTNode member : anonymousClassBody) {
            if (member instanceof FieldDeclaration) {
                FieldDeclaration field = (FieldDeclaration) member;
                ScriptField scriptField = new ScriptField(
                    field.getName(), field.getModifiers(), field.getType(),
                    field.getInitializer(), anonymousClass, field.getAnnotations()
                );
                anonymousClass.addField(scriptField);
            } else if (member instanceof MethodDeclaration) {
                MethodDeclaration method = (MethodDeclaration) member;
                ScriptMethod scriptMethod = new ScriptMethod(
                    method.getName(), method.getModifiers(), method.getReturnType(),
                    method.getParameters(), method.isVarArgs(), method.getBody(),
                    anonymousClass, false, method.isDefault(), method.getAnnotations()
                );
                anonymousClass.addMethod(scriptMethod);
            } else if (member instanceof ConstructorDeclaration) {
                ConstructorDeclaration constructor = (ConstructorDeclaration) member;
                ScriptMethod scriptMethod = new ScriptMethod(
                    constructor.getName(), constructor.getModifiers(), null,
                    constructor.getParameters(), false, constructor.getBody(),
                    anonymousClass, true, false, constructor.getAnnotations()
                );
                anonymousClass.addConstructor(scriptMethod);
            } else if (member instanceof InitializerBlock) {
                InitializerBlock init = (InitializerBlock) member;
                if (init.isStatic()) {
                    anonymousClass.addStaticInitializer(init);
                } else {
                    anonymousClass.addInstanceInitializer(init);
                }
            }
        }
        
        globalEnv.defineClass(anonymousClassName, anonymousClass);
        loadedClasses.put(anonymousClassName, anonymousClass);
        
        RuntimeObject instance = new RuntimeObject(anonymousClass);
        
        initializeFields(anonymousClass, instance);
        
        runInstanceInitializers(anonymousClass, instance);
        
        List<Object> args = new ArrayList<>();
        for (Expression arg : node.getArguments()) {
            args.add(arg.accept(this));
        }
        
        ScriptMethod constructor = findConstructor(anonymousClass, args);
        if (constructor != null) {
            invokeMethod(instance, constructor, args);
        } else if (superClass != null) {
            constructor = findConstructor(superClass, args);
            if (constructor != null) {
                invokeMethod(instance, constructor, args);
            }
        }
        
        return instance;
    }
    
    private ScriptClass createRunnableInterface() {
        ScriptClass runnableInterface = new ScriptClass(
            "Runnable", "java.lang.Runnable", Modifier.ABSTRACT | Modifier.INTERFACE,
            null, new ArrayList<>(), null
        );
        
        List<ParameterDeclaration> params = new ArrayList<>();
        Type voidType = new Type(null, "void", new ArrayList<>(), 0, new ArrayList<>());
        ScriptMethod runMethod = new ScriptMethod(
            "run", Modifier.PUBLIC | Modifier.ABSTRACT, voidType,
            params, false, null, runnableInterface, false, false, new ArrayList<>()
        );
        runnableInterface.addMethod(runMethod);
        
        return runnableInterface;
    }
    
    private ScriptClass createInterfaceFromClass(Class<?> clazz) {
        ScriptClass iface = new ScriptClass(
            clazz.getSimpleName(), clazz.getName(), Modifier.INTERFACE,
            null, new ArrayList<>(), null
        );
        
        for (java.lang.reflect.Method method : clazz.getMethods()) {
            if (method.isDefault() || method.isSynthetic()) continue;
            
            List<ParameterDeclaration> params = new ArrayList<>();
            java.lang.reflect.Parameter[] reflectParams = method.getParameters();
            for (java.lang.reflect.Parameter param : reflectParams) {
                cn.langlang.javainterpreter.ast.Type paramType = createTypeFromJavaType(param.getType());
                params.add(new ParameterDeclaration(null, 0, paramType, param.getName(), 
                    false, new ArrayList<>()));
            }
            
            cn.langlang.javainterpreter.ast.Type returnType = createTypeFromJavaType(method.getReturnType());
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), Modifier.PUBLIC | Modifier.ABSTRACT, returnType,
                params, method.isVarArgs(), null, iface, false, false, new ArrayList<>()
            );
            iface.addMethod(scriptMethod);
        }
        
        return iface;
    }
    
    private ScriptClass createClassFromJavaClass(Class<?> clazz) {
        ScriptClass scriptClass = new ScriptClass(
            clazz.getSimpleName(), clazz.getName(), 0,
            null, new ArrayList<>(), null
        );
        return scriptClass;
    }
    
    private cn.langlang.javainterpreter.ast.Type createTypeFromJavaType(Class<?> type) {
        String typeName;
        if (type.isPrimitive()) {
            typeName = type.getName();
        } else if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            cn.langlang.javainterpreter.ast.Type component = createTypeFromJavaType(componentType);
            return new cn.langlang.javainterpreter.ast.Type(null, component.getName(), 
                component.getTypeArguments(), component.getArrayDimensions() + 1, new ArrayList<>());
        } else {
            typeName = type.getSimpleName();
        }
        return new cn.langlang.javainterpreter.ast.Type(null, typeName, new ArrayList<>(), 0, new ArrayList<>());
    }
    
    private void initializeFields(ScriptClass scriptClass, RuntimeObject instance) {
        if (scriptClass.getSuperClass() != null) {
            initializeFields(scriptClass.getSuperClass(), instance);
        }
        
        for (ScriptField field : scriptClass.getFields().values()) {
            if (!field.isStatic()) {
                Object value = null;
                if (field.getInitializer() != null) {
                    value = field.getInitializer().accept(this);
                }
                instance.setField(field.getName(), value);
            }
        }
    }
    
    private void runInstanceInitializers(ScriptClass scriptClass, RuntimeObject instance) {
        if (scriptClass.getSuperClass() != null) {
            runInstanceInitializers(scriptClass.getSuperClass(), instance);
        }
        
        Environment previous = currentEnv;
        currentEnv = currentEnv.push();
        currentEnv.setThisObject(instance);
        currentEnv.setCurrentClass(scriptClass);
        
        try {
            for (InitializerBlock init : scriptClass.getInstanceInitializers()) {
                init.accept(this);
            }
        } finally {
            currentEnv = previous;
        }
    }
    
    private ScriptMethod findConstructor(ScriptClass scriptClass, List<Object> args) {
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
    
    @Override
    public Object visitNewArrayExpression(NewArrayExpression node) {
        Type elementType = node.getElementType();
        List<Expression> dimensions = node.getDimensions();
        
        if (dimensions.isEmpty()) {
            return createArrayFromInitializer(elementType, node.getInitializer());
        }
        
        int[] dims = new int[dimensions.size()];
        for (int i = 0; i < dimensions.size(); i++) {
            dims[i] = toInt(dimensions.get(i).accept(this));
        }
        
        return createArray(elementType, dims, 0);
    }
    
    private Object createArrayFromInitializer(Type elementType, ArrayInitializerExpression initializer) {
        if (initializer == null) return null;
        
        List<Expression> elements = initializer.getElements();
        Object[] array = new Object[elements.size()];
        
        for (int i = 0; i < elements.size(); i++) {
            Expression elem = elements.get(i);
            if (elem instanceof ArrayInitializerExpression) {
                array[i] = createArrayFromInitializer(elementType, (ArrayInitializerExpression) elem);
            } else {
                array[i] = elem.accept(this);
            }
        }
        
        return array;
    }
    
    private Object createArray(Type elementType, int[] dimensions, int depth) {
        int size = dimensions[depth];
        
        if (depth == dimensions.length - 1) {
            String typeName = elementType.getName();
            switch (typeName) {
                case "int": return new int[size];
                case "long": return new long[size];
                case "short": return new short[size];
                case "byte": return new byte[size];
                case "char": return new char[size];
                case "boolean": return new boolean[size];
                case "float": return new float[size];
                case "double": return new double[size];
                default: return new Object[size];
            }
        }
        
        Object[] array = new Object[size];
        for (int i = 0; i < size; i++) {
            array[i] = createArray(elementType, dimensions, depth + 1);
        }
        return array;
    }
    
    @Override
    public Object visitArrayInitializerExpression(ArrayInitializerExpression node) {
        Object[] array = new Object[node.getElements().size()];
        for (int i = 0; i < node.getElements().size(); i++) {
            array[i] = node.getElements().get(i).accept(this);
        }
        return array;
    }
    
    @Override
    public Object visitCastExpression(CastExpression node) {
        Object value = node.getExpression().accept(this);
        Type targetType = node.getType();
        
        return castValue(value, targetType);
    }
    
    private Object castValue(Object value, Type targetType) {
        if (value == null) return null;
        
        String typeName = targetType.getName();
        
        switch (typeName) {
            case "int":
                if (value instanceof Number) return ((Number) value).intValue();
                if (value instanceof Character) return (int) (Character) value;
                break;
            case "long":
                if (value instanceof Number) return ((Number) value).longValue();
                if (value instanceof Character) return (long) (Character) value;
                break;
            case "short":
                if (value instanceof Number) return ((Number) value).shortValue();
                if (value instanceof Character) return (short) ((Character) value).charValue();
                break;
            case "byte":
                if (value instanceof Number) return ((Number) value).byteValue();
                if (value instanceof Character) return (byte) ((Character) value).charValue();
                break;
            case "char":
                if (value instanceof Number) return (char) ((Number) value).intValue();
                if (value instanceof Character) return value;
                break;
            case "float":
                if (value instanceof Number) return ((Number) value).floatValue();
                break;
            case "double":
                if (value instanceof Number) return ((Number) value).doubleValue();
                break;
            case "boolean":
                if (value instanceof Boolean) return value;
                break;
        }
        
        return value;
    }
    
    @Override
    public Object visitInstanceOfExpression(InstanceOfExpression node) {
        Object value = node.getExpression().accept(this);
        
        if (value == null) return false;
        
        Type checkType = node.getType();
        ScriptClass checkClass = resolveClass(checkType);
        
        if (checkClass == null) {
            String typeName = checkType.getName();
            switch (typeName) {
                case "int": return value instanceof Integer;
                case "long": return value instanceof Long;
                case "short": return value instanceof Short;
                case "byte": return value instanceof Byte;
                case "char": return value instanceof Character;
                case "boolean": return value instanceof Boolean;
                case "float": return value instanceof Float;
                case "double": return value instanceof Double;
                default: return false;
            }
        }
        
        if (value instanceof RuntimeObject) {
            ScriptClass valueClass = ((RuntimeObject) value).getScriptClass();
            return checkClass.isAssignableFrom(valueClass);
        }
        
        return false;
    }
    
    @Override
    public Object visitThisExpression(ThisExpression node) {
        return currentEnv.getThisObject();
    }
    
    @Override
    public Object visitSuperExpression(SuperExpression node) {
        String interfaceName = node.getClassName();
        
        if (interfaceName != null) {
            ScriptClass interfaceClass = globalEnv.getClass(interfaceName);
            if (interfaceClass != null) {
                return new InterfaceSuperObject(currentEnv.getThisObject(), interfaceClass);
            }
        }
        
        RuntimeObject thisObj = currentEnv.getThisObject();
        if (thisObj != null && thisObj.getScriptClass().getSuperClass() != null) {
            return new SuperObject(thisObj, thisObj.getScriptClass().getSuperClass());
        }
        return thisObj;
    }
    
    @Override
    public Object visitClassLiteralExpression(ClassLiteralExpression node) {
        Type type = node.getType();
        String typeName = type.getName();
        
        if (typeName == null) {
            return resolveClass(type);
        }
        
        if (typeName.equals("int")) return int.class;
        if (typeName.equals("long")) return long.class;
        if (typeName.equals("short")) return short.class;
        if (typeName.equals("byte")) return byte.class;
        if (typeName.equals("char")) return char.class;
        if (typeName.equals("boolean")) return boolean.class;
        if (typeName.equals("float")) return float.class;
        if (typeName.equals("double")) return double.class;
        if (typeName.equals("void")) return void.class;
        
        try {
            if (typeName.equals("String") || typeName.equals("java.lang.String")) return String.class;
            if (typeName.equals("Object") || typeName.equals("java.lang.Object")) return Object.class;
            if (typeName.equals("Class") || typeName.equals("java.lang.Class")) return Class.class;
            if (typeName.equals("Integer") || typeName.equals("java.lang.Integer")) return Integer.class;
            if (typeName.equals("Long") || typeName.equals("java.lang.Long")) return Long.class;
            if (typeName.equals("Double") || typeName.equals("java.lang.Double")) return Double.class;
            if (typeName.equals("Float") || typeName.equals("java.lang.Float")) return Float.class;
            if (typeName.equals("Boolean") || typeName.equals("java.lang.Boolean")) return Boolean.class;
            if (typeName.equals("Character") || typeName.equals("java.lang.Character")) return Character.class;
            if (typeName.equals("Number") || typeName.equals("java.lang.Number")) return Number.class;
            if (typeName.equals("Exception") || typeName.equals("java.lang.Exception")) return Exception.class;
            if (typeName.equals("RuntimeException") || typeName.equals("java.lang.RuntimeException")) return RuntimeException.class;
            if (typeName.equals("Throwable") || typeName.equals("java.lang.Throwable")) return Throwable.class;
            if (typeName.equals("StringBuilder") || typeName.equals("java.lang.StringBuilder")) return StringBuilder.class;
            if (typeName.equals("ArrayList") || typeName.equals("java.util.ArrayList")) return java.util.ArrayList.class;
            if (typeName.equals("List") || typeName.equals("java.util.List")) return java.util.List.class;
            if (typeName.equals("Map") || typeName.equals("java.util.Map")) return java.util.Map.class;
            if (typeName.equals("Set") || typeName.equals("java.util.Set")) return java.util.Set.class;
            if (typeName.equals("Collection") || typeName.equals("java.util.Collection")) return java.util.Collection.class;
            if (typeName.equals("Iterable") || typeName.equals("java.lang.Iterable")) return Iterable.class;
            if (typeName.equals("Comparable") || typeName.equals("java.lang.Comparable")) return Comparable.class;
            if (typeName.equals("Runnable") || typeName.equals("java.lang.Runnable")) return Runnable.class;
            if (typeName.equals("Function") || typeName.equals("java.util.function.Function")) return java.util.function.Function.class;
            if (typeName.equals("Consumer") || typeName.equals("java.util.function.Consumer")) return java.util.function.Consumer.class;
            if (typeName.equals("Supplier") || typeName.equals("java.util.function.Supplier")) return java.util.function.Supplier.class;
            if (typeName.equals("Predicate") || typeName.equals("java.util.function.Predicate")) return java.util.function.Predicate.class;
            
            ScriptClass scriptClass = resolveClass(type);
            if (scriptClass != null) {
                return scriptClass;
            }
            
            try {
                return Class.forName(typeName);
            } catch (ClassNotFoundException e) {
                return resolveClass(type);
            }
        } catch (Exception e) {
            return resolveClass(type);
        }
    }
    
    @Override
    public Object visitLambdaExpression(LambdaExpression node) {
        return new LambdaObject(node, currentEnv);
    }
    
    @Override
    public Object visitMethodReferenceExpression(MethodReferenceExpression node) {
        return new MethodReferenceObject(node, currentEnv);
    }
    
    @Override
    public Object visitParenthesizedExpression(ParenthesizedExpression node) {
        return node.getExpression().accept(this);
    }
    
    @Override
    public Object visitTypeParameter(TypeParameter node) {
        return null;
    }
    
    @Override
    public Object visitTypeArgument(TypeArgument node) {
        return null;
    }
    
    @Override
    public Object visitAnnotation(Annotation node) {
        return null;
    }
    
    @Override
    public Object visitEnumConstant(EnumConstant node) {
        return null;
    }
    
    private ScriptClass resolveClass(Type type) {
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
        
        if (scriptClass == null && currentEnv.getCurrentClass() != null) {
            String currentClassName = currentEnv.getCurrentClass().getName();
            scriptClass = globalEnv.getClass(currentClassName + "." + name);
        }
        
        return scriptClass;
    }
    
    private List<Object> convertArgs(List<Expression> expressions) {
        List<Object> args = new ArrayList<>();
        for (Expression expr : expressions) {
            args.add(expr.accept(this));
        }
        return args;
    }
    
    private boolean toBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        return value != null;
    }
    
    private int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (Character) value;
        throw new RuntimeException("Cannot convert to int: " + value);
    }
    
    private long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof Character) return (Character) value;
        throw new RuntimeException("Cannot convert to long: " + value);
    }
    
    private float toFloat(Object value) {
        if (value instanceof Number) return ((Number) value).floatValue();
        throw new RuntimeException("Cannot convert to float: " + value);
    }
    
    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof Character) return (Character) value;
        throw new RuntimeException("Cannot convert to double: " + value);
    }
    
    private int compareNumbers(Object left, Object right) {
        double leftVal = toDouble(left);
        double rightVal = toDouble(right);
        return Double.compare(leftVal, rightVal);
    }
    
    public Environment getGlobalEnv() {
        return globalEnv;
    }
    
    public Environment getCurrentEnv() {
        return currentEnv;
    }
    
    public void setCurrentEnv(Environment env) {
        this.currentEnv = env;
    }
}
