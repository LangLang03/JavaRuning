package cn.langlang.javanter.ast.expression;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.lexer.Token;

public class IdentifierExpression extends Expression {
    private final String name;
    
    public IdentifierExpression(Token token, String name) {
        super(token);
        this.name = name;
    }
    
    public String getName() { return name; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitIdentifierExpression(this);
    }
}
