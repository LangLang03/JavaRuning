package cn.langlang.javanter.ast.statement;

import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.lexer.Token;

public class ForEachStatement extends Statement {
    private final LocalVariableDeclaration variable;
    private final Expression iterable;
    private final Statement body;
    
    public ForEachStatement(Token token, LocalVariableDeclaration variable,
                           Expression iterable, Statement body) {
        super(token);
        this.variable = variable;
        this.iterable = iterable;
        this.body = body;
    }
    
    public LocalVariableDeclaration getVariable() { return variable; }
    public Expression getIterable() { return iterable; }
    public Statement getBody() { return body; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitForEachStatement(this);
    }
}
