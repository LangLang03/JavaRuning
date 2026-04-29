package cn.langlang.javainterpreter.runtime.model;

import cn.langlang.javainterpreter.ast.expression.LambdaExpression;
import cn.langlang.javainterpreter.runtime.environment.Environment;

public class LambdaObject {
    private final LambdaExpression lambda;
    private final Environment closureEnv;
    private final ScriptClass closureClass;
    
    public LambdaObject(LambdaExpression lambda, Environment closureEnv) {
        this.lambda = lambda;
        this.closureEnv = closureEnv;
        this.closureClass = closureEnv != null ? closureEnv.getCurrentClass() : null;
    }
    
    public LambdaExpression getLambda() { return lambda; }
    public Environment getClosureEnv() { return closureEnv; }
    public ScriptClass getClosureClass() { return closureClass; }
}
