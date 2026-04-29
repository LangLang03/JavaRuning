package cn.langlang.javainterpreter.interpreter;

import cn.langlang.javainterpreter.runtime.environment.Environment;
import cn.langlang.javainterpreter.runtime.model.ScriptClass;
import cn.langlang.javainterpreter.runtime.model.ScriptMethod;
import java.util.List;

public interface ExecutionContext {
    Environment getCurrentEnv();
    void setCurrentEnv(Environment env);
    Environment getGlobalEnv();
    ScriptClass resolveClass(cn.langlang.javainterpreter.ast.type.Type type);
    Object invokeMethod(Object target, ScriptMethod method, List<Object> args);
    Object evaluateExpression(cn.langlang.javainterpreter.ast.expression.Expression expr);
    Object executeStatement(cn.langlang.javainterpreter.ast.statement.Statement stmt);
    void pushCallStack(String className, String methodName, int lineNumber);
    void popCallStack();
    cn.langlang.javainterpreter.interpreter.exception.InterpreterException createException(String message);
    cn.langlang.javainterpreter.interpreter.exception.InterpreterException createException(String message, Throwable cause);
}
