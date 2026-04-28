package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public abstract class Statement extends ASTNode {
    protected Statement(Token token) {
        super(token);
    }
}
