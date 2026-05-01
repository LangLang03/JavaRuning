package cn.langlang.javanter.parser;

import cn.langlang.javanter.lexer.Token;
import cn.langlang.javanter.lexer.TokenType;
import cn.langlang.javanter.ast.expression.*;
import cn.langlang.javanter.ast.misc.Annotation;
import cn.langlang.javanter.ast.type.Type;
import java.util.*;

public class ModifierAndAnnotationParser {
    private final TokenReader reader;
    private ExpressionParser expressionParser;
    private TypeParser typeParser;
    
    public ModifierAndAnnotationParser(TokenReader reader) {
        this.reader = reader;
    }
    
    public void setExpressionParser(ExpressionParser expressionParser) {
        this.expressionParser = expressionParser;
    }
    
    public void setTypeParser(TypeParser typeParser) {
        this.typeParser = typeParser;
    }
    
    public int parseModifiers() {
        int modifiers = 0;
        
        while (true) {
            if (reader.match(TokenType.PUBLIC)) modifiers |= Modifier.PUBLIC;
            else if (reader.match(TokenType.PRIVATE)) modifiers |= Modifier.PRIVATE;
            else if (reader.match(TokenType.PROTECTED)) modifiers |= Modifier.PROTECTED;
            else if (reader.match(TokenType.STATIC)) modifiers |= Modifier.STATIC;
            else if (reader.match(TokenType.FINAL)) modifiers |= Modifier.FINAL;
            else if (reader.match(TokenType.SYNCHRONIZED)) modifiers |= Modifier.SYNCHRONIZED;
            else if (reader.match(TokenType.VOLATILE)) modifiers |= Modifier.VOLATILE;
            else if (reader.match(TokenType.TRANSIENT)) modifiers |= Modifier.TRANSIENT;
            else if (reader.match(TokenType.NATIVE)) modifiers |= Modifier.NATIVE;
            else if (reader.match(TokenType.ABSTRACT)) modifiers |= Modifier.ABSTRACT;
            else if (reader.match(TokenType.STRICTFP)) modifiers |= Modifier.STRICTFP;
            else if (reader.match(TokenType.DEFAULT)) modifiers |= Modifier.DEFAULT;
            else if (reader.match(TokenType.SEALED)) modifiers |= Modifier.SEALED;
            else if (reader.match(TokenType.NON_SEALED)) modifiers |= Modifier.NON_SEALED;
            else break;
        }
        
        return modifiers;
    }
    
    public List<Annotation> parseAnnotations() {
        List<Annotation> annotations = new ArrayList<>();
        
        while (reader.check(TokenType.AT) && !reader.checkNext(TokenType.INTERFACE)) {
            reader.match(TokenType.AT);
            annotations.add(parseAnnotation());
        }
        
        return annotations;
    }
    
    public Annotation parseAnnotation() {
        Token token = reader.previous();
        String typeName = parseQualifiedName();
        
        Map<String, Expression> elementValues = new HashMap<>();
        boolean isSingleElement = false;
        
        if (reader.match(TokenType.LPAREN)) {
            if (!reader.check(TokenType.RPAREN)) {
                if (reader.check(TokenType.IDENTIFIER) && reader.checkNext(TokenType.ASSIGN)) {
                    do {
                        String name = reader.consume(TokenType.IDENTIFIER, "Expected element name").getLexeme();
                        reader.consume(TokenType.ASSIGN, "Expected '=' after element name");
                        Expression value = parseElementValue();
                        elementValues.put(name, value);
                    } while (reader.match(TokenType.COMMA));
                } else {
                    isSingleElement = true;
                    elementValues.put("value", parseElementValue());
                }
            }
            reader.consume(TokenType.RPAREN, "Expected ')' after annotation elements");
        }
        
        return new Annotation(token, typeName, elementValues, isSingleElement);
    }
    
    public Expression parseElementValue() {
        if (reader.match(TokenType.AT)) {
            return parseAnnotation();
        }
        
        if (reader.match(TokenType.LBRACE)) {
            return parseAnnotationArrayInitializer();
        }
        
        return parseClassLiteralOrExpression();
    }
    
    private Expression parseClassLiteralOrExpression() {
        int save = reader.getCurrentPosition();
        
        try {
            Type type = typeParser.parseType();
            if (reader.match(TokenType.DOT) && reader.match(TokenType.CLASS)) {
                return new ClassLiteralExpression(type.getToken(), type);
            }
        } catch (Exception e) {
        }
        
        reader.setCurrentPosition(save);
        
        return expressionParser.parseExpression();
    }
    
    public Expression parseAnnotationArrayInitializer() {
        Token token = reader.previous();
        List<Expression> elements = new ArrayList<>();
        
        if (!reader.check(TokenType.RBRACE)) {
            do {
                elements.add(parseElementValue());
            } while (reader.match(TokenType.COMMA));
        }
        
        reader.consume(TokenType.RBRACE, "Expected '}' after array initializer");
        return new ArrayInitializerExpression(token, elements);
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
