package cn.langlang.javainterpreter.ast.declaration;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.lexer.Token;

public class ImportDeclaration extends ASTNode {
    private final String name;
    private final boolean isStatic;
    private final boolean isAsterisk;
    
    public ImportDeclaration(Token token, String name, boolean isStatic, boolean isAsterisk) {
        super(token);
        this.name = name;
        this.isStatic = isStatic;
        this.isAsterisk = isAsterisk;
    }
    
    public String getName() { return name; }
    public boolean isStatic() { return isStatic; }
    public boolean isAsterisk() { return isAsterisk; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitImportDeclaration(this);
    }
}
