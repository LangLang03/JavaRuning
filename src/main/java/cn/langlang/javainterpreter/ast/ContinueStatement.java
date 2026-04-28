package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class ContinueStatement extends Statement {
    private final String label;
    
    public ContinueStatement(Token token, String label) {
        super(token);
        this.label = label;
    }
    
    public String getLabel() { return label; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitContinueStatement(this);
    }
}
