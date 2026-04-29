package cn.langlang.javanter.ast.declaration;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.ast.misc.Annotation;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class FieldDeclaration extends ASTNode {
    private final int modifiers;
    private final Type type;
    private final String name;
    private final Expression initializer;
    private final List<Annotation> annotations;
    
    public FieldDeclaration(Token token, int modifiers, Type type, String name,
                           Expression initializer, List<Annotation> annotations) {
        super(token);
        this.modifiers = modifiers;
        this.type = type;
        this.name = name;
        this.initializer = initializer;
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }
    
    public int getModifiers() { return modifiers; }
    public Type getType() { return type; }
    public String getName() { return name; }
    public Expression getInitializer() { return initializer; }
    public List<Annotation> getAnnotations() { return annotations; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitFieldDeclaration(this);
    }
}
