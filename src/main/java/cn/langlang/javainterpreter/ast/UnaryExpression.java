package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;
import cn.langlang.javainterpreter.lexer.TokenType;

public class UnaryExpression extends Expression {
    private final TokenType operator;
    private final Expression operand;
    private final boolean isPrefix;
    
    public UnaryExpression(Token token, TokenType operator, Expression operand, boolean isPrefix) {
        super(token);
        this.operator = operator;
        this.operand = operand;
        this.isPrefix = isPrefix;
    }
    
    public TokenType getOperator() { return operator; }
    public Expression getOperand() { return operand; }
    public boolean isPrefix() { return isPrefix; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitUnaryExpression(this);
    }
}
