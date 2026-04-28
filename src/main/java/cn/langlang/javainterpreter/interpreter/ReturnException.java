package cn.langlang.javainterpreter.interpreter;

import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.lexer.TokenType;
import cn.langlang.javainterpreter.parser.Modifier;
import cn.langlang.javainterpreter.runtime.*;
import java.util.*;

public class ReturnException extends RuntimeException {
    private final Object value;
    
    public ReturnException(Object value) {
        this.value = value;
    }
    
    public Object getValue() { return value; }
}
