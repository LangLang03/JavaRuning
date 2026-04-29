package cn.langlang.javainterpreter.ast.declaration;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.ast.misc.Annotation;
import cn.langlang.javainterpreter.ast.misc.EnumConstant;
import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class EnumDeclaration extends TypeDeclaration {
    private final List<Type> interfaces;
    private final List<EnumConstant> constants;
    private final List<FieldDeclaration> fields;
    private final List<MethodDeclaration> methods;
    private final List<ConstructorDeclaration> constructors;
    private final List<TypeDeclaration> nestedTypes;
    
    public EnumDeclaration(Token token, String name, int modifiers, List<Annotation> annotations,
                          List<Type> interfaces, List<EnumConstant> constants,
                          List<FieldDeclaration> fields, List<MethodDeclaration> methods,
                          List<ConstructorDeclaration> constructors, List<TypeDeclaration> nestedTypes) {
        super(token, name, modifiers, annotations, null);
        this.interfaces = interfaces != null ? interfaces : new ArrayList<>();
        this.constants = constants != null ? constants : new ArrayList<>();
        this.fields = fields != null ? fields : new ArrayList<>();
        this.methods = methods != null ? methods : new ArrayList<>();
        this.constructors = constructors != null ? constructors : new ArrayList<>();
        this.nestedTypes = nestedTypes != null ? nestedTypes : new ArrayList<>();
    }
    
    public List<Type> getInterfaces() { return interfaces; }
    public List<EnumConstant> getConstants() { return constants; }
    public List<FieldDeclaration> getFields() { return fields; }
    public List<MethodDeclaration> getMethods() { return methods; }
    public List<ConstructorDeclaration> getConstructors() { return constructors; }
    public List<TypeDeclaration> getNestedTypes() { return nestedTypes; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitEnumDeclaration(this);
    }
}
