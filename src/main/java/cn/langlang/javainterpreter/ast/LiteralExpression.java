package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class LiteralExpression extends Expression {
    private final Object value;
    
    public LiteralExpression(Token token, Object value) {
        super(token);
        this.value = value;
    }
    
    public Object getValue() { return value; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitLiteralExpression(this);
    }
}
