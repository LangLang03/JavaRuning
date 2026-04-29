package cn.langlang.javainterpreter.parser;

import cn.langlang.javainterpreter.lexer.Token;
import cn.langlang.javainterpreter.lexer.TokenType;
import java.util.List;

public class TokenReader {
    private final List<Token> tokens;
    private int current = 0;
    
    public TokenReader(List<Token> tokens) {
        this.tokens = tokens;
    }
    
    public Token peek() {
        return tokens.get(current);
    }
    
    public Token previous() {
        return tokens.get(current - 1);
    }
    
    public Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    
    public boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }
    
    public boolean check(TokenType type) {
        if (isAtEnd()) return type == TokenType.EOF;
        return peek().getType() == type;
    }
    
    public boolean checkIdentifier() {
        return check(TokenType.IDENTIFIER);
    }
    
    public boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).getType() == type;
    }
    
    public boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }
    
    public Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new Parser.ParseError(peek(), message);
    }
    
    public int getCurrentPosition() {
        return current;
    }
    
    public void setCurrentPosition(int position) {
        this.current = position;
    }
    
    public List<Token> getTokens() {
        return tokens;
    }
    
    public void insertToken(int index, Token token) {
        tokens.add(index, token);
    }
}
