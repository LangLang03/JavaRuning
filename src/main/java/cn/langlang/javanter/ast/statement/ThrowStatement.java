package cn.langlang.javanter.ast.statement;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.lexer.Token;

public class ThrowStatement extends Statement {
    private final Expression expression;
    
    public ThrowStatement(Token token, Expression expression) {
        super(token);
        this.expression = expression;
    }
    
    public Expression getExpression() { return expression; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitThrowStatement(this);
    }
}
