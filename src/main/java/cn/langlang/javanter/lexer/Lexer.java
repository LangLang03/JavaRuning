package cn.langlang.javanter.lexer;

import java.util.*;

/**
 * Lexical analyzer (tokenizer) for the JavaInterpreter.
 *
 * <p>The Lexer converts raw Java source code into a stream of {@link Token}s,
 * which are then consumed by the {@link cn.langlang.javanter.parser.Parser} to
 * create an Abstract Syntax Tree (AST).</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Tracks line and column numbers for accurate error reporting</li>
 *   <li>Handles all Java tokens including operators, keywords, literals</li>
 *   <li>Supports Unicode escapes (backslash-u followed by 4 hex digits) in string and character literals</li>
 *   <li>Recognizes hex ({@code 0x}), binary ({@code 0b}), and decimal number formats</li>
 *   <li>Handles single-line ({@code //}) and block ({@code /*}) comments</li>
 *   <li>Supports numeric literals with underscores for readability (Java 7+)</li>
 * </ul>
 *
 * <p>Tokenization process:</p>
 * <ol>
 *   <li>Scan each character to identify token boundaries</li>
 *   <li>Recognize keywords, operators, and punctuation</li>
 *   <li>Identify identifiers vs keywords using a lookup table</li>
 *   <li>Parse numeric, string, and character literals</li>
 *   <li>Handle escape sequences in string/character literals</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>
 * String source = "int x = 10 + 5;";
 * Lexer lexer = new Lexer(source);
 * List&lt;Token&gt; tokens = lexer.scanTokens();
 * // tokens now contains: INT, IDENTIFIER("x"), ASSIGN, INT_LITERAL(10), PLUS, INT_LITERAL(5), SEMICOLON, EOF
 * </pre>
 *
 * @see Token
 * @see TokenType
 * @author Javanter Development Team
 */
public class Lexer {
    private final String source;
    private final List<Token> tokens;
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    
    private static final Map<String, TokenType> keywords;

    /**
     * Static initializer that populates the keyword lookup table.
     * Maps all Java reserved words and keywords to their corresponding token types.
     */
    static {
        keywords = new HashMap<>();
        keywords.put("abstract", TokenType.ABSTRACT);
        keywords.put("assert", TokenType.ASSERT);
        keywords.put("boolean", TokenType.BOOLEAN);
        keywords.put("break", TokenType.BREAK);
        keywords.put("byte", TokenType.BYTE);
        keywords.put("case", TokenType.CASE);
        keywords.put("catch", TokenType.CATCH);
        keywords.put("char", TokenType.CHAR);
        keywords.put("class", TokenType.CLASS);
        keywords.put("const", TokenType.CONST);
        keywords.put("continue", TokenType.CONTINUE);
        keywords.put("default", TokenType.DEFAULT);
        keywords.put("do", TokenType.DO);
        keywords.put("double", TokenType.DOUBLE);
        keywords.put("else", TokenType.ELSE);
        keywords.put("enum", TokenType.ENUM);
        keywords.put("extends", TokenType.EXTENDS);
        keywords.put("final", TokenType.FINAL);
        keywords.put("finally", TokenType.FINALLY);
        keywords.put("float", TokenType.FLOAT);
        keywords.put("for", TokenType.FOR);
        keywords.put("if", TokenType.IF);
        keywords.put("goto", TokenType.GOTO);
        keywords.put("implements", TokenType.IMPLEMENTS);
        keywords.put("import", TokenType.IMPORT);
        keywords.put("instanceof", TokenType.INSTANCEOF);
        keywords.put("int", TokenType.INT);
        keywords.put("interface", TokenType.INTERFACE);
        keywords.put("long", TokenType.LONG);
        keywords.put("native", TokenType.NATIVE);
        keywords.put("new", TokenType.NEW);
        keywords.put("package", TokenType.PACKAGE);
        keywords.put("private", TokenType.PRIVATE);
        keywords.put("protected", TokenType.PROTECTED);
        keywords.put("public", TokenType.PUBLIC);
        keywords.put("return", TokenType.RETURN);
        keywords.put("short", TokenType.SHORT);
        keywords.put("static", TokenType.STATIC);
        keywords.put("strictfp", TokenType.STRICTFP);
        keywords.put("super", TokenType.SUPER);
        keywords.put("switch", TokenType.SWITCH);
        keywords.put("synchronized", TokenType.SYNCHRONIZED);
        keywords.put("this", TokenType.THIS);
        keywords.put("throw", TokenType.THROW);
        keywords.put("throws", TokenType.THROWS);
        keywords.put("transient", TokenType.TRANSIENT);
        keywords.put("try", TokenType.TRY);
        keywords.put("void", TokenType.VOID);
        keywords.put("volatile", TokenType.VOLATILE);
        keywords.put("while", TokenType.WHILE);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("null", TokenType.NULL);
    }

