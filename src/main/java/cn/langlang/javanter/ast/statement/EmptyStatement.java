package cn.langlang.javanter.ast.statement;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.lexer.Token;

public class EmptyStatement extends Statement {
    public EmptyStatement(Token token) {
        super(token);
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitEmptyStatement(this);
    }
}
