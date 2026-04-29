package cn.langlang.javanter.ast.declaration;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.statement.BlockStatement;
import cn.langlang.javanter.lexer.Token;

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
