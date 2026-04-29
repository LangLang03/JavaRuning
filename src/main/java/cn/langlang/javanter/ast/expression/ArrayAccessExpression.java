package cn.langlang.javanter.ast.expression;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.lexer.Token;

public class ArrayAccessExpression extends Expression {
    private final Expression array;
    private final Expression index;
    
    public ArrayAccessExpression(Token token, Expression array, Expression index) {
        super(token);
        this.array = array;
        this.index = index;
    }
    
    public Expression getArray() { return array; }
    public Expression getIndex() { return index; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitArrayAccessExpression(this);
    }
}
