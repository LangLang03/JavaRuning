package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class IfStatement extends Statement {
    private final Expression condition;
    private final Statement thenStatement;
    private final Statement elseStatement;
    
    public IfStatement(Token token, Expression condition, Statement thenStatement, Statement elseStatement) {
        super(token);
        this.condition = condition;
        this.thenStatement = thenStatement;
        this.elseStatement = elseStatement;
    }
    
    public Expression getCondition() { return condition; }
    public Statement getThenStatement() { return thenStatement; }
    public Statement getElseStatement() { return elseStatement; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitIfStatement(this);
    }
}
