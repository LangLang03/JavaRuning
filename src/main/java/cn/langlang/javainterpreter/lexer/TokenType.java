package cn.langlang.javainterpreter.lexer;

public enum TokenType {
    EOF,
    IDENTIFIER,
    
    INT_LITERAL,
    LONG_LITERAL,
    FLOAT_LITERAL,
    DOUBLE_LITERAL,
    CHAR_LITERAL,
    STRING_LITERAL,
    BOOLEAN_LITERAL,
    NULL_LITERAL,
    
    ABSTRACT,
    ASSERT,
    BOOLEAN,
    BREAK,
    BYTE,
    CASE,
    CATCH,
    CHAR,
    CLASS,
    CONST,
    CONTINUE,
    DEFAULT,
    DO,
    DOUBLE,
    ELSE,
    ENUM,
    EXTENDS,
    FINAL,
    FINALLY,
    FLOAT,
    FOR,
    IF,
    GOTO,
    IMPLEMENTS,
    IMPORT,
    INSTANCEOF,
    INT,
    INTERFACE,
    LONG,
    NATIVE,
    NEW,
    PACKAGE,
    PRIVATE,
    PROTECTED,
    PUBLIC,
    RETURN,
    SHORT,
    STATIC,
    STRICTFP,
    SUPER,
    SWITCH,
    SYNCHRONIZED,
    THIS,
    THROW,
    THROWS,
    TRANSIENT,
    TRY,
    VOID,
    VOLATILE,
    WHILE,
    
    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    LBRACKET,
    RBRACKET,
    SEMICOLON,
    COMMA,
    DOT,
    ELLIPSIS,
    AT,
    COLONCOLON,
    ARROW,
    
    ASSIGN,
    PLUS_ASSIGN,
    MINUS_ASSIGN,
    STAR_ASSIGN,
    SLASH_ASSIGN,
    PERCENT_ASSIGN,
    AND_ASSIGN,
    OR_ASSIGN,
    XOR_ASSIGN,
    LSHIFT_ASSIGN,
    RSHIFT_ASSIGN,
    URSHIFT_ASSIGN,
    
    EQ,
    NE,
    LT,
    GT,
    LE,
    GE,
    
    PLUS,
    MINUS,
    STAR,
    SLASH,
    PERCENT,
    
    PLUSPLUS,
    MINUSMINUS,
    
    NOT,
    AND,
    OR,
    
    TILDE,
    AMPERSAND,
    PIPE,
    CARET,
    
    LSHIFT,
    RSHIFT,
    URSHIFT,
    
    QUESTION,
    COLON,
    
    TRUE,
    FALSE,
    NULL;
    
    public boolean isKeyword() {
        return ordinal() >= ABSTRACT.ordinal() && ordinal() <= WHILE.ordinal();
    }
    
    public boolean isLiteral() {
        return ordinal() >= INT_LITERAL.ordinal() && ordinal() <= NULL_LITERAL.ordinal();
    }
    
    public boolean isOperator() {
        return ordinal() >= ASSIGN.ordinal() && ordinal() <= COLON.ordinal();
    }
}
