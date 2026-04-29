package cn.langlang.javainterpreter.ast.expression;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.lexer.Token;

public class CastExpression extends Expression {
    private final Type type;
    private final Expression expression;
    
    public CastExpression(Token token, Type type, Expression expression) {
        super(token);
        this.type = type;
        this.expression = expression;
    }
    
    public Type getType() { return type; }
    public Expression getExpression() { return expression; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitCastExpression(this);
    }
}
