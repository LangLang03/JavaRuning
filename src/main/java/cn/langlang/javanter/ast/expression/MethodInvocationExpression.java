package cn.langlang.javanter.ast.expression;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.type.TypeArgument;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class MethodInvocationExpression extends Expression {
    private final Expression target;
    private final List<TypeArgument> typeArguments;
    private final String methodName;
    private final List<Expression> arguments;
    
    public MethodInvocationExpression(Token token, Expression target, 
                                    List<TypeArgument> typeArguments, String methodName,
                                    List<Expression> arguments) {
        super(token);
        this.target = target;
        this.typeArguments = typeArguments != null ? typeArguments : new ArrayList<>();
        this.methodName = methodName;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
    }
    
    public Expression getTarget() { return target; }
    public List<TypeArgument> getTypeArguments() { return typeArguments; }
    public String getMethodName() { return methodName; }
    public List<Expression> getArguments() { return arguments; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitMethodInvocationExpression(this);
    }
}
