package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class ParenthesizedExpression extends Expression {
    private final Expression expression;
    
    public ParenthesizedExpression(Token token, Expression expression) {
        super(token);
        this.expression = expression;
    }
    
    public Expression getExpression() { return expression; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitParenthesizedExpression(this);
    }
}
