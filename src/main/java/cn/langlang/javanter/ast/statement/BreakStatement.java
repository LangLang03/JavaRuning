package cn.langlang.javanter.ast.statement;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.lexer.Token;

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
