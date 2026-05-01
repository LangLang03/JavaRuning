package cn.langlang.javanter.ast.statement;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.declaration.ClassDeclaration;
import cn.langlang.javanter.ast.declaration.InterfaceDeclaration;
import cn.langlang.javanter.ast.declaration.RecordDeclaration;
import cn.langlang.javanter.ast.declaration.EnumDeclaration;
import cn.langlang.javanter.ast.declaration.TypeDeclaration;
import cn.langlang.javanter.lexer.Token;

public class LocalClassDeclarationStatement extends Statement {
    private final TypeDeclaration typeDeclaration;
    
    public LocalClassDeclarationStatement(Token token, TypeDeclaration typeDeclaration) {
        super(token);
        this.typeDeclaration = typeDeclaration;
    }
    
    public TypeDeclaration getTypeDeclaration() { return typeDeclaration; }
    public ClassDeclaration getClassDeclaration() { 
        return typeDeclaration instanceof ClassDeclaration ? (ClassDeclaration) typeDeclaration : null; 
    }
    public InterfaceDeclaration getInterfaceDeclaration() { 
        return typeDeclaration instanceof InterfaceDeclaration ? (InterfaceDeclaration) typeDeclaration : null; 
    }
    public RecordDeclaration getRecordDeclaration() { 
        return typeDeclaration instanceof RecordDeclaration ? (RecordDeclaration) typeDeclaration : null; 
    }
    public EnumDeclaration getEnumDeclaration() { 
        return typeDeclaration instanceof EnumDeclaration ? (EnumDeclaration) typeDeclaration : null; 
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitLocalClassDeclarationStatement(this);
    }
}
