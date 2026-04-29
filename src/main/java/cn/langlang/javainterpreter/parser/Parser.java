package cn.langlang.javainterpreter.parser;

import cn.langlang.javainterpreter.lexer.*;
import cn.langlang.javainterpreter.ast.declaration.*;
import cn.langlang.javainterpreter.ast.misc.*;
import java.util.*;

public class Parser {
    private final TokenReader reader;
    private final ModifierAndAnnotationParser modifierAndAnnotationParser;
    private final TypeParser typeParser;
    private final ExpressionParser expressionParser;
    private final StatementParser statementParser;
    private final DeclarationParser declarationParser;
    
    public Parser(List<Token> tokens) {
        this.reader = new TokenReader(tokens);
        this.modifierAndAnnotationParser = new ModifierAndAnnotationParser(reader);
        this.typeParser = new TypeParser(reader, modifierAndAnnotationParser);
        this.expressionParser = new ExpressionParser(reader, modifierAndAnnotationParser, typeParser);
        this.statementParser = new StatementParser(reader, modifierAndAnnotationParser, typeParser);
        this.declarationParser = new DeclarationParser(reader, modifierAndAnnotationParser, typeParser);
        
        this.expressionParser.setStatementParser(statementParser);
        this.expressionParser.setDeclarationParser(declarationParser);
        this.statementParser.setExpressionParser(expressionParser);
        this.declarationParser.setExpressionParser(expressionParser);
        this.declarationParser.setStatementParser(statementParser);
        this.modifierAndAnnotationParser.setExpressionParser(expressionParser);
        this.modifierAndAnnotationParser.setTypeParser(typeParser);
    }
    
    public CompilationUnit parseCompilationUnit() {
        Token token = reader.peek();
        PackageDeclaration packageDecl = null;
        List<ImportDeclaration> imports = new ArrayList<>();
        List<TypeDeclaration> types = new ArrayList<>();
        
        if (reader.match(TokenType.PACKAGE)) {
            packageDecl = parsePackageDeclaration();
        }
        
        while (reader.match(TokenType.IMPORT)) {
            imports.add(parseImportDeclaration());
        }
        
        while (!reader.check(TokenType.EOF)) {
            types.add(declarationParser.parseTypeDeclaration());
        }
        
        return new CompilationUnit(token, packageDecl, imports, types);
    }
    
    private PackageDeclaration parsePackageDeclaration() {
        Token token = reader.previous();
        String name = parseQualifiedName();
        reader.consume(TokenType.SEMICOLON, "Expected ';' after package declaration");
        return new PackageDeclaration(token, name);
    }
    
    private ImportDeclaration parseImportDeclaration() {
        Token token = reader.previous();
        boolean isStatic = reader.match(TokenType.STATIC);
        String name = parseQualifiedName();
        boolean isAsterisk = false;
        
        if (reader.check(TokenType.DOT) && reader.checkNext(TokenType.STAR)) {
            reader.advance();
            reader.advance();
            isAsterisk = true;
        }
        
        reader.consume(TokenType.SEMICOLON, "Expected ';' after import declaration");
        return new ImportDeclaration(token, name, isStatic, isAsterisk);
    }
    
    private String parseQualifiedName() {
        StringBuilder sb = new StringBuilder();
        sb.append(reader.consume(TokenType.IDENTIFIER, "Expected identifier").getLexeme());
        
        while (reader.check(TokenType.DOT) && reader.checkNext(TokenType.IDENTIFIER)) {
            reader.match(TokenType.DOT);
            sb.append(".");
            sb.append(reader.advance().getLexeme());
        }
        
        return sb.toString();
    }
    
    public static class ParseError extends RuntimeException {
        private final Token token;
        
        public ParseError(Token token, String message) {
            super(message + " at line " + token.getLine());
            this.token = token;
        }
        
        public Token getToken() { return token; }
    }
}
