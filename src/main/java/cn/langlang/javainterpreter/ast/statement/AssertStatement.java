package cn.langlang.javainterpreter.ast.statement;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.expression.Expression;
import cn.langlang.javainterpreter.lexer.Token;

public class AssertStatement extends Statement {
    private final Expression condition;
    private final Expression message;
    
    public AssertStatement(Token token, Expression condition, Expression message) {
        super(token);
        this.condition = condition;
        this.message = message;
    }
    
    public Expression getCondition() { return condition; }
    public Expression getMessage() { return message; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitAssertStatement(this);
    }
}
