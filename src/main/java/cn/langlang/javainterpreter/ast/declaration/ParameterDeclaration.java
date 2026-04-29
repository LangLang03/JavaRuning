package cn.langlang.javainterpreter.ast.declaration;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.ast.misc.Annotation;
import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class ParameterDeclaration extends ASTNode {
    private final int modifiers;
    private final Type type;
    private final String name;
    private final boolean isVarArgs;
    private final List<Annotation> annotations;
    
    public ParameterDeclaration(Token token, int modifiers, Type type, String name,
                               boolean isVarArgs, List<Annotation> annotations) {
        super(token);
        this.modifiers = modifiers;
        this.type = type;
        this.name = name;
        this.isVarArgs = isVarArgs;
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }
    
    public int getModifiers() { return modifiers; }
    public Type getType() { return type; }
    public String getName() { return name; }
    public boolean isVarArgs() { return isVarArgs; }
    public List<Annotation> getAnnotations() { return annotations; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitParameterDeclaration(this);
    }
}
