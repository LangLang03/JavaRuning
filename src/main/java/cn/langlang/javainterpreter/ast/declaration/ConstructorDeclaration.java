package cn.langlang.javainterpreter.ast.declaration;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.ast.type.TypeParameter;
import cn.langlang.javainterpreter.ast.statement.BlockStatement;
import cn.langlang.javainterpreter.ast.misc.Annotation;
import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class ConstructorDeclaration extends ASTNode {
    private final int modifiers;
    private final List<TypeParameter> typeParameters;
    private final String name;
    private final List<ParameterDeclaration> parameters;
    private final List<Type> exceptionTypes;
    private final BlockStatement body;
    private final List<Annotation> annotations;
    
    public ConstructorDeclaration(Token token, int modifiers, List<TypeParameter> typeParameters,
                                 String name, List<ParameterDeclaration> parameters,
                                 List<Type> exceptionTypes, BlockStatement body,
                                 List<Annotation> annotations) {
        super(token);
        this.modifiers = modifiers;
        this.typeParameters = typeParameters != null ? typeParameters : new ArrayList<>();
        this.name = name;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.exceptionTypes = exceptionTypes != null ? exceptionTypes : new ArrayList<>();
        this.body = body;
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }
    
    public int getModifiers() { return modifiers; }
    public List<TypeParameter> getTypeParameters() { return typeParameters; }
    public String getName() { return name; }
    public List<ParameterDeclaration> getParameters() { return parameters; }
    public List<Type> getExceptionTypes() { return exceptionTypes; }
    public BlockStatement getBody() { return body; }
    public List<Annotation> getAnnotations() { return annotations; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitConstructorDeclaration(this);
    }
}
