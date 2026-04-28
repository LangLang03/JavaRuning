package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class BreakStatement extends Statement {
    private final String label;
    
    public BreakStatement(Token token, String label) {
        super(token);
        this.label = label;
    }
    
    public String getLabel() { return label; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitBreakStatement(this);
    }
}
