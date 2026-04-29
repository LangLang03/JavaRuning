package cn.langlang.javanter.ast.statement;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.lexer.Token;

public class SynchronizedStatement extends Statement {
    private final Expression lock;
    private final BlockStatement body;
    
    public SynchronizedStatement(Token token, Expression lock, BlockStatement body) {
        super(token);
        this.lock = lock;
        this.body = body;
    }
    
    public Expression getLock() { return lock; }
    public BlockStatement getBody() { return body; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitSynchronizedStatement(this);
    }
}
