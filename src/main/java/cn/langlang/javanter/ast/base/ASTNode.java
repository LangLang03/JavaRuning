package cn.langlang.javanter.ast.base;

import cn.langlang.javanter.lexer.Token;

public abstract class ASTNode {
    private final Token token;
    
    public ASTNode(Token token) {
        this.token = token;
    }
    
    public Token getToken() { return token; }
    public int getLine() { return token.getLine(); }
    public int getColumn() { return token.getColumn(); }
    
    public abstract <R> R accept(ASTVisitor<R> visitor);
}
