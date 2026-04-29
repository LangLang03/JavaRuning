package cn.langlang.javanter.ast.declaration;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.ast.misc.Annotation;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class AnnotationDeclaration extends TypeDeclaration {
    private final List<AnnotationElement> elements;
    
    public AnnotationDeclaration(Token token, String name, int modifiers, 
                                List<Annotation> annotations, List<AnnotationElement> elements) {
        super(token, name, modifiers, annotations, null);
        this.elements = elements != null ? elements : new ArrayList<>();
    }
    
    public List<AnnotationElement> getElements() { return elements; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitAnnotationDeclaration(this);
    }
    
    public static class AnnotationElement extends ASTNode {
        private final String name;
        private final Type type;
        private final Expression defaultValue;
        
        public AnnotationElement(Token token, String name, Type type, Expression defaultValue) {
            super(token);
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
        }
        
        public String getName() { return name; }
        public Type getType() { return type; }
        public Expression getDefaultValue() { return defaultValue; }
        
        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            throw new UnsupportedOperationException();
        }
    }
}
