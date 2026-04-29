package cn.langlang.javanter.ast.declaration;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.lexer.Token;

public class PackageDeclaration extends ASTNode {
    private final String name;
    
    public PackageDeclaration(Token token, String name) {
        super(token);
        this.name = name;
    }
    
    public String getName() { return name; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitPackageDeclaration(this);
    }
}
