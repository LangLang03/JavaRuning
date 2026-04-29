package cn.langlang.javainterpreter.ast.expression;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.lexer.Token;

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
