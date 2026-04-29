package cn.langlang.javanter.ast.declaration;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.type.TypeParameter;
import cn.langlang.javanter.ast.statement.BlockStatement;
import cn.langlang.javanter.ast.misc.Annotation;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class MethodDeclaration extends ASTNode {
    private final int modifiers;
    private final List<TypeParameter> typeParameters;
    private final Type returnType;
    private final String name;
    private final List<ParameterDeclaration> parameters;
    private final boolean isVarArgs;
    private final List<Type> exceptionTypes;
    private final BlockStatement body;
    private final List<Annotation> annotations;
    private final boolean isDefault;
    
    public MethodDeclaration(Token token, int modifiers, List<TypeParameter> typeParameters,
                            Type returnType, String name, List<ParameterDeclaration> parameters,
                            boolean isVarArgs, List<Type> exceptionTypes, BlockStatement body,
                            List<Annotation> annotations, boolean isDefault) {
        super(token);
        this.modifiers = modifiers;
        this.typeParameters = typeParameters != null ? typeParameters : new ArrayList<>();
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.isVarArgs = isVarArgs;
        this.exceptionTypes = exceptionTypes != null ? exceptionTypes : new ArrayList<>();
        this.body = body;
        this.annotations = annotations != null ? annotations : new ArrayList<>();
        this.isDefault = isDefault;
    }
    
    public int getModifiers() { return modifiers; }
    public List<TypeParameter> getTypeParameters() { return typeParameters; }
    public Type getReturnType() { return returnType; }
    public String getName() { return name; }
    public List<ParameterDeclaration> getParameters() { return parameters; }
    public boolean isVarArgs() { return isVarArgs; }
    public List<Type> getExceptionTypes() { return exceptionTypes; }
    public BlockStatement getBody() { return body; }
    public List<Annotation> getAnnotations() { return annotations; }
    public boolean isDefault() { return isDefault; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitMethodDeclaration(this);
    }
}
