package cn.langlang.javainterpreter.ast.statement;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.lexer.Token;

public abstract class Statement extends ASTNode {
    protected Statement(Token token) {
        super(token);
    }
}
