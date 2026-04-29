package cn.langlang.javanter.ast.statement;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.lexer.Token;

public class ForStatement extends Statement {
    private final Statement init;
    private final Expression condition;
    private final Expression update;
    private final Statement body;
    
    public ForStatement(Token token, Statement init, Expression condition, 
                       Expression update, Statement body) {
        super(token);
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }
    
    public Statement getInit() { return init; }
    public Expression getCondition() { return condition; }
    public Expression getUpdate() { return update; }
    public Statement getBody() { return body; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitForStatement(this);
    }
}
