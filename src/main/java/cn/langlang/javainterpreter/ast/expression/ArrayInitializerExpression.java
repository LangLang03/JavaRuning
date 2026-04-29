package cn.langlang.javainterpreter.ast.expression;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class ArrayInitializerExpression extends Expression {
    private final List<Expression> elements;
    
    public ArrayInitializerExpression(Token token, List<Expression> elements) {
        super(token);
        this.elements = elements != null ? elements : new ArrayList<>();
    }
    
    public List<Expression> getElements() { return elements; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitArrayInitializerExpression(this);
    }
}
