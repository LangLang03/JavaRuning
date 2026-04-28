package cn.langlang.javainterpreter.api;

import cn.langlang.javainterpreter.lexer.*;
import cn.langlang.javainterpreter.parser.*;
import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.interpreter.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class JavaInterpreter {
    private final Interpreter interpreter;
    
    public JavaInterpreter() {
        this.interpreter = new Interpreter();
    }
    
    public Object execute(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        Parser parser = new Parser(tokens);
        CompilationUnit ast = parser.parseCompilationUnit();
        
        return interpreter.interpret(ast);
    }
    
    public Object executeFile(String filePath) throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(filePath)));
        return execute(source);
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: JavaInterpreter <file.java>");
            System.exit(1);
        }
        
        JavaInterpreter interpreter = new JavaInterpreter();
        
        try {
            interpreter.executeFile(args[0]);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
