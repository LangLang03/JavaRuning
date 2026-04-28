package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class MethodReferenceExpression extends Expression {
    private final Expression target;
    private final List<TypeArgument> typeArguments;
    private final String methodName;
    
    public MethodReferenceExpression(Token token, Expression target,
                                    List<TypeArgument> typeArguments, String methodName) {
        super(token);
        this.target = target;
        this.typeArguments = typeArguments != null ? typeArguments : new ArrayList<>();
        this.methodName = methodName;
    }
    
    public Expression getTarget() { return target; }
    public List<TypeArgument> getTypeArguments() { return typeArguments; }
    public String getMethodName() { return methodName; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitMethodReferenceExpression(this);
    }
}
