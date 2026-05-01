package cn.langlang.javanter.ast.declaration;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.type.TypeParameter;
import cn.langlang.javanter.ast.misc.Annotation;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class ClassDeclaration extends TypeDeclaration {
    private final Type superClass;
    private final List<Type> interfaces;
    private final List<FieldDeclaration> fields;
    private final List<MethodDeclaration> methods;
    private final List<ConstructorDeclaration> constructors;
    private final List<InitializerBlock> initializers;
    private final List<TypeDeclaration> nestedTypes;
    private final boolean isSealed;
    private final boolean isNonSealed;
    private final List<Type> permittedSubtypes;
    
    public ClassDeclaration(Token token, String name, int modifiers, List<Annotation> annotations,
                           List<TypeParameter> typeParameters, Type superClass, List<Type> interfaces,
                           List<FieldDeclaration> fields, List<MethodDeclaration> methods,
                           List<ConstructorDeclaration> constructors, List<InitializerBlock> initializers,
                           List<TypeDeclaration> nestedTypes) {
        this(token, name, modifiers, annotations, typeParameters, superClass, interfaces,
             fields, methods, constructors, initializers, nestedTypes, false, false, new ArrayList<>());
    }
    
    public ClassDeclaration(Token token, String name, int modifiers, List<Annotation> annotations,
                           List<TypeParameter> typeParameters, Type superClass, List<Type> interfaces,
                           List<FieldDeclaration> fields, List<MethodDeclaration> methods,
                           List<ConstructorDeclaration> constructors, List<InitializerBlock> initializers,
                           List<TypeDeclaration> nestedTypes, boolean isSealed, boolean isNonSealed,
                           List<Type> permittedSubtypes) {
        super(token, name, modifiers, annotations, typeParameters);
        this.superClass = superClass;
        this.interfaces = interfaces != null ? interfaces : new ArrayList<>();
        this.fields = fields != null ? fields : new ArrayList<>();
        this.methods = methods != null ? methods : new ArrayList<>();
        this.constructors = constructors != null ? constructors : new ArrayList<>();
        this.initializers = initializers != null ? initializers : new ArrayList<>();
        this.nestedTypes = nestedTypes != null ? nestedTypes : new ArrayList<>();
        this.isSealed = isSealed;
        this.isNonSealed = isNonSealed;
        this.permittedSubtypes = permittedSubtypes != null ? permittedSubtypes : new ArrayList<>();
    }
    
    public Type getSuperClass() { return superClass; }
    public List<Type> getInterfaces() { return interfaces; }
    public List<FieldDeclaration> getFields() { return fields; }
    public List<MethodDeclaration> getMethods() { return methods; }
    public List<ConstructorDeclaration> getConstructors() { return constructors; }
    public List<InitializerBlock> getInitializers() { return initializers; }
    public List<TypeDeclaration> getNestedTypes() { return nestedTypes; }
    public boolean isSealed() { return isSealed; }
    public boolean isNonSealed() { return isNonSealed; }
    public List<Type> getPermittedSubtypes() { return permittedSubtypes; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitClassDeclaration(this);
    }
}
