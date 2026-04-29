package cn.langlang.javainterpreter.parser;

import cn.langlang.javainterpreter.lexer.Token;
import cn.langlang.javainterpreter.lexer.TokenType;
import cn.langlang.javainterpreter.ast.misc.Annotation;
import cn.langlang.javainterpreter.ast.type.*;
import java.util.*;

public class TypeParser {
    private final TokenReader reader;
    private final ModifierAndAnnotationParser modifierAndAnnotationParser;
    
    public TypeParser(TokenReader reader, ModifierAndAnnotationParser modifierAndAnnotationParser) {
        this.reader = reader;
        this.modifierAndAnnotationParser = modifierAndAnnotationParser;
    }
    
    public Type parseType() {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        Token token = reader.peek();
        
        String name;
        if (reader.match(TokenType.INT)) name = "int";
        else if (reader.match(TokenType.LONG)) name = "long";
        else if (reader.match(TokenType.SHORT)) name = "short";
        else if (reader.match(TokenType.BYTE)) name = "byte";
        else if (reader.match(TokenType.CHAR)) name = "char";
        else if (reader.match(TokenType.BOOLEAN)) name = "boolean";
        else if (reader.match(TokenType.FLOAT)) name = "float";
        else if (reader.match(TokenType.DOUBLE)) name = "double";
        else if (reader.match(TokenType.VOID)) name = "void";
        else {
            name = parseQualifiedName();
        }
        
        List<TypeArgument> typeArguments = new ArrayList<>();
        if (reader.match(TokenType.LT)) {
            if (reader.match(TokenType.GT)) {
                typeArguments = new ArrayList<>();
            } else {
                typeArguments = parseTypeArguments();
            }
        }
        
        int arrayDimensions = 0;
        while (true) {
            List<Annotation> dimAnnotations = modifierAndAnnotationParser.parseAnnotations();
            if (reader.match(TokenType.LBRACKET)) {
                modifierAndAnnotationParser.parseAnnotations();
                reader.consume(TokenType.RBRACKET, "Expected ']' after '['");
                arrayDimensions++;
            } else {
                break;
            }
        }
        
        return new Type(token, name, typeArguments, arrayDimensions, annotations);
    }
    
    public List<TypeArgument> parseTypeArguments() {
        List<TypeArgument> arguments = new ArrayList<>();
        
        do {
            arguments.add(parseTypeArgument());
        } while (reader.match(TokenType.COMMA));
        
        consumeClosingAngleBracket();
        return arguments;
    }
    
    public void consumeClosingAngleBracket() {
        if (reader.match(TokenType.GT)) {
            return;
        }
        if (reader.match(TokenType.RSHIFT)) {
            Token gtToken = new Token(TokenType.GT, ">", null, reader.previous().getLine(), reader.previous().getColumn());
            reader.insertToken(reader.getCurrentPosition(), gtToken);
            return;
        }
        if (reader.match(TokenType.URSHIFT)) {
            Token gtToken = new Token(TokenType.GT, ">", null, reader.previous().getLine(), reader.previous().getColumn());
            reader.insertToken(reader.getCurrentPosition(), gtToken);
            Token rshiftToken = new Token(TokenType.RSHIFT, ">>", null, reader.previous().getLine(), reader.previous().getColumn() + 1);
            reader.insertToken(reader.getCurrentPosition() + 1, rshiftToken);
            return;
        }
        throw new Parser.ParseError(reader.peek(), "Expected '>' after type arguments");
    }
    
    public TypeArgument parseTypeArgument() {
        Token token = reader.peek();
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        
        if (reader.match(TokenType.QUESTION)) {
            if (reader.match(TokenType.EXTENDS)) {
                Type boundType = parseType();
                return new TypeArgument(token, null, TypeArgument.WildcardKind.EXTENDS, boundType, annotations);
            } else if (reader.match(TokenType.SUPER)) {
                Type boundType = parseType();
                return new TypeArgument(token, null, TypeArgument.WildcardKind.SUPER, boundType, annotations);
            } else {
                return new TypeArgument(token, null, TypeArgument.WildcardKind.UNBOUNDED, null, annotations);
            }
        }
        
        Type type = parseType();
        return new TypeArgument(token, type, TypeArgument.WildcardKind.NONE, null, annotations);
    }
    
    public List<TypeParameter> parseTypeParameters() {
        List<TypeParameter> parameters = new ArrayList<>();
        
        do {
            parameters.add(parseTypeParameter());
        } while (reader.match(TokenType.COMMA));
        
        consumeClosingAngleBracket();
        return parameters;
    }
    
    public TypeParameter parseTypeParameter() {
        Token token = reader.peek();
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        
        String name = reader.consume(TokenType.IDENTIFIER, "Expected type parameter name").getLexeme();
        
        List<Type> bounds = new ArrayList<>();
        if (reader.match(TokenType.EXTENDS)) {
            bounds.add(parseType());
            while (reader.match(TokenType.AMPERSAND)) {
                bounds.add(parseType());
            }
        }
        
        return new TypeParameter(token, name, bounds, annotations);
    }
    
    public List<Type> parseTypeList() {
        List<Type> types = new ArrayList<>();
        
        do {
            types.add(parseType());
        } while (reader.match(TokenType.COMMA));
        
        return types;
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
}
