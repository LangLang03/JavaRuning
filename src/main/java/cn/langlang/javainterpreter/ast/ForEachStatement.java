package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

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
