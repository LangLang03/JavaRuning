package cn.langlang.javanter.ast.misc;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.statement.BlockStatement;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class CatchClause extends ASTNode {
    private final List<Type> exceptionTypes;
    private final String exceptionName;
    private final BlockStatement body;
    
    public CatchClause(Token token, List<Type> exceptionTypes, String exceptionName, BlockStatement body) {
        super(token);
        this.exceptionTypes = exceptionTypes != null ? exceptionTypes : new ArrayList<>();
        this.exceptionName = exceptionName;
        this.body = body;
    }
    
    public List<Type> getExceptionTypes() { return exceptionTypes; }
    public String getExceptionName() { return exceptionName; }
    public BlockStatement getBody() { return body; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitCatchClause(this);
    }
}
