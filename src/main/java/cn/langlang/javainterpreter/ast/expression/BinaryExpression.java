package cn.langlang.javainterpreter.ast.expression;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.lexer.Token;
import cn.langlang.javainterpreter.lexer.TokenType;

public class BinaryExpression extends Expression {
    private final Expression left;
    private final TokenType operator;
    private final Expression right;
    
    public BinaryExpression(Token token, Expression left, TokenType operator, Expression right) {
        super(token);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
    
    public Expression getLeft() { return left; }
    public TokenType getOperator() { return operator; }
    public Expression getRight() { return right; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitBinaryExpression(this);
    }
}
