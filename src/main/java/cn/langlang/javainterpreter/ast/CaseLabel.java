package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class CaseLabel extends ASTNode {
    private final boolean isDefault;
    private final List<Expression> values;
    
    public CaseLabel(Token token, boolean isDefault, List<Expression> values) {
        super(token);
        this.isDefault = isDefault;
        this.values = values != null ? values : new ArrayList<>();
    }
    
    public boolean isDefault() { return isDefault; }
    public List<Expression> getValues() { return values; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitCaseLabel(this);
    }
}
