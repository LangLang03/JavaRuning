package cn.langlang.javainterpreter.ast.expression;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.lexer.Token;

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
