package cn.langlang.javainterpreter.interpreter.evaluator;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.base.AbstractASTVisitor;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.declaration.*;
import cn.langlang.javainterpreter.ast.expression.*;
import cn.langlang.javainterpreter.ast.misc.Annotation;
import cn.langlang.javainterpreter.ast.misc.EnumConstant;
import cn.langlang.javainterpreter.ast.statement.BlockStatement;
import cn.langlang.javainterpreter.ast.statement.LocalVariableDeclaration;
import cn.langlang.javainterpreter.ast.statement.LocalClassDeclarationStatement;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.interpreter.Interpreter;
import cn.langlang.javainterpreter.interpreter.exception.ReturnException;
import cn.langlang.javainterpreter.lexer.TokenType;
import cn.langlang.javainterpreter.runtime.environment.Environment;
import cn.langlang.javainterpreter.runtime.model.*;
import cn.langlang.javainterpreter.runtime.nativesupport.StandardLibrary;
import cn.langlang.javainterpreter.runtime.nativesupport.StaticImportRegistry;
import cn.langlang.javainterpreter.runtime.nativesupport.TypeRegistry;
import java.util.*;
import java.util.function.Function;

public class ExpressionEvaluator extends AbstractASTVisitor<Object> {
    private final Interpreter interpreter;
    private final TypeRegistry typeRegistry;
    
    public ExpressionEvaluator(Interpreter interpreter) {
        this.interpreter = interpreter;
        this.typeRegistry = new TypeRegistry();
    }
    
    @Override
    public Object visitLiteralExpression(LiteralExpression node) {
        return node.getValue();
    }
    
    @Override
    public Object visitIdentifierExpression(IdentifierExpression node) {
        String name = node.getName();
        
        ScriptClass currentClass = interpreter.getCurrentEnv().getCurrentClass();
        while (currentClass != null) {
            ScriptField field = currentClass.getField(name);
            if (field != null && field.isStatic()) {
                interpreter.initializeClass(currentClass);
                String fieldKey = currentClass.getName() + "." + name;
                if (interpreter.getGlobalEnv().hasVariable(fieldKey)) {
                    return interpreter.getGlobalEnv().getVariable(fieldKey);
                }
            }
            currentClass = currentClass.getEnclosingClass();
        }
        
        if (interpreter.getCurrentEnv().hasVariable(name)) {
            return interpreter.getCurrentEnv().getVariable(name);
        }
        
        if (interpreter.getCurrentEnv().hasClass(name)) {
            return interpreter.getCurrentEnv().getClass(name);
        }
        
        RuntimeObject thisObj = interpreter.getCurrentEnv().getThisObject();
        if (thisObj != null && thisObj.hasField(name)) {
            return thisObj.getField(name);
        }
        
        return interpreter.getStdLib().resolveStaticImport(name);
    }
    
