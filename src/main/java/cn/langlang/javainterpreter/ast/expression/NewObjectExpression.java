package cn.langlang.javainterpreter.ast.expression;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.ast.type.TypeArgument;
import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class NewObjectExpression extends Expression {
    private final Type type;
    private final List<TypeArgument> typeArguments;
    private final List<Expression> arguments;
    private final List<ASTNode> anonymousClassBody;
    
    public NewObjectExpression(Token token, Type type, List<TypeArgument> typeArguments,
                              List<Expression> arguments, List<ASTNode> anonymousClassBody) {
        super(token);
        this.type = type;
        this.typeArguments = typeArguments != null ? typeArguments : new ArrayList<>();
        this.arguments = arguments != null ? arguments : new ArrayList<>();
        this.anonymousClassBody = anonymousClassBody;
    }
    
    public Type getType() { return type; }
    public List<TypeArgument> getTypeArguments() { return typeArguments; }
    public List<Expression> getArguments() { return arguments; }
    public List<ASTNode> getAnonymousClassBody() { return anonymousClassBody; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitNewObjectExpression(this);
    }
}
