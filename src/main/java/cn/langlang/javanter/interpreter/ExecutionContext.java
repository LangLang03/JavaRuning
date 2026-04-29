package cn.langlang.javanter.interpreter;

import cn.langlang.javanter.runtime.environment.Environment;
import cn.langlang.javanter.runtime.model.ScriptClass;
import cn.langlang.javanter.runtime.model.ScriptMethod;
import java.util.List;

public interface ExecutionContext {
    Environment getCurrentEnv();
    void setCurrentEnv(Environment env);
    Environment getGlobalEnv();
    ScriptClass resolveClass(cn.langlang.javanter.ast.type.Type type);
    Object invokeMethod(Object target, ScriptMethod method, List<Object> args);
    Object evaluateExpression(cn.langlang.javanter.ast.expression.Expression expr);
    Object executeStatement(cn.langlang.javanter.ast.statement.Statement stmt);
    void pushCallStack(String className, String methodName, int lineNumber);
    void popCallStack();
    cn.langlang.javanter.interpreter.exception.InterpreterException createException(String message);
    cn.langlang.javanter.interpreter.exception.InterpreterException createException(String message, Throwable cause);
}
