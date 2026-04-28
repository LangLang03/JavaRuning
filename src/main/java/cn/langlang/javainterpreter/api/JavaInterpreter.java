package cn.langlang.javainterpreter.api;

import cn.langlang.javainterpreter.lexer.*;
import cn.langlang.javainterpreter.parser.*;
import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.interpreter.*;
import cn.langlang.javainterpreter.runtime.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public class JavaInterpreter {
    private final Interpreter interpreter;
    private final Environment globalEnv;
    
    public JavaInterpreter() {
        this.interpreter = new Interpreter();
        this.globalEnv = interpreter.getGlobalEnvironment();
    }
    
    public Object execute(String source) {
        String trimmedSource = source.trim();
        
        java.util.regex.Pattern classPattern = java.util.regex.Pattern.compile(
            "(?:^|\\s)(?:public\\s+)?(?:abstract\\s+)?(?:final\\s+)?class\\s+\\w");
        boolean hasClassDeclaration = classPattern.matcher(trimmedSource).find();
        
        java.util.regex.Pattern interfacePattern = java.util.regex.Pattern.compile(
            "(?:^|\\s)(?:public\\s+)?interface\\s+\\w");
        boolean hasInterfaceDeclaration = interfacePattern.matcher(trimmedSource).find();
        
        java.util.regex.Pattern enumPattern = java.util.regex.Pattern.compile(
            "(?:^|\\s)(?:public\\s+)?enum\\s+\\w");
        boolean hasEnumDeclaration = enumPattern.matcher(trimmedSource).find();
        
        java.util.regex.Pattern annotationPattern = java.util.regex.Pattern.compile(
            "(?:^|\\s)(?:public\\s+)?@interface\\s+\\w");
        boolean hasAnnotationDeclaration = annotationPattern.matcher(trimmedSource).find();
        
        boolean isFullJavaFile = hasClassDeclaration || hasInterfaceDeclaration || 
                                 hasEnumDeclaration || hasAnnotationDeclaration ||
                                 trimmedSource.startsWith("package ");
        
        if (!isFullJavaFile) {
            String imports = "";
            String body = source;
            
            java.util.regex.Pattern importPattern = java.util.regex.Pattern.compile(
                "(import\\s+(?:static\\s+)?[\\w.]+(?:\\.\\*)?\\s*;\\s*)+");
            java.util.regex.Matcher matcher = importPattern.matcher(source);
            if (matcher.find()) {
                imports = matcher.group();
                body = source.substring(matcher.end());
            }
            
            source = imports + "\npublic class Script { public static void main(String[] args) throws Exception { " + body + " } }";
        }
        
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
    
    public void registerVariable(String name, Object value) {
        globalEnv.defineVariable(name, value);
    }
    
    public Object getVariable(String name) {
        return globalEnv.getVariable(name);
    }
    
    public void setVariable(String name, Object value) {
        globalEnv.setVariable(name, value);
    }
    
    public boolean hasVariable(String name) {
        return globalEnv.hasVariable(name);
    }
    
    public void registerFunction(String name, Function<Object[], Object> function) {
        globalEnv.defineVariable(name, function);
    }
    
    public void registerClass(String name, Class<?> clazz) {
        ScriptClass scriptClass = new ScriptClass(name, name, 0, null, new ArrayList<>(), null);
        globalEnv.defineClass(name, scriptClass);
    }
    
    public Object invokeFunction(String name, Object... args) {
        Object func = globalEnv.getVariable(name);
        if (func instanceof Function) {
            @SuppressWarnings("unchecked")
            Function<Object[], Object> function = (Function<Object[], Object>) func;
            return function.apply(args);
        }
        throw new RuntimeException("Not a function: " + name);
    }
    
    public Environment getGlobalEnvironment() {
        return globalEnv;
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