    @Override
    public Object visitBinaryExpression(BinaryExpression node) {
        Object left = node.getLeft().accept(this);
        
        if (node.getOperator() == TokenType.AND) {
            if (!interpreter.toBoolean(left)) return false;
            return interpreter.toBoolean(node.getRight().accept(this));
        }
        
        if (node.getOperator() == TokenType.OR) {
            if (interpreter.toBoolean(left)) return true;
            return interpreter.toBoolean(node.getRight().accept(this));
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
                    return interpreter.toDouble(left) + interpreter.toDouble(right);
                }
                if (left instanceof Float || right instanceof Float) {
                    return interpreter.toFloat(left) + interpreter.toFloat(right);
                }
                if (left instanceof Long || right instanceof Long) {
                    return interpreter.toLong(left) + interpreter.toLong(right);
                }
                return interpreter.toInt(left) + interpreter.toInt(right);
            case MINUS:
                if (left instanceof Double || right instanceof Double) {
                    return interpreter.toDouble(left) - interpreter.toDouble(right);
                }
                if (left instanceof Float || right instanceof Float) {
                    return interpreter.toFloat(left) - interpreter.toFloat(right);
                }
                if (left instanceof Long || right instanceof Long) {
                    return interpreter.toLong(left) - interpreter.toLong(right);
                }
                return interpreter.toInt(left) - interpreter.toInt(right);
            case STAR:
                if (left instanceof Double || right instanceof Double) {
                    return interpreter.toDouble(left) * interpreter.toDouble(right);
                }
                if (left instanceof Float || right instanceof Float) {
                    return interpreter.toFloat(left) * interpreter.toFloat(right);
                }
                if (left instanceof Long || right instanceof Long) {
                    return interpreter.toLong(left) * interpreter.toLong(right);
                }
                return interpreter.toInt(left) * interpreter.toInt(right);
            case SLASH:
                if (left instanceof Double || right instanceof Double) {
                    return interpreter.toDouble(left) / interpreter.toDouble(right);
                }
                if (left instanceof Float || right instanceof Float) {
                    return interpreter.toFloat(left) / interpreter.toFloat(right);
                }
                if (left instanceof Long || right instanceof Long) {
                    return interpreter.toLong(left) / interpreter.toLong(right);
                }
                return interpreter.toInt(left) / interpreter.toInt(right);
            case PERCENT:
                if (left instanceof Double || right instanceof Double) {
                    return interpreter.toDouble(left) % interpreter.toDouble(right);
                }
                if (left instanceof Float || right instanceof Float) {
                    return interpreter.toFloat(left) % interpreter.toFloat(right);
                }
                if (left instanceof Long || right instanceof Long) {
                    return interpreter.toLong(left) % interpreter.toLong(right);
                }
                return interpreter.toInt(left) % interpreter.toInt(right);
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
                    return interpreter.toLong(left) & interpreter.toLong(right);
                }
                return interpreter.toInt(left) & interpreter.toInt(right);
            case PIPE:
                if (left instanceof Integer && right instanceof Integer) {
                    return (Integer) left | (Integer) right;
                }
                if (left instanceof Long || right instanceof Long) {
                    return interpreter.toLong(left) | interpreter.toLong(right);
                }
                return interpreter.toInt(left) | interpreter.toInt(right);
            case CARET:
                if (left instanceof Integer && right instanceof Integer) {
                    return (Integer) left ^ (Integer) right;
                }
                if (left instanceof Long || right instanceof Long) {
                    return interpreter.toLong(left) ^ interpreter.toLong(right);
                }
                return interpreter.toInt(left) ^ interpreter.toInt(right);
            case LSHIFT:
                return interpreter.toInt(left) << interpreter.toInt(right);
            case RSHIFT:
                return interpreter.toInt(left) >> interpreter.toInt(right);
            case URSHIFT:
                return interpreter.toInt(left) >>> interpreter.toInt(right);
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
                return !interpreter.toBoolean(operand);
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
            
            ScriptClass currentClass = interpreter.getCurrentEnv().getCurrentClass();
            while (currentClass != null) {
                ScriptField field = currentClass.getField(name);
                if (field != null && field.isStatic()) {
                    String fieldKey = currentClass.getName() + "." + name;
                    interpreter.getGlobalEnv().defineVariable(fieldKey, value);
                    return;
                }
                currentClass = currentClass.getEnclosingClass();
            }
            
