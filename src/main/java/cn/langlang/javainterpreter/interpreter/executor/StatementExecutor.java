package cn.langlang.javainterpreter.interpreter.executor;

import cn.langlang.javainterpreter.ast.base.AbstractASTVisitor;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.statement.LocalVariableDeclaration;
import cn.langlang.javainterpreter.ast.expression.Expression;
import cn.langlang.javainterpreter.ast.misc.CatchClause;
import cn.langlang.javainterpreter.ast.misc.CaseLabel;
import cn.langlang.javainterpreter.ast.misc.EnumConstant;
import cn.langlang.javainterpreter.ast.statement.*;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.interpreter.Interpreter;
import cn.langlang.javainterpreter.interpreter.exception.*;
import cn.langlang.javainterpreter.runtime.environment.Environment;
import cn.langlang.javainterpreter.runtime.model.*;
import java.util.*;

public class StatementExecutor extends AbstractASTVisitor<Object> {
    private final Interpreter interpreter;
    
    public StatementExecutor(Interpreter interpreter) {
        this.interpreter = interpreter;
    }
    
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
            Object[] array = (Object[]) iterable;
            for (Object element : array) {
                Environment previous = interpreter.getCurrentEnv();
                interpreter.setCurrentEnv(interpreter.getCurrentEnv().push());
                
                try {
                    LocalVariableDeclaration.VariableDeclarator declarator = 
                        node.getVariable().getDeclarators().get(0);
                    interpreter.getCurrentEnv().defineVariable(declarator.getName(), element);
                    
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
                    interpreter.setCurrentEnv(previous);
                }
            }
        } else if (iterable instanceof Iterable) {
            for (Object element : (Iterable<?>) iterable) {
                Environment previous = interpreter.getCurrentEnv();
                interpreter.setCurrentEnv(interpreter.getCurrentEnv().push());
                
                try {
                    LocalVariableDeclaration.VariableDeclarator declarator = 
                        node.getVariable().getDeclarators().get(0);
                    interpreter.getCurrentEnv().defineVariable(declarator.getName(), element);
                    
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
                    interpreter.setCurrentEnv(previous);
                }
            }
        }
        
        return null;
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
        if (typeName.equals("Exception") || typeName.equals("java.lang.Exception")) {
            return Exception.class.isAssignableFrom(exceptionClass);
        }
        if (typeName.equals("RuntimeException") || typeName.equals("java.lang.RuntimeException")) {
            return RuntimeException.class.isAssignableFrom(exceptionClass);
        }
        if (typeName.equals("Throwable") || typeName.equals("java.lang.Throwable")) {
            return Throwable.class.isAssignableFrom(exceptionClass);
        }
        if (typeName.equals("IOException") || typeName.equals("java.io.IOException")) {
            return java.io.IOException.class.isAssignableFrom(exceptionClass);
        }
        if (typeName.equals("NullPointerException") || typeName.equals("java.lang.NullPointerException")) {
            return NullPointerException.class.isAssignableFrom(exceptionClass);
        }
        if (typeName.equals("ArithmeticException") || typeName.equals("java.lang.ArithmeticException")) {
            return ArithmeticException.class.isAssignableFrom(exceptionClass);
        }
        if (typeName.equals("IllegalArgumentException") || typeName.equals("java.lang.IllegalArgumentException")) {
            return IllegalArgumentException.class.isAssignableFrom(exceptionClass);
        }
        if (typeName.equals("IndexOutOfBoundsException") || typeName.equals("java.lang.IndexOutOfBoundsException")) {
            return IndexOutOfBoundsException.class.isAssignableFrom(exceptionClass);
        }
        if (typeName.equals("ClassCastException") || typeName.equals("java.lang.ClassCastException")) {
            return ClassCastException.class.isAssignableFrom(exceptionClass);
        }
        if (typeName.equals("NumberFormatException") || typeName.equals("java.lang.NumberFormatException")) {
            return NumberFormatException.class.isAssignableFrom(exceptionClass);
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
        return node.getClassDeclaration().accept(interpreter.getDeclarationExecutor());
    }
    
    @Override
    public Object visitLocalVariableDeclaration(LocalVariableDeclaration node) {
        for (LocalVariableDeclaration.VariableDeclarator declarator : node.getDeclarators()) {
            Object value = null;
            if (declarator.getInitializer() != null) {
                value = declarator.getInitializer().accept(interpreter.getExpressionEvaluator());
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
