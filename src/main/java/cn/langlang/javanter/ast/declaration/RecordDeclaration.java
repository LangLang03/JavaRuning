package cn.langlang.javanter.ast.declaration;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.misc.Annotation;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.type.TypeParameter;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class RecordDeclaration extends TypeDeclaration {
    private final List<RecordComponent> components;
    private final List<Type> implementsInterfaces;
    private final List<MethodDeclaration> methods;
    private final List<FieldDeclaration> staticFields;
    private final List<TypeDeclaration> nestedTypes;
    
    public RecordDeclaration(Token token, String name, int modifiers,
                            List<Annotation> annotations,
                            List<TypeParameter> typeParameters,
                            List<RecordComponent> components,
                            List<Type> implementsInterfaces,
                            List<MethodDeclaration> methods,
                            List<FieldDeclaration> staticFields,
                            List<TypeDeclaration> nestedTypes) {
        super(token, name, modifiers, annotations, typeParameters);
        this.components = components != null ? components : new ArrayList<>();
        this.implementsInterfaces = implementsInterfaces != null ? implementsInterfaces : new ArrayList<>();
        this.methods = methods != null ? methods : new ArrayList<>();
        this.staticFields = staticFields != null ? staticFields : new ArrayList<>();
        this.nestedTypes = nestedTypes != null ? nestedTypes : new ArrayList<>();
    }
    
    public List<RecordComponent> getComponents() { return components; }
    public List<Type> getImplementsInterfaces() { return implementsInterfaces; }
    public List<MethodDeclaration> getMethods() { return methods; }
    public List<FieldDeclaration> getStaticFields() { return staticFields; }
    public List<TypeDeclaration> getNestedTypes() { return nestedTypes; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitRecordDeclaration(this);
    }
    
    public static class RecordComponent {
        private final List<Annotation> annotations;
        private final Type type;
        private final String name;
        private final List<Annotation> componentAnnotations;
        
        public RecordComponent(List<Annotation> annotations, Type type, String name,
                             List<Annotation> componentAnnotations) {
            this.annotations = annotations != null ? annotations : new ArrayList<>();
            this.type = type;
            this.name = name;
            this.componentAnnotations = componentAnnotations != null ? componentAnnotations : new ArrayList<>();
        }
        
        public List<Annotation> getAnnotations() { return annotations; }
        public Type getType() { return type; }
        public String getName() { return name; }
        public List<Annotation> getComponentAnnotations() { return componentAnnotations; }
    }
}
