package cn.langlang.javainterpreter.ast.statement;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.ast.expression.Expression;
import cn.langlang.javainterpreter.ast.misc.CatchClause;
import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class TryStatement extends Statement {
    private final List<ResourceDeclaration> resources;
    private final BlockStatement tryBlock;
    private final List<CatchClause> catchClauses;
    private final BlockStatement finallyBlock;
    
    public TryStatement(Token token, List<ResourceDeclaration> resources,
                       BlockStatement tryBlock, List<CatchClause> catchClauses,
                       BlockStatement finallyBlock) {
        super(token);
        this.resources = resources != null ? resources : new ArrayList<>();
        this.tryBlock = tryBlock;
        this.catchClauses = catchClauses != null ? catchClauses : new ArrayList<>();
        this.finallyBlock = finallyBlock;
    }
    
    public List<ResourceDeclaration> getResources() { return resources; }
    public BlockStatement getTryBlock() { return tryBlock; }
    public List<CatchClause> getCatchClauses() { return catchClauses; }
    public BlockStatement getFinallyBlock() { return finallyBlock; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitTryStatement(this);
    }
    
    public static class ResourceDeclaration extends ASTNode {
        private final Type type;
        private final String name;
        private final Expression expression;
        
        public ResourceDeclaration(Token token, Type type, String name, Expression expression) {
            super(token);
            this.type = type;
            this.name = name;
            this.expression = expression;
        }
        
        public Type getType() { return type; }
        public String getName() { return name; }
        public Expression getExpression() { return expression; }
        
        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            throw new UnsupportedOperationException();
        }
    }
}
