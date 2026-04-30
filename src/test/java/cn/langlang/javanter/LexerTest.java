package cn.langlang.javanter;

import cn.langlang.javanter.lexer.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class LexerTest {
    
    @Test
    public void testKeywords() {
        String source = "public class private protected static final void int long double float boolean char byte short";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.PUBLIC, tokens.get(0).getType());
        assertEquals(TokenType.CLASS, tokens.get(1).getType());
        assertEquals(TokenType.PRIVATE, tokens.get(2).getType());
        assertEquals(TokenType.PROTECTED, tokens.get(3).getType());
        assertEquals(TokenType.STATIC, tokens.get(4).getType());
        assertEquals(TokenType.FINAL, tokens.get(5).getType());
    }
    
    @Test
    public void testIdentifiers() {
        String source = "myVariable _underscore $dollar CamelCase";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals("myVariable", tokens.get(0).getLexeme());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals("_underscore", tokens.get(1).getLexeme());
        assertEquals(TokenType.IDENTIFIER, tokens.get(2).getType());
        assertEquals("$dollar", tokens.get(2).getLexeme());
    }
    
    @Test
    public void testIntegerLiterals() {
        String source = "42 0 123456789";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.INT_LITERAL, tokens.get(0).getType());
        assertEquals(42, tokens.get(0).getLiteral());
        assertEquals(TokenType.INT_LITERAL, tokens.get(1).getType());
        assertEquals(0, tokens.get(1).getLiteral());
        assertEquals(TokenType.INT_LITERAL, tokens.get(2).getType());
        assertEquals(123456789, tokens.get(2).getLiteral());
    }
    
    @Test
    public void testLongLiterals() {
        String source = "42L 0l 123456789L";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.LONG_LITERAL, tokens.get(0).getType());
        assertEquals(42L, tokens.get(0).getLiteral());
        assertEquals(TokenType.LONG_LITERAL, tokens.get(1).getType());
        assertEquals(0L, tokens.get(1).getLiteral());
    }
    
    @Test
    public void testFloatLiterals() {
        String source = "3.14f 1.0F 2.5e3f";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.FLOAT_LITERAL, tokens.get(0).getType());
        assertEquals(3.14f, tokens.get(0).getLiteral());
        assertEquals(TokenType.FLOAT_LITERAL, tokens.get(1).getType());
        assertEquals(1.0F, tokens.get(1).getLiteral());
    }
    
    @Test
    public void testDoubleLiterals() {
        String source = "3.14 1.0 2.5e3 1.5e-3";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.DOUBLE_LITERAL, tokens.get(0).getType());
        assertEquals(3.14, tokens.get(0).getLiteral());
        assertEquals(TokenType.DOUBLE_LITERAL, tokens.get(1).getType());
        assertEquals(1.0, tokens.get(1).getLiteral());
    }
    
    @Test
    public void testStringLiterals() {
        String source = "\"hello world\" \"escaped\\nstring\" \"unicode\\u0041\"";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.STRING_LITERAL, tokens.get(0).getType());
        assertEquals("hello world", tokens.get(0).getLiteral());
        assertEquals(TokenType.STRING_LITERAL, tokens.get(1).getType());
        assertEquals("escaped\nstring", tokens.get(1).getLiteral());
        assertEquals(TokenType.STRING_LITERAL, tokens.get(2).getType());
        assertEquals("unicodeA", tokens.get(2).getLiteral());
    }
    
    @Test
    public void testCharLiterals() {
        String source = "'a' '\\n' '\\t' '\\''";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.CHAR_LITERAL, tokens.get(0).getType());
        assertEquals('a', tokens.get(0).getLiteral());
        assertEquals(TokenType.CHAR_LITERAL, tokens.get(1).getType());
        assertEquals('\n', tokens.get(1).getLiteral());
        assertEquals(TokenType.CHAR_LITERAL, tokens.get(2).getType());
        assertEquals('\t', tokens.get(2).getLiteral());
        assertEquals(TokenType.CHAR_LITERAL, tokens.get(3).getType());
        assertEquals('\'', tokens.get(3).getLiteral());
    }
    
    @Test
    public void testBinaryLiterals() {
        String source = "0b1010 0B1111_0000";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.INT_LITERAL, tokens.get(0).getType());
        assertEquals(10, tokens.get(0).getLiteral());
        assertEquals(TokenType.INT_LITERAL, tokens.get(1).getType());
        assertEquals(240, tokens.get(1).getLiteral());
    }
    
    @Test
    public void testHexLiterals() {
        String source = "0xFF 0xCAFE_BABE";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.INT_LITERAL, tokens.get(0).getType());
        assertEquals(255, tokens.get(0).getLiteral());
        assertEquals(TokenType.LONG_LITERAL, tokens.get(1).getType());
        assertEquals(0xCAFEBABEL, tokens.get(1).getLiteral());
    }
    
    @Test
    public void testOperators() {
        String source = "+ - * / % = == != < > <= >= && || ! & | ^ << >> >>>";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.PLUS, tokens.get(0).getType());
        assertEquals(TokenType.MINUS, tokens.get(1).getType());
        assertEquals(TokenType.STAR, tokens.get(2).getType());
        assertEquals(TokenType.SLASH, tokens.get(3).getType());
        assertEquals(TokenType.PERCENT, tokens.get(4).getType());
        assertEquals(TokenType.ASSIGN, tokens.get(5).getType());
        assertEquals(TokenType.EQ, tokens.get(6).getType());
        assertEquals(TokenType.NE, tokens.get(7).getType());
        assertEquals(TokenType.LT, tokens.get(8).getType());
        assertEquals(TokenType.GT, tokens.get(9).getType());
        assertEquals(TokenType.LE, tokens.get(10).getType());
        assertEquals(TokenType.GE, tokens.get(11).getType());
        assertEquals(TokenType.AND, tokens.get(12).getType());
        assertEquals(TokenType.OR, tokens.get(13).getType());
        assertEquals(TokenType.NOT, tokens.get(14).getType());
        assertEquals(TokenType.AMPERSAND, tokens.get(15).getType());
        assertEquals(TokenType.PIPE, tokens.get(16).getType());
        assertEquals(TokenType.CARET, tokens.get(17).getType());
        assertEquals(TokenType.LSHIFT, tokens.get(18).getType());
        assertEquals(TokenType.RSHIFT, tokens.get(19).getType());
        assertEquals(TokenType.URSHIFT, tokens.get(20).getType());
    }
    
    @Test
    public void testCompoundAssignments() {
        String source = "+= -= *= /= %= &= |= ^= <<= >>= >>>=";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.PLUS_ASSIGN, tokens.get(0).getType());
        assertEquals(TokenType.MINUS_ASSIGN, tokens.get(1).getType());
        assertEquals(TokenType.STAR_ASSIGN, tokens.get(2).getType());
        assertEquals(TokenType.SLASH_ASSIGN, tokens.get(3).getType());
        assertEquals(TokenType.PERCENT_ASSIGN, tokens.get(4).getType());
        assertEquals(TokenType.AND_ASSIGN, tokens.get(5).getType());
        assertEquals(TokenType.OR_ASSIGN, tokens.get(6).getType());
        assertEquals(TokenType.XOR_ASSIGN, tokens.get(7).getType());
        assertEquals(TokenType.LSHIFT_ASSIGN, tokens.get(8).getType());
        assertEquals(TokenType.RSHIFT_ASSIGN, tokens.get(9).getType());
        assertEquals(TokenType.URSHIFT_ASSIGN, tokens.get(10).getType());
    }
    
    @Test
    public void testIncrementDecrement() {
        String source = "++ --";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.PLUSPLUS, tokens.get(0).getType());
        assertEquals(TokenType.MINUSMINUS, tokens.get(1).getType());
    }
    
    @Test
    public void testDelimiters() {
        String source = "( ) { } [ ] ; , . ... @ :: ->";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.LPAREN, tokens.get(0).getType());
        assertEquals(TokenType.RPAREN, tokens.get(1).getType());
        assertEquals(TokenType.LBRACE, tokens.get(2).getType());
        assertEquals(TokenType.RBRACE, tokens.get(3).getType());
        assertEquals(TokenType.LBRACKET, tokens.get(4).getType());
        assertEquals(TokenType.RBRACKET, tokens.get(5).getType());
        assertEquals(TokenType.SEMICOLON, tokens.get(6).getType());
        assertEquals(TokenType.COMMA, tokens.get(7).getType());
        assertEquals(TokenType.DOT, tokens.get(8).getType());
        assertEquals(TokenType.ELLIPSIS, tokens.get(9).getType());
        assertEquals(TokenType.AT, tokens.get(10).getType());
        assertEquals(TokenType.COLONCOLON, tokens.get(11).getType());
        assertEquals(TokenType.ARROW, tokens.get(12).getType());
    }
    
    @Test
    public void testComments() {
        String source = "// single line comment\nint x; /* multi\nline\ncomment */ int y;";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.INT, tokens.get(0).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals("x", tokens.get(1).getLexeme());
        assertEquals(TokenType.INT, tokens.get(3).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(4).getType());
        assertEquals("y", tokens.get(4).getLexeme());
    }
    
    @Test
    public void testBooleanLiterals() {
        String source = "true false";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.BOOLEAN_LITERAL, tokens.get(0).getType());
        assertEquals(true, tokens.get(0).getLiteral());
        assertEquals(TokenType.BOOLEAN_LITERAL, tokens.get(1).getType());
        assertEquals(false, tokens.get(1).getLiteral());
    }
    
    @Test
    public void testNullLiteral() {
        String source = "null";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.NULL_LITERAL, tokens.get(0).getType());
        assertNull(tokens.get(0).getLiteral());
    }
    
    @Test
    public void testNestedGenericTokens() {
        String source = "Box<Box<Box<String>>> deep";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals("Box", tokens.get(0).getLexeme());
        assertEquals(TokenType.LT, tokens.get(1).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(2).getType());
        assertEquals("Box", tokens.get(2).getLexeme());
        assertEquals(TokenType.LT, tokens.get(3).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(4).getType());
        assertEquals("Box", tokens.get(4).getLexeme());
        assertEquals(TokenType.LT, tokens.get(5).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(6).getType());
        assertEquals("String", tokens.get(6).getLexeme());
        assertEquals(TokenType.URSHIFT, tokens.get(7).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(8).getType());
        assertEquals("deep", tokens.get(8).getLexeme());
    }
    
    @Test
    public void testTwoLevelNestedGenericTokens() {
        String source = "Box<Box<String>> two";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        List<TokenType> expectedClosing = Arrays.asList(TokenType.RSHIFT);
        List<TokenType> actualClosing = new ArrayList<>();
        for (Token t : tokens) {
            if (t.getType() == TokenType.GT || t.getType() == TokenType.RSHIFT || t.getType() == TokenType.URSHIFT) {
                actualClosing.add(t.getType());
            }
        }
        
        assertEquals(expectedClosing, actualClosing, "Two level nested generics should produce RSHIFT for >>");
    }
    
    @Test
    public void testThreeLevelNestedGenericTokens() {
        String source = "Box<Box<Box<String>>> three";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        List<TokenType> expectedClosing = Arrays.asList(TokenType.URSHIFT);
        List<TokenType> actualClosing = new ArrayList<>();
        for (Token t : tokens) {
            if (t.getType() == TokenType.GT || t.getType() == TokenType.RSHIFT || t.getType() == TokenType.URSHIFT) {
                actualClosing.add(t.getType());
            }
        }
        
        assertEquals(expectedClosing, actualClosing, "Three level nested generics should produce URSHIFT for >>>");
    }
    
    @Test
    public void testFourLevelNestedGenericTokens() {
        String source = "Box<Box<Box<Box<String>>>> four";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        List<TokenType> expectedClosing = Arrays.asList(TokenType.URSHIFT, TokenType.GT);
        List<TokenType> actualClosing = new ArrayList<>();
        for (Token t : tokens) {
            if (t.getType() == TokenType.GT || t.getType() == TokenType.RSHIFT || t.getType() == TokenType.URSHIFT) {
                actualClosing.add(t.getType());
            }
        }
        
        assertEquals(expectedClosing, actualClosing, "Four level nested generics should produce URSHIFT GT for >>>>");
    }
    
    @Test
    public void testFiveLevelNestedGenericTokens() {
        String source = "Box<Box<Box<Box<Box<String>>>>> five";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        List<TokenType> expectedClosing = Arrays.asList(TokenType.URSHIFT, TokenType.RSHIFT);
        List<TokenType> actualClosing = new ArrayList<>();
        for (Token t : tokens) {
            if (t.getType() == TokenType.GT || t.getType() == TokenType.RSHIFT || t.getType() == TokenType.URSHIFT) {
                actualClosing.add(t.getType());
            }
        }
        
        assertEquals(expectedClosing, actualClosing, "Five level nested generics should produce URSHIFT RSHIFT for >>>>>");
    }
    
    @Test
    public void testSixLevelNestedGenericTokens() {
        String source = "Box<Box<Box<Box<Box<Box<String>>>>>> six";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        
        List<TokenType> expectedClosing = Arrays.asList(TokenType.URSHIFT, TokenType.URSHIFT);
        List<TokenType> actualClosing = new ArrayList<>();
        for (Token t : tokens) {
            if (t.getType() == TokenType.GT || t.getType() == TokenType.RSHIFT || t.getType() == TokenType.URSHIFT) {
                actualClosing.add(t.getType());
            }
        }
        
        assertEquals(expectedClosing, actualClosing, "Six level nested generics should produce URSHIFT URSHIFT for >>>>>>>");
    }
}
