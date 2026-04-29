package cn.langlang.javanter.ast.expression;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.lexer.Token;

public class ThisExpression extends Expression {
    private final String className;
    
    public ThisExpression(Token token, String className) {
        super(token);
        this.className = className;
    }
    
    public String getClassName() { return className; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitThisExpression(this);
    }
}
