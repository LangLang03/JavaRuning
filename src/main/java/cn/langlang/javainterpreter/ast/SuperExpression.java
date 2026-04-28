package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class SuperExpression extends Expression {
    private final String className;
    
    public SuperExpression(Token token, String className) {
        super(token);
        this.className = className;
    }
    
    public String getClassName() { return className; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitSuperExpression(this);
    }
}
