package cn.langlang.javainterpreter.runtime.model;

import cn.langlang.javainterpreter.ast.expression.MethodReferenceExpression;
import cn.langlang.javainterpreter.runtime.environment.Environment;

public class MethodReferenceObject {
    private final MethodReferenceExpression methodRef;
    private final Environment closureEnv;
    
    public MethodReferenceObject(MethodReferenceExpression methodRef, Environment closureEnv) {
        this.methodRef = methodRef;
        this.closureEnv = closureEnv;
    }
    
    public MethodReferenceExpression getMethodRef() { return methodRef; }
    public Environment getClosureEnv() { return closureEnv; }
}
