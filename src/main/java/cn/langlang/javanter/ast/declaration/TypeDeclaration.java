package cn.langlang.javanter.ast.declaration;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.type.TypeParameter;
import cn.langlang.javanter.ast.misc.Annotation;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public abstract class TypeDeclaration extends ASTNode {
    private final String name;
    private final int modifiers;
    private final List<Annotation> annotations;
    private final List<TypeParameter> typeParameters;
    
    protected TypeDeclaration(Token token, String name, int modifiers, 
                             List<Annotation> annotations, List<TypeParameter> typeParameters) {
        super(token);
        this.name = name;
        this.modifiers = modifiers;
        this.annotations = annotations != null ? annotations : new ArrayList<>();
        this.typeParameters = typeParameters != null ? typeParameters : new ArrayList<>();
    }
    
    public String getName() { return name; }
    public int getModifiers() { return modifiers; }
    public List<Annotation> getAnnotations() { return annotations; }
    public List<TypeParameter> getTypeParameters() { return typeParameters; }
}
