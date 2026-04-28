package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class EmptyStatement extends Statement {
    public EmptyStatement(Token token) {
        super(token);
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitEmptyStatement(this);
    }
}
