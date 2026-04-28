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
    
    public Interpreter() {
        this.globalEnv = new Environment();
        this.currentEnv = globalEnv;
        this.loadedClasses = new HashMap<>();
        this.stdLib = new StandardLibrary(this);
        initializeBuiltInClasses();
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
            ScriptMethod mainMethod = findMainMethod(mainClass.getName());
            if (mainMethod != null) {
                return invokeMethod(null, mainMethod, Arrays.asList(new Object[0]));
            }
        }
        
        return null;
    }
    
    private ScriptMethod findMainMethod(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass == null) return null;
        
        List<ScriptMethod> methods = scriptClass.getMethods("main");
        for (ScriptMethod method : methods) {
            if (method.isStatic() && 
                method.getParameters().size() == 1 &&
                method.getParameters().get(0).getType().getName().equals("String") &&
                method.getParameters().get(0).getType().getArrayDimensions() == 1) {
                return method;
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
                field.getInitializer(), scriptClass);
            scriptClass.addField(scriptField);
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), method.getModifiers(), method.getReturnType(),
                method.getParameters(), method.isVarArgs(), method.getBody(),
                scriptClass, false, method.isDefault());
            scriptClass.addMethod(scriptMethod);
        }
        
        for (ConstructorDeclaration constructor : node.getConstructors()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                constructor.getName(), constructor.getModifiers(), null,
                constructor.getParameters(), false, constructor.getBody(),
                scriptClass, true, false);
            scriptClass.addConstructor(scriptMethod);
        }
        
        for (InitializerBlock init : node.getInitializers()) {
            if (init.isStatic()) {
                scriptClass.addStaticInitializer(init);
            } else {
                scriptClass.addInstanceInitializer(init);
            }
        }
        
        return null;
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
                scriptClass, false, method.isDefault());
            scriptClass.addMethod(scriptMethod);
        }
        
        for (FieldDeclaration constant : node.getConstants()) {
            ScriptField scriptField = new ScriptField(
                constant.getName(), constant.getModifiers(), constant.getType(),
                constant.getInitializer(), scriptClass);
            scriptClass.addField(scriptField);
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
        
        Map<String, RuntimeObject> enumConstants = new HashMap<>();
        int ordinal = 0;
        for (EnumConstant constant : node.getConstants()) {
            RuntimeObject enumObj = new RuntimeObject(scriptClass);
            enumObj.setField("name", constant.getName());
            enumObj.setField("ordinal", ordinal);
            enumConstants.put(constant.getName(), enumObj);
            ordinal++;
        }
        
        scriptClass.addField(new ScriptField("enumConstants", Modifier.PUBLIC | Modifier.STATIC,
                                            new Type(null, "Map", null, 0, null), null, scriptClass));
        
        for (FieldDeclaration field : node.getFields()) {
            ScriptField scriptField = new ScriptField(
                field.getName(), field.getModifiers(), field.getType(),
                field.getInitializer(), scriptClass);
            scriptClass.addField(scriptField);
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), method.getModifiers(), method.getReturnType(),
                method.getParameters(), method.isVarArgs(), method.getBody(),
                scriptClass, false, method.isDefault());
            scriptClass.addMethod(scriptMethod);
        }
        
        for (ConstructorDeclaration constructor : node.getConstructors()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                constructor.getName(), constructor.getModifiers(), null,
                constructor.getParameters(), false, constructor.getBody(),
                scriptClass, true, false);
            scriptClass.addConstructor(scriptMethod);
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
        
        try {
            for (TryStatement.ResourceDeclaration resource : node.getResources()) {
                Object res = resource.getExpression().accept(this);
                resources.add(res);
            }
            
            node.getTryBlock().accept(this);
        } catch (ReturnException | BreakException | ContinueException e) {
            throw e;
        } catch (RuntimeException e) {
            boolean caught = false;
            
            for (CatchClause catchClause : node.getCatchClauses()) {
                if (matchesException(e, catchClause.getExceptionTypes())) {
                    Environment previous = currentEnv;
                    currentEnv = currentEnv.push();
                    
                    try {
                        currentEnv.defineVariable(catchClause.getExceptionName(), e);
                        catchClause.getBody().accept(this);
                        caught = true;
                        break;
                    } finally {
                        currentEnv = previous;
                    }
                }
            }
            
            if (!caught) {
                throw e;
            }
        } finally {
            if (node.getFinallyBlock() != null) {
                node.getFinallyBlock().accept(this);
            }
        }
        
        return null;
    }
    
    private boolean matchesException(RuntimeException e, List<Type> exceptionTypes) {
        for (Type type : exceptionTypes) {
            String typeName = type.getName();
            if (typeName.equals("Exception") || typeName.equals("RuntimeException") ||
                typeName.equals("Throwable") || e.getClass().getSimpleName().equals(typeName)) {
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
        
        ScriptClass currentClass = currentEnv.getCurrentClass();
        if (currentClass != null) {
            ScriptField field = currentClass.getField(name);
            if (field != null && field.isStatic()) {
                return currentClass.getFields().get(name);
            }
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
            currentEnv.setVariable(((IdentifierExpression) target).getName(), value);
        } else if (target instanceof FieldAccessExpression) {
            FieldAccessExpression fieldAccess = (FieldAccessExpression) target;
            Object obj = fieldAccess.getTarget().accept(this);
            if (obj instanceof RuntimeObject) {
                ((RuntimeObject) obj).setField(fieldAccess.getFieldName(), value);
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
            }
        }
        
        if (target instanceof ScriptClass) {
            ScriptClass scriptClass = (ScriptClass) target;
            ScriptMethod method = scriptClass.getMethod(node.getMethodName(), args);
            if (method != null && method.isStatic()) {
                return invokeMethod(null, method, args);
            }
        }
        
        if (target instanceof RuntimeObject) {
            RuntimeObject obj = (RuntimeObject) target;
            ScriptMethod method = obj.getScriptClass().getMethod(node.getMethodName(), args);
            if (method != null) {
                return invokeMethod(obj, method, args);
            }
        }
        
        return stdLib.invokeMethod(target, node.getMethodName(), args);
    }
    
    private Object invokeMethod(RuntimeObject target, ScriptMethod method, List<Object> args) {
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
                return scriptClass.getFields().get(node.getFieldName());
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
        RuntimeObject thisObj = currentEnv.getThisObject();
        if (thisObj != null && thisObj.getScriptClass().getSuperClass() != null) {
            return new SuperObject(thisObj, thisObj.getScriptClass().getSuperClass());
        }
        return thisObj;
    }
    
    @Override
    public Object visitClassLiteralExpression(ClassLiteralExpression node) {
        return resolveClass(node.getType());
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
        
        if (name.equals("int") || name.equals("long") || name.equals("short") ||
            name.equals("byte") || name.equals("char") || name.equals("boolean") ||
            name.equals("float") || name.equals("double") || name.equals("void")) {
            return null;
        }
        
        ScriptClass scriptClass = globalEnv.getClass(name);
        if (scriptClass == null) {
            scriptClass = stdLib.getStandardClass(name);
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
