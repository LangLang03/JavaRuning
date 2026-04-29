package cn.langlang.javainterpreter.ast.expression;

import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.lexer.Token;

public abstract class Expression extends ASTNode {
    protected Expression(Token token) {
        super(token);
    }
}
