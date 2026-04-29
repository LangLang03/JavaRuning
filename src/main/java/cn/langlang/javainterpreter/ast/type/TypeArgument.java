package cn.langlang.javainterpreter.ast.type;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.misc.Annotation;
import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class TypeArgument extends ASTNode {
    private final Type type;
    private final WildcardKind wildcardKind;
    private final Type boundType;
    private final List<Annotation> annotations;
    
    public TypeArgument(Token token, Type type, WildcardKind wildcardKind, 
                       Type boundType, List<Annotation> annotations) {
        super(token);
        this.type = type;
        this.wildcardKind = wildcardKind;
        this.boundType = boundType;
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }
    
    public Type getType() { return type; }
    public WildcardKind getWildcardKind() { return wildcardKind; }
    public Type getBoundType() { return boundType; }
    public List<Annotation> getAnnotations() { return annotations; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitTypeArgument(this);
    }
    
    public enum WildcardKind {
        NONE,
        UNBOUNDED,
        EXTENDS,
        SUPER
    }
}
