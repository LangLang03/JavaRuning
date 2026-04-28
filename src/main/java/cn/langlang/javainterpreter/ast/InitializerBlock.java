package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public class InitializerBlock extends ASTNode {
    private final boolean isStatic;
    private final BlockStatement body;
    
    public InitializerBlock(Token token, boolean isStatic, BlockStatement body) {
        super(token);
        this.isStatic = isStatic;
        this.body = body;
    }
    
    public boolean isStatic() { return isStatic; }
    public BlockStatement getBody() { return body; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitInitializerBlock(this);
    }
}
