package cn.langlang.javainterpreter;

import cn.langlang.javainterpreter.api.*;
import cn.langlang.javainterpreter.lexer.*;
import cn.langlang.javainterpreter.parser.*;
import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.interpreter.*;

public class DebugMain {
    public static void main(String[] args) throws Exception {
        String testSource = "public class Test { public static void main(String[] args) { @NonNull String @Nullable [] arr = null; } }";
        
        System.out.println("=== Testing annotated array type ===");
        
        try {
            Lexer lexer = new Lexer(testSource);
            java.util.List<Token> tokens = lexer.scanTokens();
            System.out.println("Lexer OK, tokens: " + tokens.size());
            
            for (int i = 0; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                System.out.println("  " + i + ": " + t.getType() + " '" + t.getLexeme() + "'");
            }
            
            Parser parser = new Parser(tokens);
            CompilationUnit ast = parser.parseCompilationUnit();
            System.out.println("Parser OK");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
