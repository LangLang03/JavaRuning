package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class ClassLiteralExpression extends Expression {
    private final Type type;
    
    public ClassLiteralExpression(Token token, Type type) {
        super(token);
        this.type = type;
    }
    
    public Type getType() { return type; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitClassLiteralExpression(this);
    }
}
