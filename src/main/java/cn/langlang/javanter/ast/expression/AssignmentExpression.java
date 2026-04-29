package cn.langlang.javanter.ast.expression;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.lexer.Token;
import cn.langlang.javanter.lexer.TokenType;

public class AssignmentExpression extends Expression {
    private final Expression target;
    private final TokenType operator;
    private final Expression value;
    
    public AssignmentExpression(Token token, Expression target, TokenType operator, Expression value) {
        super(token);
        this.target = target;
        this.operator = operator;
        this.value = value;
    }
    
    public Expression getTarget() { return target; }
    public TokenType getOperator() { return operator; }
    public Expression getValue() { return value; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitAssignmentExpression(this);
    }
}
