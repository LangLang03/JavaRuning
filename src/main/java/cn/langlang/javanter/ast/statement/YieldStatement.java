package cn.langlang.javanter.ast.statement;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.lexer.Token;

public class YieldStatement extends Statement {
    private final Expression value;
    
    public YieldStatement(Token token, Expression value) {
        super(token);
        this.value = value;
    }
    
    public Expression getValue() { return value; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitYieldStatement(this);
    }
}
