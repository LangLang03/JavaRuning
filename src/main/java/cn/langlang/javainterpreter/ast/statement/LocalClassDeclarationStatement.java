package cn.langlang.javainterpreter.ast.statement;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.declaration.ClassDeclaration;
import cn.langlang.javainterpreter.lexer.Token;

public class LocalClassDeclarationStatement extends Statement {
    private final ClassDeclaration classDeclaration;
    
    public LocalClassDeclarationStatement(Token token, ClassDeclaration classDeclaration) {
        super(token);
        this.classDeclaration = classDeclaration;
    }
    
    public ClassDeclaration getClassDeclaration() { return classDeclaration; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitLocalClassDeclarationStatement(this);
    }
}
