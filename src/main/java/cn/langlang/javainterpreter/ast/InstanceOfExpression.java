package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class InstanceOfExpression extends Expression {
    private final Expression expression;
    private final Type type;
    
    public InstanceOfExpression(Token token, Expression expression, Type type) {
        super(token);
        this.expression = expression;
        this.type = type;
    }
    
    public Expression getExpression() { return expression; }
    public Type getType() { return type; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitInstanceOfExpression(this);
    }
}
