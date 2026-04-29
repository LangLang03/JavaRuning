package cn.langlang.javainterpreter.ast.expression;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.lexer.Token;

public class FieldAccessExpression extends Expression {
    private final Expression target;
    private final String fieldName;
    
    public FieldAccessExpression(Token token, Expression target, String fieldName) {
        super(token);
        this.target = target;
        this.fieldName = fieldName;
    }
    
    public Expression getTarget() { return target; }
    public String getFieldName() { return fieldName; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitFieldAccessExpression(this);
    }
}
