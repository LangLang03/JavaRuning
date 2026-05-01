package cn.langlang.javanter.ast.statement;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.ast.misc.CaseLabel;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class SwitchStatement extends Statement {
    private final Expression expression;
    private final List<SwitchCase> cases;
    
    public SwitchStatement(Token token, Expression expression, List<SwitchCase> cases) {
        super(token);
        this.expression = expression;
        this.cases = cases != null ? cases : new ArrayList<>();
    }
    
    public Expression getExpression() { return expression; }
    public List<SwitchCase> getCases() { return cases; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitSwitchStatement(this);
    }
    
    public static class SwitchCase {
        private final CaseLabel label;
        private final List<Statement> statements;
        private final boolean isArrow;
        
        public SwitchCase(CaseLabel label, List<Statement> statements) {
            this(label, statements, false);
        }
        
        public SwitchCase(CaseLabel label, List<Statement> statements, boolean isArrow) {
            this.label = label;
            this.statements = statements != null ? statements : new ArrayList<>();
            this.isArrow = isArrow;
        }
        
        public CaseLabel getLabel() { return label; }
        public List<Statement> getStatements() { return statements; }
        public boolean isArrow() { return isArrow; }
    }
}
