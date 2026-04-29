package cn.langlang.javainterpreter.ast.statement;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.expression.Expression;
import cn.langlang.javainterpreter.lexer.Token;

public class DoStatement extends Statement {
    private final Statement body;
    private final Expression condition;
    
    public DoStatement(Token token, Statement body, Expression condition) {
        super(token);
        this.body = body;
        this.condition = condition;
    }
    
    public Statement getBody() { return body; }
    public Expression getCondition() { return condition; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitDoStatement(this);
    }
}
