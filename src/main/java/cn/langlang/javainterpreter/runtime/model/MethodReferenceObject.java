package cn.langlang.javainterpreter.runtime.model;

import cn.langlang.javainterpreter.ast.expression.ClassLiteralExpression;
import cn.langlang.javainterpreter.ast.expression.Expression;
import cn.langlang.javainterpreter.ast.expression.IdentifierExpression;
import cn.langlang.javainterpreter.ast.expression.MethodReferenceExpression;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.interpreter.ExecutionContext;
import cn.langlang.javainterpreter.runtime.environment.Environment;
import java.util.ArrayList;
import java.util.List;

public class MethodReferenceObject implements Callable {
    private final MethodReferenceExpression methodRef;
    private final Environment closureEnv;
    
    public MethodReferenceObject(MethodReferenceExpression methodRef, Environment closureEnv) {
        this.methodRef = methodRef;
        this.closureEnv = closureEnv;
    }
    
    public MethodReferenceExpression getMethodRef() { return methodRef; }
    public Environment getClosureEnv() { return closureEnv; }
    
    @Override
    public Object call(ExecutionContext context, Object target, List<Object> arguments) {
        MethodReferenceExpression methodRef = this.methodRef;
        Expression targetExpr = methodRef.getTarget();
        String methodName = methodRef.getMethodName();
        
        if (methodName.equals("new")) {
            if (targetExpr instanceof ClassLiteralExpression) {
                Type type = ((ClassLiteralExpression) targetExpr).getType();
                if (type.getArrayDimensions() > 0 || type.getName().equals("int") || 
                    type.getName().equals("long") || type.getName().equals("double")) {
                    int size = arguments.isEmpty() ? 0 : toInt(arguments.get(0));
                    return createArray(type, size);
                }
            }
            ScriptClass scriptClass = resolveClassFromExpression(context, targetExpr);
            if (scriptClass != null) {
                ScriptMethod constructor = findConstructor(scriptClass, arguments);
                if (constructor != null) {
                    RuntimeObject instance = new RuntimeObject(scriptClass);
                    initializeFields(context, scriptClass, instance);
                    runInstanceInitializers(context, scriptClass, instance);
                    context.invokeMethod(instance, constructor, arguments);
                    return instance;
                }
            }
        }
        
        if (targetExpr != null) {
            Object targetObj = context.evaluateExpression(targetExpr);
            if (targetObj instanceof ScriptClass) {
                ScriptClass scriptClass = (ScriptClass) targetObj;
                ScriptMethod method = scriptClass.getMethod(methodName, arguments);
                if (method != null && method.isStatic()) {
                    return context.invokeMethod(null, method, arguments);
                }
            }
            if (targetObj instanceof RuntimeObject) {
                RuntimeObject runtimeObj = (RuntimeObject) targetObj;
                ScriptMethod method = runtimeObj.getScriptClass().getMethod(methodName, arguments);
                if (method != null) {
                    return context.invokeMethod(runtimeObj, method, arguments);
                }
            }
        }
        
        return null;
    }
    
    private ScriptClass resolveClassFromExpression(ExecutionContext context, Expression expr) {
        if (expr instanceof ClassLiteralExpression) {
            return context.resolveClass(((ClassLiteralExpression) expr).getType());
        }
        if (expr instanceof IdentifierExpression) {
            return context.getGlobalEnv().getClass(((IdentifierExpression) expr).getName());
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
        List<cn.langlang.javainterpreter.ast.declaration.ParameterDeclaration> params = constructor.getParameters();
        if (params.size() != args.size() && !constructor.isVarArgs()) {
            return -1;
        }
        
        return args.size();
    }
    
    private void initializeFields(ExecutionContext context, ScriptClass scriptClass, RuntimeObject instance) {
        if (scriptClass.getSuperClass() != null) {
            initializeFields(context, scriptClass.getSuperClass(), instance);
        }
        
        for (ScriptField field : scriptClass.getFields().values()) {
            if (!field.isStatic()) {
                Object value = null;
                if (field.getInitializer() != null) {
                    value = context.evaluateExpression(field.getInitializer());
                }
                instance.setField(field.getName(), value);
            }
        }
    }
    
    private void runInstanceInitializers(ExecutionContext context, ScriptClass scriptClass, RuntimeObject instance) {
        if (scriptClass.getSuperClass() != null) {
            runInstanceInitializers(context, scriptClass.getSuperClass(), instance);
        }
        
        Environment previous = context.getCurrentEnv();
        context.setCurrentEnv(context.getCurrentEnv().push());
        context.getCurrentEnv().setThisObject(instance);
        context.getCurrentEnv().setCurrentClass(scriptClass);
        
        try {
            for (cn.langlang.javainterpreter.ast.declaration.InitializerBlock init : scriptClass.getInstanceInitializers()) {
                context.executeStatement(init.getBody());
            }
        } finally {
            context.setCurrentEnv(previous);
        }
    }
    
    private int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (Character) value;
        throw new RuntimeException("Cannot convert to int: " + value);
    }
}
