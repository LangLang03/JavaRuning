package cn.langlang.javanter.ast.statement;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class BlockStatement extends Statement {
    private final List<Statement> statements;
    
    public BlockStatement(Token token, List<Statement> statements) {
        super(token);
        this.statements = statements != null ? statements : new ArrayList<>();
    }
    
    public List<Statement> getStatements() { return statements; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitBlockStatement(this);
    }
}
