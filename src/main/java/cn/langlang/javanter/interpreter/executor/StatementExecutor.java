package cn.langlang.javanter.interpreter.executor;

import cn.langlang.javanter.ast.base.AbstractASTVisitor;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.statement.LocalVariableDeclaration;
import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.ast.misc.CatchClause;
import cn.langlang.javanter.ast.misc.CaseLabel;
import cn.langlang.javanter.ast.misc.EnumConstant;
import cn.langlang.javanter.ast.statement.*;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.interpreter.Interpreter;
import cn.langlang.javanter.interpreter.exception.*;
import cn.langlang.javanter.runtime.ExceptionConstants;
import cn.langlang.javanter.runtime.environment.Environment;
import cn.langlang.javanter.runtime.model.*;
import java.util.*;

public class StatementExecutor extends AbstractASTVisitor<Object> {
    private final Interpreter interpreter;
    private boolean inSwitchExpression = false;
    
    public StatementExecutor(Interpreter interpreter) {
        this.interpreter = interpreter;
    }
    
    public void setInSwitchExpression(boolean inSwitchExpression) {
        this.inSwitchExpression = inSwitchExpression;
    }
    
    public boolean isInSwitchExpression() { return inSwitchExpression; }
    
    @Override
    public Object visitBlockStatement(BlockStatement node) {
        Environment previous = interpreter.getCurrentEnv();
        interpreter.setCurrentEnv(interpreter.getCurrentEnv().push());
        
        try {
            for (Statement stmt : node.getStatements()) {
                stmt.accept(this);
            }
        } finally {
            interpreter.setCurrentEnv(previous);
        }
        
        return null;
    }
    
    @Override
    public Object visitIfStatement(IfStatement node) {
        Object condition = node.getCondition().accept(interpreter.getExpressionEvaluator());
        
        if (interpreter.toBoolean(condition)) {
            return node.getThenStatement().accept(this);
        } else if (node.getElseStatement() != null) {
            return node.getElseStatement().accept(this);
        }
        
        return null;
    }
    
    @Override
    public Object visitWhileStatement(WhileStatement node) {
        while (interpreter.toBoolean(node.getCondition().accept(interpreter.getExpressionEvaluator()))) {
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
        } while (interpreter.toBoolean(node.getCondition().accept(interpreter.getExpressionEvaluator())));
        return null;
    }
    
    @Override
    public Object visitForStatement(ForStatement node) {
        Environment previous = interpreter.getCurrentEnv();
        interpreter.setCurrentEnv(interpreter.getCurrentEnv().push());
        
        try {
            if (node.getInit() != null) {
                node.getInit().accept(this);
            }
            
            while (node.getCondition() == null || interpreter.toBoolean(node.getCondition().accept(interpreter.getExpressionEvaluator()))) {
                try {
                    node.getBody().accept(this);
                } catch (BreakException e) {
                    if (e.getLabel() == null) break;
                    throw e;
                } catch (ContinueException e) {
                    if (e.getLabel() == null) {
                        if (node.getUpdate() != null) {
                            node.getUpdate().accept(interpreter.getExpressionEvaluator());
                        }
                        continue;
                    }
                    throw e;
                }
                
                if (node.getUpdate() != null) {
                    node.getUpdate().accept(interpreter.getExpressionEvaluator());
                }
            }
        } finally {
            interpreter.setCurrentEnv(previous);
        }
        
        return null;
    }
    
    @Override
    public Object visitForEachStatement(ForEachStatement node) {
        Object iterable = node.getIterable().accept(interpreter.getExpressionEvaluator());
        
        if (iterable instanceof Object[]) {
            executeForEach((Object[]) iterable, node);
        } else if (iterable instanceof int[]) {
            executeForEachPrimitive((int[]) iterable, node);
        } else if (iterable instanceof long[]) {
            executeForEachPrimitive((long[]) iterable, node);
        } else if (iterable instanceof double[]) {
            executeForEachPrimitive((double[]) iterable, node);
        } else if (iterable instanceof float[]) {
            executeForEachPrimitive((float[]) iterable, node);
        } else if (iterable instanceof boolean[]) {
            executeForEachPrimitive((boolean[]) iterable, node);
        } else if (iterable instanceof char[]) {
            executeForEachPrimitive((char[]) iterable, node);
        } else if (iterable instanceof short[]) {
            executeForEachPrimitive((short[]) iterable, node);
        } else if (iterable instanceof byte[]) {
            executeForEachPrimitive((byte[]) iterable, node);
        } else if (iterable instanceof Iterable) {
            executeForEach((Iterable<?>) iterable, node);
        }
        
        return null;
    }
    
