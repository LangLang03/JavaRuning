package cn.langlang.javainterpreter;

import cn.langlang.javainterpreter.api.*;
import cn.langlang.javainterpreter.lexer.*;
import cn.langlang.javainterpreter.parser.*;
import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.interpreter.*;

public class DebugParser {
    public static void main(String[] args) {
        String source = "public class Test { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        
        try {
            Lexer lexer = new Lexer(source);
            java.util.List<Token> tokens = lexer.scanTokens();
            System.out.println("Lexer OK, tokens: " + tokens.size());
            
            for (int i = 0; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                System.out.println("  " + i + ": " + t.getType() + " '" + t.getLexeme() + "'");
            }
            
            Parser parser = new Parser(tokens);
            CompilationUnit ast = parser.parseCompilationUnit();
            System.out.println("Parser OK");
            
            Interpreter interpreter = new Interpreter();
            interpreter.interpret(ast);
            System.out.println("Interpreter OK");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
