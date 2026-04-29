package cn.langlang.javainterpreter.ast.declaration;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.lexer.Token;

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
