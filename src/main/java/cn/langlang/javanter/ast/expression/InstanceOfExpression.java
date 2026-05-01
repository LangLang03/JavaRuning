package cn.langlang.javanter.ast.expression;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.lexer.Token;

public class InstanceOfExpression extends Expression {
    private final Expression expression;
    private final Type type;
    private final String patternVariable;
    private final boolean hasPattern;
    
    public InstanceOfExpression(Token token, Expression expression, Type type) {
        this(token, expression, type, null, false);
    }
    
    public InstanceOfExpression(Token token, Expression expression, Type type,
                               String patternVariable, boolean hasPattern) {
        super(token);
        this.expression = expression;
        this.type = type;
        this.patternVariable = patternVariable;
        this.hasPattern = hasPattern;
    }
    
    public Expression getExpression() { return expression; }
    public Type getType() { return type; }
    public String getPatternVariable() { return patternVariable; }
    public boolean hasPattern() { return hasPattern; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitInstanceOfExpression(this);
    }
}