    private void executeForEach(Object[] array, ForEachStatement node) {
        for (Object element : array) {
            if (!executeForEachBody(element, node)) break;
        }
    }
    
    private void executeForEach(Iterable<?> iterable, ForEachStatement node) {
        for (Object element : iterable) {
            if (!executeForEachBody(element, node)) break;
        }
    }
    
    private void executeForEachPrimitive(int[] array, ForEachStatement node) {
        for (int element : array) {
            if (!executeForEachBody(element, node)) break;
        }
    }
    
    private void executeForEachPrimitive(long[] array, ForEachStatement node) {
        for (long element : array) {
            if (!executeForEachBody(element, node)) break;
        }
    }
    
    private void executeForEachPrimitive(double[] array, ForEachStatement node) {
        for (double element : array) {
            if (!executeForEachBody(element, node)) break;
        }
    }
    
    private void executeForEachPrimitive(float[] array, ForEachStatement node) {
        for (float element : array) {
            if (!executeForEachBody(element, node)) break;
        }
    }
    
    private void executeForEachPrimitive(boolean[] array, ForEachStatement node) {
        for (boolean element : array) {
            if (!executeForEachBody(element, node)) break;
        }
    }
    
    private void executeForEachPrimitive(char[] array, ForEachStatement node) {
        for (char element : array) {
            if (!executeForEachBody(element, node)) break;
        }
    }
    
    private void executeForEachPrimitive(short[] array, ForEachStatement node) {
        for (short element : array) {
            if (!executeForEachBody(element, node)) break;
        }
    }
    
    private void executeForEachPrimitive(byte[] array, ForEachStatement node) {
        for (byte element : array) {
            if (!executeForEachBody(element, node)) break;
        }
    }
    
    private boolean executeForEachBody(Object element, ForEachStatement node) {
        Environment previous = interpreter.getCurrentEnv();
        interpreter.setCurrentEnv(interpreter.getCurrentEnv().push());
        
        try {
            LocalVariableDeclaration.VariableDeclarator declarator = 
                node.getVariable().getDeclarators().get(0);
            interpreter.getCurrentEnv().defineVariable(declarator.getName(), element);
            
            try {
                node.getBody().accept(this);
                return true;
            } catch (BreakException e) {
                if (e.getLabel() == null) return false;
                throw e;
            } catch (ContinueException e) {
                if (e.getLabel() == null) return true;
                throw e;
            }
        } finally {
            interpreter.setCurrentEnv(previous);
        }
    }
    
