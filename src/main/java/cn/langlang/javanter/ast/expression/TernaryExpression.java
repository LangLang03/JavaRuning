package cn.langlang.javanter.ast.expression;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.lexer.Token;

public class TernaryExpression extends Expression {
    private final Expression condition;
    private final Expression trueExpression;
    private final Expression falseExpression;
    
    public TernaryExpression(Token token, Expression condition, 
                            Expression trueExpression, Expression falseExpression) {
        super(token);
        this.condition = condition;
        this.trueExpression = trueExpression;
        this.falseExpression = falseExpression;
    }
    
    public Expression getCondition() { return condition; }
    public Expression getTrueExpression() { return trueExpression; }
    public Expression getFalseExpression() { return falseExpression; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitTernaryExpression(this);
    }
}
