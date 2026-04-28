package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class Type extends ASTNode {
    private final String name;
    private final List<TypeArgument> typeArguments;
    private final int arrayDimensions;
    private final List<Annotation> annotations;
    
    public Type(Token token, String name, List<TypeArgument> typeArguments, 
               int arrayDimensions, List<Annotation> annotations) {
        super(token);
        this.name = name;
        this.typeArguments = typeArguments != null ? typeArguments : new ArrayList<>();
        this.arrayDimensions = arrayDimensions;
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }
    
    public String getName() { return name; }
    public List<TypeArgument> getTypeArguments() { return typeArguments; }
    public int getArrayDimensions() { return arrayDimensions; }
    public List<Annotation> getAnnotations() { return annotations; }
    
    public boolean isArray() { return arrayDimensions > 0; }
    public boolean isParameterized() { return !typeArguments.isEmpty(); }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        throw new UnsupportedOperationException("Type does not support visitor");
    }
    
    public String getQualifiedName() {
        return name;
    }
}
