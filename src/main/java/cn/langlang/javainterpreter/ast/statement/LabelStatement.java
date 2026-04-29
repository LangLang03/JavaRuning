package cn.langlang.javainterpreter.ast.statement;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.lexer.Token;

public class LabelStatement extends Statement {
    private final String label;
    private final Statement statement;
    
    public LabelStatement(Token token, String label, Statement statement) {
        super(token);
        this.label = label;
        this.statement = statement;
    }
    
    public String getLabel() { return label; }
    public Statement getStatement() { return statement; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitLabelStatement(this);
    }
}
