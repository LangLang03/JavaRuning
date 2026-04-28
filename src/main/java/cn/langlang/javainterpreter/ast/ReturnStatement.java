package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class ReturnStatement extends Statement {
    private final Expression expression;
    
    public ReturnStatement(Token token, Expression expression) {
        super(token);
        this.expression = expression;
    }
    
    public Expression getExpression() { return expression; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitReturnStatement(this);
    }
}
