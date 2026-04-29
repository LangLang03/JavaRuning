package cn.langlang.javainterpreter.ast.type;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.misc.Annotation;
import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class TypeParameter extends ASTNode {
    private final String name;
    private final List<Type> bounds;
    private final List<Annotation> annotations;
    
    public TypeParameter(Token token, String name, List<Type> bounds, List<Annotation> annotations) {
        super(token);
        this.name = name;
        this.bounds = bounds != null ? bounds : new ArrayList<>();
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }
    
    public String getName() { return name; }
    public List<Type> getBounds() { return bounds; }
    public List<Annotation> getAnnotations() { return annotations; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitTypeParameter(this);
    }
}
