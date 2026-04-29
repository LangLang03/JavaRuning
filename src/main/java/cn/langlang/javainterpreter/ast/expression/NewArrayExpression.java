package cn.langlang.javainterpreter.ast.expression;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class NewArrayExpression extends Expression {
    private final Type elementType;
    private final List<Expression> dimensions;
    private final ArrayInitializerExpression initializer;
    
    public NewArrayExpression(Token token, Type elementType, List<Expression> dimensions,
                             ArrayInitializerExpression initializer) {
        super(token);
        this.elementType = elementType;
        this.dimensions = dimensions != null ? dimensions : new ArrayList<>();
        this.initializer = initializer;
    }
    
    public Type getElementType() { return elementType; }
    public List<Expression> getDimensions() { return dimensions; }
    public ArrayInitializerExpression getInitializer() { return initializer; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitNewArrayExpression(this);
    }
}
