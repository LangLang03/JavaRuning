package cn.langlang.javanter.ast.expression;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class LambdaExpression extends Expression {
    private final List<LambdaParameter> parameters;
    private final ASTNode body;
    
    public LambdaExpression(Token token, List<LambdaParameter> parameters, ASTNode body) {
        super(token);
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.body = body;
    }
    
    public List<LambdaParameter> getParameters() { return parameters; }
    public ASTNode getBody() { return body; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitLambdaExpression(this);
    }
    
    public static class LambdaParameter {
        private final Type type;
        private final String name;
        
        public LambdaParameter(Type type, String name) {
            this.type = type;
            this.name = name;
        }
        
        public Type getType() { return type; }
        public String getName() { return name; }
    }
}
