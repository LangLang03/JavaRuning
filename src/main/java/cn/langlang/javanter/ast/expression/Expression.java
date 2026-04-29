package cn.langlang.javanter.ast.expression;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.lexer.Token;

public abstract class Expression extends ASTNode {
    protected Expression(Token token) {
        super(token);
    }
}
