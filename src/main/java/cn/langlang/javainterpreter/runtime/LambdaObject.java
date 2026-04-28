package cn.langlang.javainterpreter.runtime;

import cn.langlang.javainterpreter.ast.*;
import java.util.*;

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
