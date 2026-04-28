package cn.langlang.javainterpreter.runtime;

import cn.langlang.javainterpreter.ast.*;

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
