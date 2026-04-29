package cn.langlang.javanter.ast.statement;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.lexer.Token;

public abstract class Statement extends ASTNode {
    protected Statement(Token token) {
        super(token);
    }
}
