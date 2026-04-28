package cn.langlang.javainterpreter.ast;

import cn.langlang.javainterpreter.lexer.Token;

public abstract class Expression extends ASTNode {
    protected Expression(Token token) {
        super(token);
    }
}
