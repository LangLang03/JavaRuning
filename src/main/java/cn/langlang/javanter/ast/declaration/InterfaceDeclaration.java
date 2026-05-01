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
    private final boolean isSealed;
    private final boolean isNonSealed;
    private final List<Type> permittedSubtypes;
    
    public InterfaceDeclaration(Token token, String name, int modifiers, List<Annotation> annotations,
                               List<TypeParameter> typeParameters, List<Type> extendsInterfaces,
                               List<MethodDeclaration> methods, List<FieldDeclaration> constants,
                               List<TypeDeclaration> nestedTypes) {
        this(token, name, modifiers, annotations, typeParameters, extendsInterfaces,
             methods, constants, nestedTypes, false, false, new ArrayList<>());
    }
    
    public InterfaceDeclaration(Token token, String name, int modifiers, List<Annotation> annotations,
                               List<TypeParameter> typeParameters, List<Type> extendsInterfaces,
                               List<MethodDeclaration> methods, List<FieldDeclaration> constants,
                               List<TypeDeclaration> nestedTypes, boolean isSealed, boolean isNonSealed,
                               List<Type> permittedSubtypes) {
        super(token, name, modifiers, annotations, typeParameters);
        this.extendsInterfaces = extendsInterfaces != null ? extendsInterfaces : new ArrayList<>();
        this.methods = methods != null ? methods : new ArrayList<>();
        this.constants = constants != null ? constants : new ArrayList<>();
        this.nestedTypes = nestedTypes != null ? nestedTypes : new ArrayList<>();
        this.isSealed = isSealed;
        this.isNonSealed = isNonSealed;
        this.permittedSubtypes = permittedSubtypes != null ? permittedSubtypes : new ArrayList<>();
    }
    
    public List<Type> getExtendsInterfaces() { return extendsInterfaces; }
    public List<MethodDeclaration> getMethods() { return methods; }
    public List<FieldDeclaration> getConstants() { return constants; }
    public List<TypeDeclaration> getNestedTypes() { return nestedTypes; }
    public boolean isSealed() { return isSealed; }
    public boolean isNonSealed() { return isNonSealed; }
    public List<Type> getPermittedSubtypes() { return permittedSubtypes; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitInterfaceDeclaration(this);
    }
}
