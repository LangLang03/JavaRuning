package cn.langlang.javainterpreter.runtime.model;

import cn.langlang.javainterpreter.ast.expression.LambdaExpression;
import cn.langlang.javainterpreter.interpreter.ExecutionContext;
import cn.langlang.javainterpreter.runtime.environment.Environment;
import java.util.List;

public class LambdaObject implements Callable {
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
    
    @Override
    public Object call(ExecutionContext context, Object target, List<Object> arguments) {
        LambdaExpression lambdaExpr = this.lambda;
        Environment closureEnv = this.closureEnv;
        ScriptClass closureClass = this.closureClass;
        Environment previous = context.getCurrentEnv();
        context.setCurrentEnv(new Environment(closureEnv != null ? closureEnv : context.getGlobalEnv()));
        
        if (closureClass != null) {
            context.getCurrentEnv().setCurrentClass(closureClass);
        }
        
        try {
            List<LambdaExpression.LambdaParameter> params = lambdaExpr.getParameters();
            for (int i = 0; i < arguments.size() && i < params.size(); i++) {
                LambdaExpression.LambdaParameter param = params.get(i);
                context.getCurrentEnv().defineVariable(param.getName(), arguments.get(i));
            }
            
            cn.langlang.javainterpreter.ast.base.ASTNode body = lambdaExpr.getBody();
            if (body instanceof cn.langlang.javainterpreter.ast.expression.Expression) {
                return context.evaluateExpression((cn.langlang.javainterpreter.ast.expression.Expression) body);
            } else if (body instanceof cn.langlang.javainterpreter.ast.statement.BlockStatement) {
                try {
                    context.executeStatement((cn.langlang.javainterpreter.ast.statement.BlockStatement) body);
                    return null;
                } catch (cn.langlang.javainterpreter.interpreter.exception.ReturnException e) {
                    return e.getValue();
                }
            }
        } finally {
            context.setCurrentEnv(previous);
        }
        
        return null;
    }
}