    @Override
    public Object visitSwitchStatement(SwitchStatement node) {
        Object value = node.getExpression().accept(interpreter.getExpressionEvaluator());
        
        SwitchStatement.SwitchCase defaultCase = null;
        SwitchStatement.SwitchCase matchedCase = null;
        
        for (SwitchStatement.SwitchCase switchCase : node.getCases()) {
            if (switchCase.getLabel().isDefault()) {
                defaultCase = switchCase;
            } else {
                for (Expression caseValue : switchCase.getLabel().getValues()) {
                    Object caseVal = caseValue.accept(interpreter.getExpressionEvaluator());
                    if (isSwitchMatch(value, caseVal)) {
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
            if (matchedCase.isArrow()) {
                try {
                    for (Statement stmt : matchedCase.getStatements()) {
                        stmt.accept(this);
                    }
                } catch (BreakException e) {
                    // ignore break for arrow syntax
                }
            } else {
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
        }
        
        return null;
    }
    
    private boolean isSwitchMatch(Object value, Object caseVal) {
        if (value == null && caseVal == null) {
            return true;
        }
        if (value == null || caseVal == null) {
            return false;
        }
        
        if (value instanceof String && caseVal instanceof String) {
            return value.equals(caseVal);
        }
        
        if (value instanceof EnumConstant && caseVal instanceof EnumConstant) {
            return ((EnumConstant) value).getName().equals(((EnumConstant) caseVal).getName());
        }
        
        if (value instanceof Number && caseVal instanceof Number) {
            return compareNumbers((Number) value, (Number) caseVal) == 0;
        }
        
        if (value instanceof Character && caseVal instanceof Character) {
            return value.equals(caseVal);
        }
        
        return Objects.equals(value, caseVal);
    }
    
    private int compareNumbers(Number a, Number b) {
        if (a instanceof Double || b instanceof Double) {
            return Double.compare(a.doubleValue(), b.doubleValue());
        }
        if (a instanceof Float || b instanceof Float) {
            return Float.compare(a.floatValue(), b.floatValue());
        }
        if (a instanceof Long || b instanceof Long) {
            return Long.compare(a.longValue(), b.longValue());
        }
        return Integer.compare(a.intValue(), b.intValue());
    }
    
    @Override
    public Object visitCaseLabel(CaseLabel node) {
        return null;
    }
    
    @Override
    public Object visitReturnStatement(ReturnStatement node) {
        Object value = null;
        if (node.getExpression() != null) {
            value = node.getExpression().accept(interpreter.getExpressionEvaluator());
        }
        throw new ReturnException(value);
    }
    
    @Override
    public Object visitThrowStatement(ThrowStatement node) {
        Object exception = node.getExpression().accept(interpreter.getExpressionEvaluator());
        if (exception instanceof Throwable) {
            InterpreterException ex = interpreter.createException(((Throwable) exception).getMessage(), (Throwable) exception);
            throw ex;
        }
        throw interpreter.createException("Thrown object is not a Throwable");
    }
    
    @Override
    public Object visitTryStatement(TryStatement node) {
        List<Object> resources = new ArrayList<>();
        List<TryStatement.ResourceDeclaration> resourceDecls = node.getResources();
        Throwable primaryException = null;
        
        try {
            for (TryStatement.ResourceDeclaration resource : resourceDecls) {
                Object res = resource.getExpression().accept(interpreter.getExpressionEvaluator());
                resources.add(res);
                interpreter.getCurrentEnv().defineVariable(resource.getName(), res);
            }
            
            node.getTryBlock().accept(this);
        } catch (ReturnException | BreakException | ContinueException e) {
            throw e;
        } catch (InterpreterException e) {
            primaryException = e.getCause() != null ? e.getCause() : e;
            boolean caught = false;
            
            for (CatchClause catchClause : node.getCatchClauses()) {
                if (matchesException(e, catchClause.getExceptionTypes())) {
                    Environment previous = interpreter.getCurrentEnv();
                    interpreter.setCurrentEnv(interpreter.getCurrentEnv().push());
                    
                    try {
                        Throwable actualException = e.getCause() != null ? e.getCause() : e;
                        interpreter.getCurrentEnv().defineVariable(catchClause.getExceptionName(), actualException);
                        catchClause.getBody().accept(this);
                        caught = true;
                        primaryException = null;
                        break;
                    } finally {
                        interpreter.setCurrentEnv(previous);
                    }
                }
            }
            
            if (!caught) {
                throw e;
            }
        } catch (RuntimeException e) {
            primaryException = e.getCause() != null ? e.getCause() : e;
            boolean caught = false;
            
            for (CatchClause catchClause : node.getCatchClauses()) {
                if (matchesException(e, catchClause.getExceptionTypes())) {
                    Environment previous = interpreter.getCurrentEnv();
                    interpreter.setCurrentEnv(interpreter.getCurrentEnv().push());
                    
                    try {
                        Throwable actualException = e.getCause() != null ? e.getCause() : e;
                        interpreter.getCurrentEnv().defineVariable(catchClause.getExceptionName(), actualException);
                        catchClause.getBody().accept(this);
                        caught = true;
                        primaryException = null;
                        break;
                    } finally {
                        interpreter.setCurrentEnv(previous);
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
                        interpreter.invokeMethod(runtimeObj, closeMethod, new ArrayList<>());
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
        Throwable actualException = e;
        if (e instanceof InterpreterException) {
            actualException = e.getCause() != null ? e.getCause() : e;
        }
        
        for (Type type : exceptionTypes) {
            String typeName = type.getName();
            
            ScriptClass catchClass = interpreter.getGlobalEnv().getClass(typeName);
            if (catchClass == null) {
                catchClass = interpreter.getStdLib().getStandardClass(typeName);
            }
            
            if (actualException instanceof RuntimeException) {
                RuntimeException runtimeEx = (RuntimeException) actualException;
                if (runtimeEx.getClass().getSimpleName().equals(typeName) ||
                    runtimeEx.getClass().getName().equals(typeName)) {
                    return true;
                }
                
                if (isExceptionTypeMatch(runtimeEx.getClass(), typeName)) {
                    return true;
                }
            }
            
            if (actualException instanceof InterpreterException) {
                InterpreterException interpEx = (InterpreterException) actualException;
                if (interpEx.getExceptionClass() != null && catchClass != null) {
                    if (catchClass.isAssignableFrom(interpEx.getExceptionClass())) {
                        return true;
                    }
                }
            }
            
            if (actualException instanceof Throwable) {
                if (isExceptionTypeMatch(actualException.getClass(), typeName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isExceptionTypeMatch(Class<?> exceptionClass, String typeName) {
        if (ExceptionConstants.isAssignableFrom(typeName, exceptionClass)) {
            return true;
        }
        
        try {
            Class<?> catchType = Class.forName(typeName.contains(".") ? typeName : "java.lang." + typeName);
            return catchType.isAssignableFrom(exceptionClass);
        } catch (ClassNotFoundException ex) {
            return exceptionClass.getSimpleName().equals(typeName) ||
                   exceptionClass.getName().contains(typeName);
        }
    }
    
    @Override
    public Object visitCatchClause(CatchClause node) {
        return null;
    }
    
    @Override
    public Object visitSynchronizedStatement(SynchronizedStatement node) {
        Object lock = node.getLock().accept(interpreter.getExpressionEvaluator());
        synchronized (lock != null ? lock : new Object()) {
            node.getBody().accept(this);
        }
        return null;
    }
    
    @Override
    public Object visitAssertStatement(AssertStatement node) {
        Object condition = node.getCondition().accept(interpreter.getExpressionEvaluator());
        if (!interpreter.toBoolean(condition)) {
            String message = "";
            if (node.getMessage() != null) {
                message = String.valueOf(node.getMessage().accept(interpreter.getExpressionEvaluator()));
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
    public Object visitYieldStatement(YieldStatement node) {
        if (!inSwitchExpression) {
            throw new RuntimeException("yield statement is only allowed inside a switch expression");
        }
        Object value = node.getValue().accept(interpreter.getExpressionEvaluator());
        throw new YieldException(value);
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
        return node.getExpression().accept(interpreter.getExpressionEvaluator());
    }
    
    @Override
    public Object visitEmptyStatement(EmptyStatement node) {
        return null;
    }
    
    @Override
    public Object visitLocalClassDeclarationStatement(LocalClassDeclarationStatement node) {
        return node.getTypeDeclaration().accept(interpreter.getDeclarationExecutor());
    }
    
    @Override
    public Object visitLocalVariableDeclaration(LocalVariableDeclaration node) {
        for (LocalVariableDeclaration.VariableDeclarator declarator : node.getDeclarators()) {
            Object value = null;
            if (declarator.getInitializer() != null) {
                value = declarator.getInitializer().accept(interpreter.getExpressionEvaluator());
                
                if (value instanceof Object[] && node.getType() != null) {
                    String typeName = node.getType().getName();
                    int arrayDims = node.getType().getArrayDimensions();
                    
                    if (arrayDims > 0) {
                        Object[] objArr = (Object[]) value;
                        
                        switch (typeName) {
                            case "int":
                                int[] intArr = new int[objArr.length];
                                for (int i = 0; i < objArr.length; i++) {
                                    intArr[i] = objArr[i] != null ? ((Number) objArr[i]).intValue() : 0;
                                }
                                value = intArr;
                                break;
                            case "long":
                                long[] longArr = new long[objArr.length];
                                for (int i = 0; i < objArr.length; i++) {
                                    longArr[i] = objArr[i] != null ? ((Number) objArr[i]).longValue() : 0L;
                                }
                                value = longArr;
                                break;
                            case "double":
                                double[] doubleArr = new double[objArr.length];
                                for (int i = 0; i < objArr.length; i++) {
                                    doubleArr[i] = objArr[i] != null ? ((Number) objArr[i]).doubleValue() : 0.0;
                                }
                                value = doubleArr;
                                break;
                            case "float":
                                float[] floatArr = new float[objArr.length];
                                for (int i = 0; i < objArr.length; i++) {
                                    floatArr[i] = objArr[i] != null ? ((Number) objArr[i]).floatValue() : 0.0f;
                                }
                                value = floatArr;
                                break;
                            case "boolean":
                                boolean[] boolArr = new boolean[objArr.length];
                                for (int i = 0; i < objArr.length; i++) {
                                    boolArr[i] = objArr[i] != null && (Boolean) objArr[i];
                                }
                                value = boolArr;
                                break;
                            case "char":
                                char[] charArr = new char[objArr.length];
                                for (int i = 0; i < objArr.length; i++) {
                                    charArr[i] = objArr[i] != null ? (Character) objArr[i] : '\0';
                                }
                                value = charArr;
                                break;
                            case "byte":
                                byte[] byteArr = new byte[objArr.length];
                                for (int i = 0; i < objArr.length; i++) {
                                    byteArr[i] = objArr[i] != null ? ((Number) objArr[i]).byteValue() : 0;
                                }
                                value = byteArr;
                                break;
                            case "short":
                                short[] shortArr = new short[objArr.length];
                                for (int i = 0; i < objArr.length; i++) {
                                    shortArr[i] = objArr[i] != null ? ((Number) objArr[i]).shortValue() : 0;
                                }
                                value = shortArr;
                                break;
                        }
                    }
                }
            }
            interpreter.getCurrentEnv().defineVariable(declarator.getName(), value);
        }
        return null;
    }
    
    @Override
    public Object visitEnumConstant(EnumConstant node) {
        return null;
    }
}