    /**
     * Constructs a Lexer for the given source code.
     *
     * @param source The Java source code to tokenize
     */
    public Lexer(String source) {
        this.source = source;
        this.tokens = new ArrayList<>();
    }

    /**
     * Scans all tokens from the source code.
     *
     * <p>This is the main entry point for lexical analysis. It processes
     * the entire source string character by character until EOF is reached.</p>
     *
     * @return A list of tokens representing the source code
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, column));
        return tokens;
    }

    /**
     * Scans a single token from the current position.
     *
     * <p>This method examines the current character and dispatches to the
     * appropriate scanning method based on the character value:</p>
     * <ul>
     *   <li>Single-char tokens: parentheses, braces, brackets, operators, etc.</li>
     *   <li>Double-char tokens: compound operators like {@code ==}, {@code !=}, {@code &&}</li>
     *   <li>Triple-char tokens: {@code ...} (ellipsis), {@code >>>} (unsigned right shift)</li>
     *   <li>Whitespace: ignored (except for newline tracking)</li>
     *   <li>Comments: skipped over</li>
     *   <li>String/char literals: handled by {@link #scanString} and {@link #scanChar}</li>
     *   <li>Numbers: handled by {@link #scanNumber}</li>
     *   <li>Identifiers/keywords: handled by {@link #scanIdentifier}</li>
     * </ul>
     */
    private void scanToken() {
        char c = advance();
        
        switch (c) {
            case ' ': case '\t': case '\r': break;
            case '\n':
                line++;
                column = 1;
                break;
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '{': addToken(TokenType.LBRACE); break;
            case '}': addToken(TokenType.RBRACE); break;
            case '[': addToken(TokenType.LBRACKET); break;
            case ']': addToken(TokenType.RBRACKET); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': 
                if (match('.') && match('.')) {
                    addToken(TokenType.ELLIPSIS);
                } else {
                    addToken(TokenType.DOT);
                }
                break;
            case '@': addToken(TokenType.AT); break;
            case '?': addToken(TokenType.QUESTION); break;
            case ':':
                if (match(':')) {
                    addToken(TokenType.COLONCOLON);
                } else {
                    addToken(TokenType.COLON);
                }
                break;
            case '+':
                if (match('+')) {
                    addToken(TokenType.PLUSPLUS);
                } else if (match('=')) {
                    addToken(TokenType.PLUS_ASSIGN);
                } else {
                    addToken(TokenType.PLUS);
                }
                break;
            case '-':
                if (match('-')) {
                    addToken(TokenType.MINUSMINUS);
                } else if (match('=')) {
                    addToken(TokenType.MINUS_ASSIGN);
                } else if (match('>')) {
                    addToken(TokenType.ARROW);
                } else {
                    addToken(TokenType.MINUS);
                }
                break;
            case '*':
                if (match('=')) {
                    addToken(TokenType.STAR_ASSIGN);
                } else {
                    addToken(TokenType.STAR);
                }
                break;
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    scanBlockComment();
                } else if (match('=')) {
                    addToken(TokenType.SLASH_ASSIGN);
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            case '%':
                if (match('=')) {
                    addToken(TokenType.PERCENT_ASSIGN);
                } else {
                    addToken(TokenType.PERCENT);
                }
                break;
            case '!':
                if (match('=')) {
                    addToken(TokenType.NE);
                } else {
                    addToken(TokenType.NOT);
                }
                break;
            case '=':
                if (match('=')) {
                    addToken(TokenType.EQ);
                } else {
                    addToken(TokenType.ASSIGN);
                }
                break;
            case '<':
                if (match('<')) {
                    if (match('=')) {
                        addToken(TokenType.LSHIFT_ASSIGN);
                    } else {
                        addToken(TokenType.LSHIFT);
                    }
                } else if (match('=')) {
                    addToken(TokenType.LE);
                } else {
                    addToken(TokenType.LT);
                }
                break;
            case '>':
                if (match('>')) {
                    if (match('>')) {
                        if (match('=')) {
                            addToken(TokenType.URSHIFT_ASSIGN);
                        } else {
                            addToken(TokenType.URSHIFT);
                        }
                    } else if (match('=')) {
                        addToken(TokenType.RSHIFT_ASSIGN);
                    } else {
                        addToken(TokenType.RSHIFT);
                    }
                } else if (match('=')) {
                    addToken(TokenType.GE);
                } else {
                    addToken(TokenType.GT);
                }
                break;
            case '&':
                if (match('&')) {
                    addToken(TokenType.AND);
                } else if (match('=')) {
                    addToken(TokenType.AND_ASSIGN);
                } else {
                    addToken(TokenType.AMPERSAND);
                }
                break;
            case '|':
                if (match('|')) {
                    addToken(TokenType.OR);
                } else if (match('=')) {
                    addToken(TokenType.OR_ASSIGN);
                } else {
                    addToken(TokenType.PIPE);
                }
                break;
            case '^':
                if (match('=')) {
                    addToken(TokenType.XOR_ASSIGN);
                } else {
                    addToken(TokenType.CARET);
                }
                break;
            case '~':
                addToken(TokenType.TILDE);
                break;
            case '"': scanString(); break;
            case '\'': scanChar(); break;
            default:
                if (isDigit(c)) {
                    scanNumber();
                } else if (isAlpha(c)) {
                    scanIdentifier();
                } else {
                    throw new LexerException("Unexpected character: '" + c + "' at line " + line + ", column " + column);
                }
                break;
        }
    }
    
