package cn.langlang.javanter.ast.declaration;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.type.TypeParameter;
import cn.langlang.javanter.ast.misc.Annotation;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class InterfaceDeclaration extends TypeDeclaration {
    private final List<Type> extendsInterfaces;
    private final List<MethodDeclaration> methods;
    private final List<FieldDeclaration> constants;
    private final List<TypeDeclaration> nestedTypes;
    
    public InterfaceDeclaration(Token token, String name, int modifiers, List<Annotation> annotations,
                               List<TypeParameter> typeParameters, List<Type> extendsInterfaces,
                               List<MethodDeclaration> methods, List<FieldDeclaration> constants,
                               List<TypeDeclaration> nestedTypes) {
        super(token, name, modifiers, annotations, typeParameters);
        this.extendsInterfaces = extendsInterfaces != null ? extendsInterfaces : new ArrayList<>();
        this.methods = methods != null ? methods : new ArrayList<>();
        this.constants = constants != null ? constants : new ArrayList<>();
        this.nestedTypes = nestedTypes != null ? nestedTypes : new ArrayList<>();
    }
    
    public List<Type> getExtendsInterfaces() { return extendsInterfaces; }
    public List<MethodDeclaration> getMethods() { return methods; }
    public List<FieldDeclaration> getConstants() { return constants; }
    public List<TypeDeclaration> getNestedTypes() { return nestedTypes; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitInterfaceDeclaration(this);
    }
}
