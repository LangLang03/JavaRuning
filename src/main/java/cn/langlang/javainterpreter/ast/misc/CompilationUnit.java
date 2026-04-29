package cn.langlang.javainterpreter.ast.misc;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.declaration.ImportDeclaration;
import cn.langlang.javainterpreter.ast.declaration.PackageDeclaration;
import cn.langlang.javainterpreter.ast.declaration.TypeDeclaration;
import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class CompilationUnit extends ASTNode {
    private final PackageDeclaration packageDeclaration;
    private final List<ImportDeclaration> imports;
    private final List<TypeDeclaration> typeDeclarations;
    
    public CompilationUnit(Token token, PackageDeclaration packageDeclaration,
                          List<ImportDeclaration> imports, List<TypeDeclaration> typeDeclarations) {
        super(token);
        this.packageDeclaration = packageDeclaration;
        this.imports = imports != null ? imports : new ArrayList<>();
        this.typeDeclarations = typeDeclarations != null ? typeDeclarations : new ArrayList<>();
    }
    
    public PackageDeclaration getPackageDeclaration() { return packageDeclaration; }
    public List<ImportDeclaration> getImports() { return imports; }
    public List<TypeDeclaration> getTypeDeclarations() { return typeDeclarations; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitCompilationUnit(this);
    }
}
