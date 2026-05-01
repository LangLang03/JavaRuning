package cn.langlang.javanter.ast.expression;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.misc.CaseLabel;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class SwitchExpression extends Expression {
    private final Expression selector;
    private final List<SwitchCase> cases;
    
    public SwitchExpression(Token token, Expression selector, List<SwitchCase> cases) {
        super(token);
        this.selector = selector;
        this.cases = cases != null ? cases : new ArrayList<>();
    }
    
    public Expression getSelector() { return selector; }
    public List<SwitchCase> getCases() { return cases; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitSwitchExpression(this);
    }
    
    public static class SwitchCase {
        private final List<CaseLabel> labels;
        private final ASTNode body;
        private final boolean isArrow;
        
        public SwitchCase(List<CaseLabel> labels, ASTNode body, boolean isArrow) {
            this.labels = labels != null ? labels : new ArrayList<>();
            this.body = body;
            this.isArrow = isArrow;
        }
        
        public List<CaseLabel> getLabels() { return labels; }
        public ASTNode getBody() { return body; }
        public boolean isArrow() { return isArrow; }
    }
}