            interpreter.getCurrentEnv().setVariable(name, value);
        } else if (target instanceof FieldAccessExpression) {
            FieldAccessExpression fieldAccess = (FieldAccessExpression) target;
            Object obj = fieldAccess.getTarget().accept(this);
            if (obj instanceof RuntimeObject) {
                ((RuntimeObject) obj).setField(fieldAccess.getFieldName(), value);
            } else if (obj instanceof ScriptClass) {
                ScriptClass scriptClass = (ScriptClass) obj;
                String fieldKey = scriptClass.getName() + "." + fieldAccess.getFieldName();
                interpreter.getGlobalEnv().defineVariable(fieldKey, value);
            }
        } else if (target instanceof ArrayAccessExpression) {
            ArrayAccessExpression arrayAccess = (ArrayAccessExpression) target;
            Object array = arrayAccess.getArray().accept(this);
            Object index = arrayAccess.getIndex().accept(this);
            int idx = interpreter.toInt(index);
            if (array instanceof Object[]) {
                ((Object[]) array)[idx] = value;
            } else if (array instanceof int[]) {
                ((int[]) array)[idx] = interpreter.toInt(value);
            } else if (array instanceof long[]) {
                ((long[]) array)[idx] = interpreter.toLong(value);
            } else if (array instanceof double[]) {
                ((double[]) array)[idx] = interpreter.toDouble(value);
            } else if (array instanceof float[]) {
                ((float[]) array)[idx] = interpreter.toFloat(value);
            } else if (array instanceof boolean[]) {
                ((boolean[]) array)[idx] = interpreter.toBoolean(value);
            } else if (array instanceof char[]) {
                ((char[]) array)[idx] = interpreter.toChar(value);
            } else if (array instanceof byte[]) {
                ((byte[]) array)[idx] = interpreter.toByte(value);
            } else if (array instanceof short[]) {
                ((short[]) array)[idx] = interpreter.toShort(value);
            }
        }
    }
    
    @Override
    public Object visitTernaryExpression(TernaryExpression node) {
        Object condition = node.getCondition().accept(this);
        if (interpreter.toBoolean(condition)) {
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
                    return interpreter.toDouble(oldValue) + interpreter.toDouble(newValue);
                }
                return interpreter.toInt(oldValue) + interpreter.toInt(newValue);
            case MINUS_ASSIGN:
                return interpreter.toInt(oldValue) - interpreter.toInt(newValue);
            case STAR_ASSIGN:
                return interpreter.toInt(oldValue) * interpreter.toInt(newValue);
            case SLASH_ASSIGN:
                return interpreter.toInt(oldValue) / interpreter.toInt(newValue);
            case PERCENT_ASSIGN:
                return interpreter.toInt(oldValue) % interpreter.toInt(newValue);
            case AND_ASSIGN:
                return interpreter.toInt(oldValue) & interpreter.toInt(newValue);
            case OR_ASSIGN:
                return interpreter.toInt(oldValue) | interpreter.toInt(newValue);
            case XOR_ASSIGN:
                return interpreter.toInt(oldValue) ^ interpreter.toInt(newValue);
            case LSHIFT_ASSIGN:
                return interpreter.toInt(oldValue) << interpreter.toInt(newValue);
            case RSHIFT_ASSIGN:
                return interpreter.toInt(oldValue) >> interpreter.toInt(newValue);
            case URSHIFT_ASSIGN:
                return interpreter.toInt(oldValue) >>> interpreter.toInt(newValue);
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
        
        if (target == null && node.getTarget() != null) {
            if (node.getTarget() instanceof IdentifierExpression) {
                String varName = ((IdentifierExpression) node.getTarget()).getName();
                Class<?> javaClass = typeRegistry.getClassLiteral(varName);
                if (javaClass != null) {
                    target = javaClass;
                }
            }
        }
        
        List<Object> args = new ArrayList<>();
        for (Expression arg : node.getArguments()) {
            args.add(arg.accept(this));
        }
        
        if (target == null) {
            Object var = interpreter.getCurrentEnv().getVariable(node.getMethodName());
            if (var instanceof ScriptClass) {
                target = var;
            } else if (var instanceof StandardLibrary.StaticMethodHolder) {
                return ((StandardLibrary.StaticMethodHolder) var).invoke(args);
            } else if (var instanceof StaticImportRegistry.StaticMethodHolder) {
                return ((StaticImportRegistry.StaticMethodHolder) var).invoke(args);
            }
        }
        
        if (target == null && node.getTarget() == null) {
            Object staticImport = interpreter.getStdLib().resolveStaticImport(node.getMethodName());
            if (staticImport instanceof StaticImportRegistry.StaticMethodHolder) {
                return ((StaticImportRegistry.StaticMethodHolder) staticImport).invoke(args);
            } else if (staticImport instanceof StandardLibrary.StaticMethodHolder) {
                return ((StandardLibrary.StaticMethodHolder) staticImport).invoke(args);
            }
        }

        if (target == null && node.getTarget() == null) {
            ScriptClass currentClass = interpreter.getCurrentEnv().getCurrentClass();
            if (currentClass != null) {
                ScriptMethod method = currentClass.getMethod(node.getMethodName(), args);
                if (method != null && method.isStatic()) {
                    target = currentClass;
                }
                
                if (method == null && currentClass.getEnclosingClass() != null) {
                    ScriptClass enclosingClass = currentClass.getEnclosingClass();
                    method = enclosingClass.getMethod(node.getMethodName(), args);
                    if (method != null && method.isStatic()) {
                        target = enclosingClass;
                    }
                }
            }
            
            if (target == null) {
                RuntimeObject thisObj = interpreter.getCurrentEnv().getThisObject();
                if (thisObj != null) {
                    target = thisObj;
                }
            }
        }
        
        if (target == null && node.getTarget() instanceof IdentifierExpression) {
            String varName = ((IdentifierExpression) node.getTarget()).getName();
            target = interpreter.getCurrentEnv().getVariable(varName);
            
            if (target == null) {
                ScriptClass scriptClass = interpreter.getGlobalEnv().getClass(varName);
                if (scriptClass != null) {
                    target = scriptClass;
                }
            }
            
            if (target == null) {
                Class<?> javaClass = typeRegistry.getClassLiteral(varName);
                if (javaClass != null) {
                    target = javaClass;
                }
            }
        }
        
        if (target instanceof Callable) {
            return ((Callable) target).call(interpreter, target, args);
        }
        
        if (target instanceof Class) {
            return interpreter.getStdLib().invokeMethod(target, node.getMethodName(), args);
        }
        
        if (target instanceof java.lang.reflect.Method) {
            return interpreter.getStdLib().invokeMethod(target, node.getMethodName(), args);
        }
        
        if (target instanceof java.lang.reflect.Field) {
            return interpreter.getStdLib().invokeMethod(target, node.getMethodName(), args);
        }
        
        if (target instanceof java.lang.reflect.Constructor) {
            return interpreter.getStdLib().invokeMethod(target, node.getMethodName(), args);
        }
        
        if (target instanceof ScriptClass) {
            ScriptClass scriptClass = (ScriptClass) target;
            interpreter.initializeClass(scriptClass);
            ScriptMethod method = scriptClass.getMethod(node.getMethodName(), args);
            if (method != null && method.isStatic()) {
                return interpreter.invokeMethod(null, method, args);
            }
        }
        
        if (target instanceof RuntimeObject) {
            RuntimeObject obj = (RuntimeObject) target;
            
            if (node.getMethodName().equals("getClass") && args.isEmpty()) {
                return obj.getScriptClass();
            }
            
            ScriptMethod method = obj.getScriptClass().getMethod(node.getMethodName(), args);
            if (method != null) {
                return interpreter.invokeMethod(obj, method, args);
            }
        }
        
        if (target instanceof InterfaceSuperObject) {
            InterfaceSuperObject iso = (InterfaceSuperObject) target;
            ScriptMethod method = iso.getInterfaceClass().getMethod(node.getMethodName(), args);
            if (method != null && method.isDefault()) {
                return interpreter.invokeMethod(iso.getThisObject(), method, args);
            }
        }
        
        if (target instanceof SuperObject) {
            SuperObject superObj = (SuperObject) target;
            ScriptMethod method = superObj.getSuperClass().getMethod(node.getMethodName(), args);
            if (method != null) {
                return interpreter.invokeMethod(superObj.getTarget(), method, args);
            }
        }
        
        if (target instanceof StandardLibrary.StaticMethodHolder) {
            return interpreter.getStdLib().invokeMethod(target, node.getMethodName(), args);
        }
        
        return interpreter.getStdLib().invokeMethod(target, node.getMethodName(), args);
    }
    
    @Override
    public Object visitFieldAccessExpression(FieldAccessExpression node) {
        Object target = node.getTarget().accept(this);
        
        if (target instanceof ScriptClass) {
            ScriptClass scriptClass = (ScriptClass) target;
            ScriptField field = scriptClass.getField(node.getFieldName());
            if (field != null && field.isStatic()) {
                String varName = scriptClass.getName() + "." + node.getFieldName();
                if (interpreter.getCurrentEnv().hasVariable(varName)) {
                    return interpreter.getCurrentEnv().getVariable(varName);
                }
                if (interpreter.getGlobalEnv().hasVariable(varName)) {
                    return interpreter.getGlobalEnv().getVariable(varName);
                }
                return null;
            }
            
            if (interpreter.getCurrentEnv().hasVariable(node.getFieldName())) {
                return interpreter.getCurrentEnv().getVariable(node.getFieldName());
            }
        }
        
        if (target instanceof Class) {
            Class<?> clazz = (Class<?>) target;
            try {
                java.lang.reflect.Field field = clazz.getField(node.getFieldName());
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    return field.get(null);
                }
            } catch (NoSuchFieldException e) {
            } catch (IllegalAccessException e) {
            }
            try {
                for (java.lang.reflect.Field field : clazz.getFields()) {
                    if (field.getName().equals(node.getFieldName())) {
                        return field.get(null);
                    }
                }
            } catch (Exception e) {
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
        
        return interpreter.getStdLib().getField(target, node.getFieldName());
    }
    
    @Override
    public Object visitArrayAccessExpression(ArrayAccessExpression node) {
        Object array = node.getArray().accept(this);
        Object index = node.getIndex().accept(this);
        
        if (array instanceof Object[]) {
            return ((Object[]) array)[interpreter.toInt(index)];
        } else if (array instanceof int[]) {
            return ((int[]) array)[interpreter.toInt(index)];
        } else if (array instanceof long[]) {
            return ((long[]) array)[interpreter.toInt(index)];
        } else if (array instanceof double[]) {
            return ((double[]) array)[interpreter.toInt(index)];
        } else if (array instanceof float[]) {
            return ((float[]) array)[interpreter.toInt(index)];
        } else if (array instanceof boolean[]) {
            return ((boolean[]) array)[interpreter.toInt(index)];
        } else if (array instanceof char[]) {
            return ((char[]) array)[interpreter.toInt(index)];
        } else if (array instanceof byte[]) {
            return ((byte[]) array)[interpreter.toInt(index)];
        } else if (array instanceof short[]) {
            return ((short[]) array)[interpreter.toInt(index)];
        }
        
        throw new RuntimeException("Cannot access array element on non-array type");
    }
    
    @Override
    public Object visitNewObjectExpression(NewObjectExpression node) {
        List<ASTNode> anonymousClassBody = node.getAnonymousClassBody();
        
        if (anonymousClassBody != null && !anonymousClassBody.isEmpty()) {
            return createAnonymousClassInstance(node);
        }
        
        ScriptClass scriptClass = interpreter.resolveClass(node.getType());
        
        if (scriptClass == null) {
            return interpreter.getStdLib().createObject(node.getType().getName(), 
                convertArgs(node.getArguments()));
        }
        
        RuntimeObject instance = new RuntimeObject(scriptClass);
        
        interpreter.initializeFields(scriptClass, instance);
        
        interpreter.runInstanceInitializers(scriptClass, instance);
        
        List<Object> args = new ArrayList<>();
        for (Expression arg : node.getArguments()) {
            args.add(arg.accept(this));
        }
        
        ScriptMethod constructor = interpreter.findConstructor(scriptClass, args);
        if (constructor != null) {
            interpreter.invokeMethod(instance, constructor, args);
        }
        
        return instance;
    }
    
    private Object createAnonymousClassInstance(NewObjectExpression node) {
        Type baseType = node.getType();
        List<ASTNode> anonymousClassBody = node.getAnonymousClassBody();
        
        ScriptClass baseClass = interpreter.resolveClass(baseType);
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
        
        ScriptClass enclosingClass = interpreter.getCurrentEnv().getCurrentClass();
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
        
        interpreter.getGlobalEnv().defineClass(anonymousClassName, anonymousClass);
        interpreter.getLoadedClasses().put(anonymousClassName, anonymousClass);
        
        RuntimeObject instance = new RuntimeObject(anonymousClass);
        
        Set<String> localVariables = new HashSet<>();
        for (ASTNode member : anonymousClassBody) {
            if (member instanceof MethodDeclaration) {
                MethodDeclaration method = (MethodDeclaration) member;
                for (ParameterDeclaration param : method.getParameters()) {
                    localVariables.add(param.getName());
                }
            } else if (member instanceof ConstructorDeclaration) {
                ConstructorDeclaration constructor = (ConstructorDeclaration) member;
                for (ParameterDeclaration param : constructor.getParameters()) {
                    localVariables.add(param.getName());
                }
            }
        }
        
        Set<String> usedVariables = new HashSet<>();
        for (ASTNode member : anonymousClassBody) {
            collectUsedVariables(member, usedVariables);
        }
        
        for (String varName : usedVariables) {
            if (!localVariables.contains(varName) && interpreter.getCurrentEnv().hasVariable(varName)) {
                Object value = interpreter.getCurrentEnv().getVariable(varName);
                instance.setCapturedVariable(varName, value);
            }
        }
        
        interpreter.initializeFields(anonymousClass, instance);
        
        interpreter.runInstanceInitializers(anonymousClass, instance);
        
        List<Object> args = new ArrayList<>();
        for (Expression arg : node.getArguments()) {
            args.add(arg.accept(this));
        }
        
        ScriptMethod constructor = interpreter.findConstructor(anonymousClass, args);
        if (constructor != null) {
            interpreter.invokeMethod(instance, constructor, args);
        } else if (superClass != null) {
            constructor = interpreter.findConstructor(superClass, args);
            if (constructor != null) {
                interpreter.invokeMethod(instance, constructor, args);
            }
        }
        
        return instance;
    }
    
    private void collectUsedVariables(ASTNode node, Set<String> variables) {
        if (node == null) return;
        
        if (node instanceof IdentifierExpression) {
            IdentifierExpression id = (IdentifierExpression) node;
            variables.add(id.getName());
        } else if (node instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) node;
            collectUsedVariables(binary.getLeft(), variables);
            collectUsedVariables(binary.getRight(), variables);
        } else if (node instanceof MethodInvocationExpression) {
            MethodInvocationExpression call = (MethodInvocationExpression) node;
            collectUsedVariables(call.getTarget(), variables);
            for (Expression arg : call.getArguments()) {
                collectUsedVariables(arg, variables);
            }
        } else if (node instanceof FieldAccessExpression) {
            FieldAccessExpression field = (FieldAccessExpression) node;
            collectUsedVariables(field.getTarget(), variables);
        } else if (node instanceof AssignmentExpression) {
            AssignmentExpression assign = (AssignmentExpression) node;
            collectUsedVariables(assign.getTarget(), variables);
            collectUsedVariables(assign.getValue(), variables);
        } else if (node instanceof LocalVariableDeclaration) {
            LocalVariableDeclaration varDecl = (LocalVariableDeclaration) node;
            for (LocalVariableDeclaration.VariableDeclarator declarator : varDecl.getDeclarators()) {
                if (declarator.getInitializer() != null) {
                    collectUsedVariables(declarator.getInitializer(), variables);
                }
            }
        } else if (node instanceof BlockStatement) {
            BlockStatement block = (BlockStatement) node;
            for (ASTNode stmt : block.getStatements()) {
                collectUsedVariables(stmt, variables);
            }
        } else if (node instanceof cn.langlang.javainterpreter.ast.statement.IfStatement) {
            cn.langlang.javainterpreter.ast.statement.IfStatement ifStmt = (cn.langlang.javainterpreter.ast.statement.IfStatement) node;
            collectUsedVariables(ifStmt.getCondition(), variables);
            collectUsedVariables(ifStmt.getThenStatement(), variables);
            collectUsedVariables(ifStmt.getElseStatement(), variables);
        } else if (node instanceof cn.langlang.javainterpreter.ast.statement.WhileStatement) {
            cn.langlang.javainterpreter.ast.statement.WhileStatement whileStmt = (cn.langlang.javainterpreter.ast.statement.WhileStatement) node;
            collectUsedVariables(whileStmt.getCondition(), variables);
            collectUsedVariables(whileStmt.getBody(), variables);
        } else if (node instanceof cn.langlang.javainterpreter.ast.statement.ForStatement) {
            cn.langlang.javainterpreter.ast.statement.ForStatement forStmt = (cn.langlang.javainterpreter.ast.statement.ForStatement) node;
            collectUsedVariables(forStmt.getInit(), variables);
            collectUsedVariables(forStmt.getCondition(), variables);
            collectUsedVariables(forStmt.getUpdate(), variables);
            collectUsedVariables(forStmt.getBody(), variables);
        } else if (node instanceof cn.langlang.javainterpreter.ast.statement.ReturnStatement) {
            cn.langlang.javainterpreter.ast.statement.ReturnStatement ret = (cn.langlang.javainterpreter.ast.statement.ReturnStatement) node;
            collectUsedVariables(ret.getExpression(), variables);
        } else if (node instanceof cn.langlang.javainterpreter.ast.statement.ExpressionStatement) {
            cn.langlang.javainterpreter.ast.statement.ExpressionStatement exprStmt = (cn.langlang.javainterpreter.ast.statement.ExpressionStatement) node;
            collectUsedVariables(exprStmt.getExpression(), variables);
        } else if (node instanceof cn.langlang.javainterpreter.ast.statement.TryStatement) {
            cn.langlang.javainterpreter.ast.statement.TryStatement tryStmt = (cn.langlang.javainterpreter.ast.statement.TryStatement) node;
            collectUsedVariables(tryStmt.getTryBlock(), variables);
            for (cn.langlang.javainterpreter.ast.misc.CatchClause catchClause : tryStmt.getCatchClauses()) {
                collectUsedVariables(catchClause.getBody(), variables);
            }
            collectUsedVariables(tryStmt.getFinallyBlock(), variables);
        } else if (node instanceof cn.langlang.javainterpreter.ast.statement.ThrowStatement) {
            cn.langlang.javainterpreter.ast.statement.ThrowStatement throwStmt = (cn.langlang.javainterpreter.ast.statement.ThrowStatement) node;
            collectUsedVariables(throwStmt.getExpression(), variables);
        } else if (node instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) node;
            collectUsedVariables(method.getBody(), variables);
        } else if (node instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructor = (ConstructorDeclaration) node;
            collectUsedVariables(constructor.getBody(), variables);
        } else if (node instanceof NewObjectExpression) {
            NewObjectExpression newObj = (NewObjectExpression) node;
            for (Expression arg : newObj.getArguments()) {
                collectUsedVariables(arg, variables);
            }
            if (newObj.getAnonymousClassBody() != null) {
                for (ASTNode member : newObj.getAnonymousClassBody()) {
                    collectUsedVariables(member, variables);
                }
            }
        } else if (node instanceof ArrayAccessExpression) {
            ArrayAccessExpression arrayAccess = (ArrayAccessExpression) node;
            collectUsedVariables(arrayAccess.getArray(), variables);
            collectUsedVariables(arrayAccess.getIndex(), variables);
        } else if (node instanceof NewArrayExpression) {
            NewArrayExpression arrayCreation = (NewArrayExpression) node;
            for (Expression dim : arrayCreation.getDimensions()) {
                collectUsedVariables(dim, variables);
            }
            if (arrayCreation.getInitializer() != null) {
                for (Expression elem : arrayCreation.getInitializer().getElements()) {
                    collectUsedVariables(elem, variables);
                }
            }
        } else if (node instanceof CastExpression) {
            CastExpression cast = (CastExpression) node;
            collectUsedVariables(cast.getExpression(), variables);
        } else if (node instanceof InstanceOfExpression) {
            InstanceOfExpression instanceOf = (InstanceOfExpression) node;
            collectUsedVariables(instanceOf.getExpression(), variables);
        } else if (node instanceof TernaryExpression) {
            TernaryExpression ternary = (TernaryExpression) node;
            collectUsedVariables(ternary.getCondition(), variables);
            collectUsedVariables(ternary.getTrueExpression(), variables);
            collectUsedVariables(ternary.getFalseExpression(), variables);
        } else if (node instanceof LambdaExpression) {
            LambdaExpression lambda = (LambdaExpression) node;
            collectUsedVariables(lambda.getBody(), variables);
        } else if (node instanceof cn.langlang.javainterpreter.ast.statement.SynchronizedStatement) {
            cn.langlang.javainterpreter.ast.statement.SynchronizedStatement sync = (cn.langlang.javainterpreter.ast.statement.SynchronizedStatement) node;
            collectUsedVariables(sync.getLock(), variables);
            collectUsedVariables(sync.getBody(), variables);
        }
    }
    
    private ScriptClass createRunnableInterface() {
        ScriptClass runnableInterface = new ScriptClass(
            "Runnable", "java.lang.Runnable", cn.langlang.javainterpreter.parser.Modifier.ABSTRACT | cn.langlang.javainterpreter.parser.Modifier.INTERFACE,
            null, new ArrayList<>(), null
        );
        
        List<ParameterDeclaration> params = new ArrayList<>();
        Type voidType = new Type(null, "void", new ArrayList<>(), 0, new ArrayList<>());
        ScriptMethod runMethod = new ScriptMethod(
            "run", cn.langlang.javainterpreter.parser.Modifier.PUBLIC | cn.langlang.javainterpreter.parser.Modifier.ABSTRACT, voidType,
            params, false, null, runnableInterface, false, false, new ArrayList<>()
        );
        runnableInterface.addMethod(runMethod);
        
        return runnableInterface;
    }
    
    private ScriptClass createInterfaceFromClass(Class<?> clazz) {
        ScriptClass iface = new ScriptClass(
            clazz.getSimpleName(), clazz.getName(), cn.langlang.javainterpreter.parser.Modifier.INTERFACE,
            null, new ArrayList<>(), null
        );
        
        for (java.lang.reflect.Method method : clazz.getMethods()) {
            if (method.isDefault() || method.isSynthetic()) continue;
            
            List<ParameterDeclaration> params = new ArrayList<>();
            java.lang.reflect.Parameter[] reflectParams = method.getParameters();
            for (java.lang.reflect.Parameter param : reflectParams) {
                cn.langlang.javainterpreter.ast.type.Type paramType = createTypeFromJavaType(param.getType());
                params.add(new ParameterDeclaration(null, 0, paramType, param.getName(), 
                    false, new ArrayList<>()));
            }
            
            cn.langlang.javainterpreter.ast.type.Type returnType = createTypeFromJavaType(method.getReturnType());
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), cn.langlang.javainterpreter.parser.Modifier.PUBLIC | cn.langlang.javainterpreter.parser.Modifier.ABSTRACT, returnType,
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
    
    private cn.langlang.javainterpreter.ast.type.Type createTypeFromJavaType(Class<?> type) {
        String typeName;
        if (type.isPrimitive()) {
            typeName = type.getName();
        } else if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            cn.langlang.javainterpreter.ast.type.Type component = createTypeFromJavaType(componentType);
            return new cn.langlang.javainterpreter.ast.type.Type(null, component.getName(), 
                component.getTypeArguments(), component.getArrayDimensions() + 1, new ArrayList<>());
        } else {
            typeName = type.getSimpleName();
        }
        return new cn.langlang.javainterpreter.ast.type.Type(null, typeName, new ArrayList<>(), 0, new ArrayList<>());
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
            dims[i] = interpreter.toInt(dimensions.get(i).accept(this));
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
        Object result = typeRegistry.castValue(value, typeName);
        
        if (result != value || value == null) {
            return result;
        }
        
        return value;
    }
    
    @Override
    public Object visitInstanceOfExpression(InstanceOfExpression node) {
        Object value = node.getExpression().accept(this);
        
        if (value == null) return false;
        
        Type checkType = node.getType();
        ScriptClass checkClass = interpreter.resolveClass(checkType);
        
        if (checkClass == null) {
            String typeName = checkType.getName();
            int arrayDims = checkType.getArrayDimensions();
            
            if (arrayDims > 0) {
                return checkArrayInstance(value, typeName);
            }
            
            Class<?> typeClass = typeRegistry.getClassLiteral(typeName);
            if (typeClass != null) {
                return typeClass.isInstance(value);
            }
            
            return typeRegistry.isInstance(value, typeName);
        }
        
        if (value instanceof RuntimeObject) {
            ScriptClass valueClass = ((RuntimeObject) value).getScriptClass();
            return checkClass.isAssignableFrom(valueClass);
        }
        
        return false;
    }
    
    private boolean checkArrayInstance(Object value, String typeName) {
        switch (typeName) {
            case "byte": return value instanceof byte[];
            case "short": return value instanceof short[];
            case "int": return value instanceof int[];
            case "long": return value instanceof long[];
            case "char": return value instanceof char[];
            case "float": return value instanceof float[];
            case "double": return value instanceof double[];
            case "boolean": return value instanceof boolean[];
            default: return value.getClass().isArray();
        }
    }
    
    @Override
    public Object visitThisExpression(ThisExpression node) {
        return interpreter.getCurrentEnv().getThisObject();
    }
    
    @Override
    public Object visitSuperExpression(SuperExpression node) {
        String interfaceName = node.getClassName();
        
        if (interfaceName != null) {
            ScriptClass interfaceClass = interpreter.getGlobalEnv().getClass(interfaceName);
            if (interfaceClass != null) {
                return new InterfaceSuperObject(interpreter.getCurrentEnv().getThisObject(), interfaceClass);
            }
        }
        
        RuntimeObject thisObj = interpreter.getCurrentEnv().getThisObject();
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
            return interpreter.resolveClass(type);
        }
        
        Class<?> typeClass = typeRegistry.getClassLiteral(typeName);
        if (typeClass != null) {
            return typeClass;
        }
        
        ScriptClass scriptClass = interpreter.resolveClass(type);
        if (scriptClass != null) {
            return scriptClass;
        }
        
        try {
            return Class.forName(typeName.contains(".") ? typeName : "java.lang." + typeName);
        } catch (ClassNotFoundException e) {
            return interpreter.resolveClass(type);
        }
    }
    
    @Override
    public Object visitLambdaExpression(LambdaExpression node) {
        return new LambdaObject(node, interpreter.getCurrentEnv());
    }
    
    @Override
    public Object visitMethodReferenceExpression(MethodReferenceExpression node) {
        return new MethodReferenceObject(node, interpreter.getCurrentEnv());
    }
    
    @Override
    public Object visitParenthesizedExpression(ParenthesizedExpression node) {
        return node.getExpression().accept(this);
    }
    
    @Override
    public Object visitTypeParameter(cn.langlang.javainterpreter.ast.type.TypeParameter node) {
        return null;
    }
    
    @Override
    public Object visitTypeArgument(cn.langlang.javainterpreter.ast.type.TypeArgument node) {
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
            interpreter.getCurrentEnv().defineVariable(declarator.getName(), value);
        }
        return null;
    }
    
    private List<Object> convertArgs(List<Expression> expressions) {
        List<Object> args = new ArrayList<>();
        for (Expression expr : expressions) {
            args.add(expr.accept(this));
        }
        return args;
    }
    
    private int compareNumbers(Object left, Object right) {
        double leftVal = interpreter.toDouble(left);
        double rightVal = interpreter.toDouble(right);
        return Double.compare(leftVal, rightVal);
    }
    
    @Override
    public Object visitLocalClassDeclarationStatement(LocalClassDeclarationStatement node) {
        return node.getClassDeclaration().accept(interpreter.getDeclarationExecutor());
    }
}
