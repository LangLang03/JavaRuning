package cn.langlang.javanter.ast.expression;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.lexer.Token;

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