    private void scanBlockComment() {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                break;
            }
            if (peek() == '\n') {
                line++;
                column = 1;
            }
            advance();
        }
    }
    
    private void scanString() {
        StringBuilder value = new StringBuilder();
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                column = 1;
            }
            if (peek() == '\\') {
                advance();
                value.append(scanEscapeSequence());
            } else {
                value.append(advance());
            }
        }
        if (isAtEnd()) {
            throw new LexerException("Unterminated string at line " + line);
        }
        advance();
        addToken(TokenType.STRING_LITERAL, value.toString());
    }
    
    private void scanChar() {
        char value;
        if (peek() == '\\') {
            advance();
            value = scanEscapeSequence();
        } else {
            value = advance();
        }
        if (peek() != '\'') {
            throw new LexerException("Unterminated character literal at line " + line);
        }
        advance();
        addToken(TokenType.CHAR_LITERAL, value);
    }

    /**
     * Scans and returns the character value for an escape sequence.
     *
     * <p>Supported escape sequences:</p>
     * <ul>
     *   <li>{@code \b} - Backspace</li>
     *   <li>{@code \t} - Tab</li>
     *   <li>{@code \n} - Newline</li>
     *   <li>{@code \f} - Form feed</li>
     *   <li>{@code \r} - Carriage return</li>
     *   <li>{@code \"} - Double quote</li>
     *   <li>{@code \'} - Single quote</li>
     *   <li>{@code \\} - Backslash</li>
     *   <li>{@code \0-7} - Octal escape (1-3 digits)</li>
     *   <li>{@code \\uXXXX} - Unicode escape (4 hex digits)</li>
     * </ul>
     *
     * @return The interpreted character value
     * @throws LexerException if an invalid escape sequence is encountered
     */
    private char scanEscapeSequence() {
        char c = advance();
        switch (c) {
            case 'b': return '\b';
            case 't': return '\t';
            case 'n': return '\n';
            case 'f': return '\f';
            case 'r': return '\r';
            case '"': return '"';
            case '\'': return '\'';
            case '\\': return '\\';
            case '0': case '1': case '2': case '3':
            case '4': case '5': case '6': case '7':
                return scanOctalEscape(c);
            case 'u':
                return scanUnicodeEscape();
            default:
                throw new LexerException("Invalid escape sequence: \\" + c + " at line " + line);
        }
    }
    
    private char scanOctalEscape(char first) {
        int value = first - '0';
        if (isOctalDigit(peek())) {
            value = value * 8 + (advance() - '0');
            if (isOctalDigit(peek()) && value <= 3) {
                value = value * 8 + (advance() - '0');
            }
        }
        return (char) value;
    }
    
    private char scanUnicodeEscape() {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            char c = advance();
            if (!isHexDigit(c)) {
                throw new LexerException("Invalid unicode escape sequence at line " + line);
            }
            value = value * 16 + hexValue(c);
        }
        return (char) value;
    }
    
    private void scanNumber() {
        boolean isHex = false;
        boolean isBinary = false;
        
        if (peek() == 'x' || peek() == 'X') {
            advance();
            isHex = true;
            scanHexNumber();
            return;
        } else if (peek() == 'b' || peek() == 'B') {
            advance();
            isBinary = true;
            scanBinaryNumber();
            return;
        }
        
        while (isDigit(peek()) || peek() == '_') {
            advance();
        }
        
        boolean isFloat = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isFloat = true;
            advance();
            while (isDigit(peek()) || peek() == '_') {
                advance();
            }
        }
        
        if (peek() == 'e' || peek() == 'E') {
            isFloat = true;
            advance();
            if (peek() == '+' || peek() == '-') {
                advance();
            }
            while (isDigit(peek()) || peek() == '_') {
                advance();
            }
        }
        
        String lexeme = source.substring(start, current).replace("_", "");
        
        if (isFloat) {
            if (peek() == 'f' || peek() == 'F') {
                advance();
                addToken(TokenType.FLOAT_LITERAL, Float.parseFloat(lexeme));
            } else {
                if (peek() == 'd' || peek() == 'D') {
                    advance();
                }
                addToken(TokenType.DOUBLE_LITERAL, Double.parseDouble(lexeme));
            }
        } else {
            if (peek() == 'l' || peek() == 'L') {
                advance();
                addToken(TokenType.LONG_LITERAL, Long.parseLong(lexeme));
            } else {
                addToken(TokenType.INT_LITERAL, Integer.parseInt(lexeme));
            }
        }
    }
    
    private void scanHexNumber() {
        while (isHexDigit(peek()) || peek() == '_') {
            advance();
        }
        
        String lexeme = source.substring(start, current);
        String hexValue = lexeme.replace("_", "").substring(2);
        
        if (peek() == 'l' || peek() == 'L') {
            advance();
            addToken(TokenType.LONG_LITERAL, Long.parseLong(hexValue, 16));
        } else {
            long value = Long.parseLong(hexValue, 16);
            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
                addToken(TokenType.LONG_LITERAL, value);
            } else {
                addToken(TokenType.INT_LITERAL, (int) value);
            }
        }
    }
    
    private void scanBinaryNumber() {
        while (peek() == '0' || peek() == '1' || peek() == '_') {
            advance();
        }
        
        String lexeme = source.substring(start, current);
        String binaryValue = lexeme.replace("_", "").substring(2);
        
        if (peek() == 'l' || peek() == 'L') {
            advance();
            addToken(TokenType.LONG_LITERAL, Long.parseLong(binaryValue, 2));
        } else {
            addToken(TokenType.INT_LITERAL, Integer.parseInt(binaryValue, 2));
        }
    }
    
    private void scanIdentifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) {
            type = TokenType.IDENTIFIER;
        } else if (type == TokenType.TRUE) {
            addToken(TokenType.BOOLEAN_LITERAL, true);
            return;
        } else if (type == TokenType.FALSE) {
            addToken(TokenType.BOOLEAN_LITERAL, false);
            return;
        } else if (type == TokenType.NULL) {
            addToken(TokenType.NULL_LITERAL, null);
            return;
        }
        addToken(type);
    }
    
    private boolean isAtEnd() {
        return current >= source.length();
    }
    
    private char advance() {
        column++;
        return source.charAt(current++);
    }
    
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        column++;
        return true;
    }
    
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }
    
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }
    
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    
    private boolean isOctalDigit(char c) {
        return c >= '0' && c <= '7';
    }
    
    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
    
    private int hexValue(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        return c - 'A' + 10;
    }
    
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
    }
    
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
    
    private void addToken(TokenType type) {
        addToken(type, null);
    }
    
    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(start, current);
        tokens.add(new Token(type, lexeme, literal, line, column - lexeme.length()));
    }

    /**
     * Exception thrown when the lexer encounters invalid source code.
     *
     * <p>This includes:</p>
     * <ul>
     *   <li>Unterminated string literals</li>
     *   <li>Unterminated character literals</li>
     *   <li>Invalid escape sequences</li>
     *   <li>Invalid unicode escapes</li>
     *   <li>Unexpected characters not matching any token pattern</li>
     * </ul>
     */
    public static class LexerException extends RuntimeException {
        public LexerException(String message) {
            super(message);
        }
    }
}
