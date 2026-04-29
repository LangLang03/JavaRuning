package cn.langlang.javainterpreter.ast.statement;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.expression.Expression;
import cn.langlang.javainterpreter.lexer.Token;

public class ExpressionStatement extends Statement {
    private final Expression expression;
    
    public ExpressionStatement(Token token, Expression expression) {
        super(token);
        this.expression = expression;
    }
    
    public Expression getExpression() { return expression; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitExpressionStatement(this);
    }
}
