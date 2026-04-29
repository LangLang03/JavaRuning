package cn.langlang.javanter.ast.misc;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class Annotation extends Expression {
    private final String typeName;
    private final Map<String, Expression> elementValues;
    private final boolean isSingleElement;
    
    public Annotation(Token token, String typeName, Map<String, Expression> elementValues, 
                     boolean isSingleElement) {
        super(token);
        this.typeName = typeName;
        this.elementValues = elementValues != null ? elementValues : new HashMap<>();
        this.isSingleElement = isSingleElement;
    }
    
    public String getTypeName() { return typeName; }
    public Map<String, Expression> getElementValues() { return elementValues; }
    public boolean isSingleElement() { return isSingleElement; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitAnnotation(this);
    }
}
