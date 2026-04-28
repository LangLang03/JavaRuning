package cn.langlang.javainterpreter;

import cn.langlang.javainterpreter.lexer.*;
import cn.langlang.javainterpreter.parser.*;
import cn.langlang.javainterpreter.interpreter.*;
import cn.langlang.javainterpreter.ast.*;
import java.io.*;
import java.nio.file.*;

public class MainRunner {
    public static void main(String[] args) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(".trae/Main.java"))).trim();
            System.out.println("=== Starting to parse Main.java ===");
            System.out.println("Source length: " + source.length() + " characters");
            
            boolean isScript = !source.startsWith("package ") && 
                              !source.startsWith("import ") &&
                              !source.startsWith("public class ") &&
                              !source.startsWith("class ") &&
                              !source.startsWith("public interface ") &&
                              !source.startsWith("interface ") &&
                              !source.startsWith("public enum ") &&
                              !source.startsWith("enum ") &&
                              !source.startsWith("public @interface ") &&
                              !source.startsWith("@interface ");
            
            if (isScript) {
                source = "public class Script { public static void main(String[] args) { " + source + " } }";
                System.out.println("=== Script mode detected, wrapping in class ===");
            }
            
            Lexer lexer = new Lexer(source);
            System.out.println("=== Lexing completed ===");
            
            java.util.List<Token> tokens = lexer.scanTokens();
            System.out.println("Token count: " + tokens.size());
            
            Parser parser = new Parser(tokens);
            System.out.println("=== Starting to parse ===");
            
            CompilationUnit unit = parser.parseCompilationUnit();
            System.out.println("=== Parsing completed successfully ===");
            
            System.out.println("\n=== Type declarations ===");
            for (TypeDeclaration type : unit.getTypeDeclarations()) {
                if (type instanceof ClassDeclaration) {
                    ClassDeclaration cd = (ClassDeclaration) type;
                    System.out.println("Class: " + cd.getName() + 
                        " (fields: " + cd.getFields().size() + 
                        ", methods: " + cd.getMethods().size() + 
                        ", constructors: " + cd.getConstructors().size() + 
                        ", initializers: " + cd.getInitializers().size() + ")");
                } else if (type instanceof InterfaceDeclaration) {
                    System.out.println("Interface: " + ((InterfaceDeclaration) type).getName());
                } else if (type instanceof EnumDeclaration) {
                    System.out.println("Enum: " + ((EnumDeclaration) type).getName());
                } else if (type instanceof AnnotationDeclaration) {
                    System.out.println("Annotation: " + ((AnnotationDeclaration) type).getName());
                }
            }
            
            System.out.println("\n=== Starting interpretation ===");
            Interpreter interpreter = new Interpreter();
            Object result = interpreter.interpret(unit);
            System.out.println("\n=== Interpretation completed ===");
            System.out.println("Result: " + result);
            
        } catch (Parser.ParseError e) {
            System.err.println("Parse error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
