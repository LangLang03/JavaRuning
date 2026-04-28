package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class WhileStatement extends Statement {
    private final Expression condition;
    private final Statement body;
    
    public WhileStatement(Token token, Expression condition, Statement body) {
        super(token);
        this.condition = condition;
        this.body = body;
    }
    
    public Expression getCondition() { return condition; }
    public Statement getBody() { return body; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitWhileStatement(this);
    }
}
