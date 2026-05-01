package cn.langlang.javanter.parser;

import cn.langlang.javanter.lexer.Token;
import cn.langlang.javanter.lexer.TokenType;
import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.expression.*;
import cn.langlang.javanter.ast.misc.*;
import cn.langlang.javanter.ast.statement.BlockStatement;
import cn.langlang.javanter.ast.statement.Statement;
import cn.langlang.javanter.ast.type.*;
import cn.langlang.javanter.ast.declaration.*;
import java.util.*;

public class ExpressionParser {
    private static final int PRECEDENCE_MULTIPLICATIVE = 12;
    private static final int PRECEDENCE_ADDITIVE = 11;
    private static final int PRECEDENCE_SHIFT = 10;
    private static final int PRECEDENCE_RELATIONAL = 9;
    private static final int PRECEDENCE_EQUALITY = 8;
    private static final int PRECEDENCE_BITWISE_AND = 7;
    private static final int PRECEDENCE_BITWISE_XOR = 6;
    private static final int PRECEDENCE_BITWISE_OR = 5;
    private static final int PRECEDENCE_LOGICAL_AND = 4;
    private static final int PRECEDENCE_LOGICAL_OR = 3;
    private static final int PRECEDENCE_NONE = -1;
    
    private final TokenReader reader;
    private final ModifierAndAnnotationParser modifierAndAnnotationParser;
    private final TypeParser typeParser;
    private StatementParser statementParser;
    private DeclarationParser declarationParser;
    
    public ExpressionParser(TokenReader reader, ModifierAndAnnotationParser modifierAndAnnotationParser, TypeParser typeParser) {
        this.reader = reader;
        this.modifierAndAnnotationParser = modifierAndAnnotationParser;
        this.typeParser = typeParser;
    }
    
    public void setStatementParser(StatementParser statementParser) {
        this.statementParser = statementParser;
    }
    
    public void setDeclarationParser(DeclarationParser declarationParser) {
        this.declarationParser = declarationParser;
    }
    
    public Expression parseExpression() {
        return parseAssignmentExpression();
    }
    
    public Expression parseAssignmentExpression() {
        Expression expression = parseTernaryExpression();
        
        if (isAssignmentOperator(reader.peek().getType())) {
            Token token = reader.advance();
            Expression value = parseAssignmentExpression();
            return new AssignmentExpression(token, expression, token.getType(), value);
        }
        
        return expression;
    }
    
    public boolean isAssignmentOperator(TokenType type) {
        return type == TokenType.ASSIGN || type == TokenType.PLUS_ASSIGN ||
               type == TokenType.MINUS_ASSIGN || type == TokenType.STAR_ASSIGN ||
               type == TokenType.SLASH_ASSIGN || type == TokenType.PERCENT_ASSIGN ||
               type == TokenType.AND_ASSIGN || type == TokenType.OR_ASSIGN ||
               type == TokenType.XOR_ASSIGN || type == TokenType.LSHIFT_ASSIGN ||
               type == TokenType.RSHIFT_ASSIGN || type == TokenType.URSHIFT_ASSIGN;
    }
    
    public Expression parseTernaryExpression() {
        Expression expression = parseBinaryExpression(0);
        
        if (reader.match(TokenType.QUESTION)) {
            Expression trueExpr = parseExpression();
            reader.consume(TokenType.COLON, "Expected ':' in ternary expression");
            Expression falseExpr = parseTernaryExpression();
            return new TernaryExpression(expression.getToken(), expression, trueExpr, falseExpr);
        }
        
        return expression;
    }
    
    public Expression parseBinaryExpression(int precedence) {
        Expression left = parseUnaryExpression();
        
        while (true) {
            TokenType op = reader.peek().getType();
            int opPrecedence = getOperatorPrecedence(op);
            
            if (opPrecedence < precedence) break;
            
            if (op == TokenType.INSTANCEOF) {
                reader.advance();
                Type type = typeParser.parseType();
                String patternVariable = null;
                boolean hasPattern = false;
                
                if (reader.check(TokenType.IDENTIFIER)) {
                    patternVariable = reader.advance().getLexeme();
                    hasPattern = true;
                }
                
                left = new InstanceOfExpression(reader.previous(), left, type, patternVariable, hasPattern);
            } else {
                reader.advance();
                Expression right = parseBinaryExpression(opPrecedence + 1);
                left = new BinaryExpression(reader.previous(), left, op, right);
            }
        }
        
        return left;
    }
    
    private int getOperatorPrecedence(TokenType type) {
        switch (type) {
            case STAR:
            case SLASH:
            case PERCENT:
                return PRECEDENCE_MULTIPLICATIVE;
            case PLUS:
            case MINUS:
                return PRECEDENCE_ADDITIVE;
            case LSHIFT:
            case RSHIFT:
            case URSHIFT:
                return PRECEDENCE_SHIFT;
            case LT:
            case GT:
            case LE:
            case GE:
            case INSTANCEOF:
                return PRECEDENCE_RELATIONAL;
            case EQ:
            case NE:
                return PRECEDENCE_EQUALITY;
            case AMPERSAND:
                return PRECEDENCE_BITWISE_AND;
            case CARET:
                return PRECEDENCE_BITWISE_XOR;
            case PIPE:
                return PRECEDENCE_BITWISE_OR;
            case AND:
                return PRECEDENCE_LOGICAL_AND;
            case OR:
                return PRECEDENCE_LOGICAL_OR;
            default:
                return PRECEDENCE_NONE;
        }
    }
    
    public Expression parseUnaryExpression() {
        if (reader.match(TokenType.PLUS)) {
            return new UnaryExpression(reader.previous(), TokenType.PLUS, parseUnaryExpression(), true);
        }
        if (reader.match(TokenType.MINUS)) {
            return new UnaryExpression(reader.previous(), TokenType.MINUS, parseUnaryExpression(), true);
        }
        if (reader.match(TokenType.PLUSPLUS)) {
            return new UnaryExpression(reader.previous(), TokenType.PLUSPLUS, parseUnaryExpression(), true);
        }
        if (reader.match(TokenType.MINUSMINUS)) {
            return new UnaryExpression(reader.previous(), TokenType.MINUSMINUS, parseUnaryExpression(), true);
        }
        if (reader.match(TokenType.NOT)) {
            return new UnaryExpression(reader.previous(), TokenType.NOT, parseUnaryExpression(), true);
        }
        if (reader.match(TokenType.TILDE)) {
            return new UnaryExpression(reader.previous(), TokenType.TILDE, parseUnaryExpression(), true);
        }
        
        Expression expression = parsePostfixExpression();
        
        if (reader.match(TokenType.PLUSPLUS)) {
            return new UnaryExpression(reader.previous(), TokenType.PLUSPLUS, expression, false);
        }
        if (reader.match(TokenType.MINUSMINUS)) {
            return new UnaryExpression(reader.previous(), TokenType.MINUSMINUS, expression, false);
        }
        
        return expression;
    }
    
    public Expression parsePostfixExpression() {
        Expression expression = parsePrimaryExpression();
        
        while (true) {
            if (reader.match(TokenType.DOT)) {
                List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
                
                if (reader.match(TokenType.NEW)) {
                    expression = parseInnerClassCreation(expression);
                } else if (reader.match(TokenType.SUPER)) {
                    SuperExpression superExpr = new SuperExpression(reader.previous(), 
                        expression instanceof IdentifierExpression ? 
                            ((IdentifierExpression) expression).getName() : null);
                    
                    if (reader.match(TokenType.DOT)) {
                        String name = reader.consume(TokenType.IDENTIFIER, "Expected method or field name").getLexeme();
                        
                        if (reader.match(TokenType.LPAREN)) {
                            List<TypeArgument> typeArgs = new ArrayList<>();
                            List<Expression> args = new ArrayList<>();
                            
                            if (!reader.check(TokenType.RPAREN)) {
                                args = parseArgumentList();
                            }
                            reader.consume(TokenType.RPAREN, "Expected ')' after arguments");
                            
                            expression = new MethodInvocationExpression(reader.previous(), superExpr, typeArgs, name, args);
                        } else {
                            expression = new FieldAccessExpression(reader.previous(), superExpr, name);
                        }
                    } else {
                        expression = superExpr;
                    }
                } else if (reader.match(TokenType.THIS)) {
                    String className = null;
                    if (expression instanceof IdentifierExpression) {
                        className = ((IdentifierExpression) expression).getName();
                    } else if (expression instanceof ClassLiteralExpression) {
                        className = ((ClassLiteralExpression) expression).getType().getName();
                    }
                    expression = new ThisExpression(reader.previous(), className);
                } else if (reader.match(TokenType.CLASS)) {
                    String typeName = null;
                    if (expression instanceof IdentifierExpression) {
                        typeName = ((IdentifierExpression) expression).getName();
                    } else if (expression instanceof ClassLiteralExpression) {
                        typeName = ((ClassLiteralExpression) expression).getType().getName();
                    }
                    expression = new ClassLiteralExpression(reader.previous(), 
                        new Type(expression.getToken(), typeName, 
                                new ArrayList<>(), 0, annotations));
                } else {
                    String name = reader.consume(TokenType.IDENTIFIER, "Expected identifier").getLexeme();
                    
                    if (reader.match(TokenType.LPAREN)) {
                        List<TypeArgument> typeArgs = new ArrayList<>();
                        List<Expression> args = new ArrayList<>();
                        
                        if (!reader.check(TokenType.RPAREN)) {
                            args = parseArgumentList();
                        }
                        reader.consume(TokenType.RPAREN, "Expected ')' after arguments");
                        
                        expression = new MethodInvocationExpression(reader.previous(), expression, typeArgs, name, args);
                    } else {
                        expression = new FieldAccessExpression(reader.previous(), expression, name);
                    }
                }
            } else if (reader.match(TokenType.LBRACKET)) {
                Expression index = parseExpression();
                reader.consume(TokenType.RBRACKET, "Expected ']' after index");
                expression = new ArrayAccessExpression(reader.previous(), expression, index);
            } else if (reader.match(TokenType.COLONCOLON)) {
                List<TypeArgument> typeArgs = new ArrayList<>();
                if (reader.match(TokenType.LT)) {
                    typeArgs = typeParser.parseTypeArguments();
                }
                
                String methodName;
                if (reader.match(TokenType.NEW)) {
                    methodName = "new";
                } else {
                    methodName = reader.consume(TokenType.IDENTIFIER, "Expected method name").getLexeme();
                }
                
                expression = new MethodReferenceExpression(reader.previous(), expression, typeArgs, methodName);
            } else if (reader.match(TokenType.PLUSPLUS)) {
                expression = new UnaryExpression(reader.previous(), TokenType.PLUSPLUS, expression, false);
            } else if (reader.match(TokenType.MINUSMINUS)) {
                expression = new UnaryExpression(reader.previous(), TokenType.MINUSMINUS, expression, false);
            } else {
                break;
            }
        }

        return expression;
    }
    
    public Expression parsePostfixExpressionWithPrefix(Expression prefix) {
        Expression expression = prefix;
        
        while (true) {
            if (reader.match(TokenType.DOT)) {
                List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
                
                if (reader.match(TokenType.NEW)) {
                    expression = parseInnerClassCreation(expression);
                } else if (reader.match(TokenType.SUPER)) {
                    SuperExpression superExpr = new SuperExpression(reader.previous(), 
                        expression instanceof IdentifierExpression ? 
                            ((IdentifierExpression) expression).getName() : null);
                    
                    if (reader.match(TokenType.DOT)) {
                        String name = reader.consume(TokenType.IDENTIFIER, "Expected method or field name").getLexeme();
                        
                        if (reader.match(TokenType.LPAREN)) {
                            List<TypeArgument> typeArgs = new ArrayList<>();
                            List<Expression> args = new ArrayList<>();
                            
                            if (!reader.check(TokenType.RPAREN)) {
                                args = parseArgumentList();
                            }
                            reader.consume(TokenType.RPAREN, "Expected ')' after arguments");
                            
                            expression = new MethodInvocationExpression(reader.previous(), superExpr, typeArgs, name, args);
                        } else {
                            expression = new FieldAccessExpression(reader.previous(), superExpr, name);
                        }
                    } else {
                        expression = superExpr;
                    }
                } else if (reader.match(TokenType.THIS)) {
                    String className = null;
                    if (expression instanceof IdentifierExpression) {
                        className = ((IdentifierExpression) expression).getName();
                    } else if (expression instanceof ClassLiteralExpression) {
                        className = ((ClassLiteralExpression) expression).getType().getName();
                    }
                    expression = new ThisExpression(reader.previous(), className);
                } else if (reader.match(TokenType.CLASS)) {
                    String typeName = null;
                    if (expression instanceof IdentifierExpression) {
                        typeName = ((IdentifierExpression) expression).getName();
                    } else if (expression instanceof ClassLiteralExpression) {
                        typeName = ((ClassLiteralExpression) expression).getType().getName();
                    }
                    expression = new ClassLiteralExpression(reader.previous(), 
                        new Type(expression.getToken(), typeName, 
                                new ArrayList<>(), 0, annotations));
                } else {
                    String name = reader.consume(TokenType.IDENTIFIER, "Expected identifier").getLexeme();
                    
                    if (reader.match(TokenType.LPAREN)) {
                        List<TypeArgument> typeArgs = new ArrayList<>();
                        List<Expression> args = new ArrayList<>();
                        
                        if (!reader.check(TokenType.RPAREN)) {
                            args = parseArgumentList();
                        }
                        reader.consume(TokenType.RPAREN, "Expected ')' after arguments");
                        
                        expression = new MethodInvocationExpression(reader.previous(), expression, typeArgs, name, args);
                    } else {
                        expression = new FieldAccessExpression(reader.previous(), expression, name);
                    }
                }
            } else if (reader.match(TokenType.LBRACKET)) {
                Expression index = parseExpression();
                reader.consume(TokenType.RBRACKET, "Expected ']' after index");
                expression = new ArrayAccessExpression(reader.previous(), expression, index);
            } else if (reader.match(TokenType.COLONCOLON)) {
                List<TypeArgument> typeArgs = new ArrayList<>();
                if (reader.match(TokenType.LT)) {
                    typeArgs = typeParser.parseTypeArguments();
                }
                
                String methodName;
                if (reader.match(TokenType.NEW)) {
                    methodName = "new";
                } else {
                    methodName = reader.consume(TokenType.IDENTIFIER, "Expected method name").getLexeme();
                }
                
                expression = new MethodReferenceExpression(reader.previous(), expression, typeArgs, methodName);
            } else if (reader.match(TokenType.PLUSPLUS)) {
                expression = new UnaryExpression(reader.previous(), TokenType.PLUSPLUS, expression, false);
            } else if (reader.match(TokenType.MINUSMINUS)) {
                expression = new UnaryExpression(reader.previous(), TokenType.MINUSMINUS, expression, false);
            } else {
                break;
            }
        }
        return expression;
    }
    
    public Expression parseBinaryExpressionRest(Expression left, int precedence) {
        while (true) {
            TokenType op = reader.peek().getType();
            int opPrecedence = getOperatorPrecedence(op);
            
            if (opPrecedence < precedence) break;
            
            if (op == TokenType.INSTANCEOF) {
                reader.advance();
                Type type = typeParser.parseType();
                left = new InstanceOfExpression(reader.previous(), left, type);
            } else {
                reader.advance();
                Expression right = parseBinaryExpression(opPrecedence + 1);
                left = new BinaryExpression(reader.previous(), left, op, right);
            }
        }
        
        return left;
    }
    
    public Expression parseTernaryExpressionRest(Expression expression) {
        if (reader.match(TokenType.QUESTION)) {
            Expression trueExpr = parseExpression();
            reader.consume(TokenType.COLON, "Expected ':' in ternary expression");
            Expression falseExpr = parseTernaryExpression();
            return new TernaryExpression(expression.getToken(), expression, trueExpr, falseExpr);
        }
        
        return expression;
    }
    
    public Expression parsePrimaryExpression() {
        if (reader.match(TokenType.LPAREN)) {
            return parseParenthesizedOrCastExpression();
        }
        
        if (reader.match(TokenType.NEW)) {
            return parseNewExpression();
        }
        
        if (reader.match(TokenType.THIS)) {
            return new ThisExpression(reader.previous(), null);
        }
        
        if (reader.match(TokenType.SUPER)) {
            return new SuperExpression(reader.previous(), null);
        }
        
        if (reader.match(TokenType.SWITCH)) {
            return parseSwitchExpression();
        }
        
        if (reader.check(TokenType.INT) || reader.check(TokenType.LONG) || reader.check(TokenType.SHORT) ||
            reader.check(TokenType.BYTE) || reader.check(TokenType.CHAR) || reader.check(TokenType.BOOLEAN) ||
            reader.check(TokenType.FLOAT) || reader.check(TokenType.DOUBLE)) {
            Token typeToken = reader.advance();
            String typeName = typeToken.getLexeme();
            
            int arrayDims = 0;
            while (reader.match(TokenType.LBRACKET)) {
                reader.consume(TokenType.RBRACKET, "Expected ']' after '['");
                arrayDims++;
            }
            
            if (reader.match(TokenType.COLONCOLON)) {
                List<TypeArgument> typeArgs = new ArrayList<>();
                if (reader.match(TokenType.LT)) {
                    typeArgs = typeParser.parseTypeArguments();
                }
                
                String methodName;
                if (reader.match(TokenType.NEW)) {
                    methodName = "new";
                } else {
                    methodName = reader.consume(TokenType.IDENTIFIER, "Expected method name").getLexeme();
                }
                
                Type type = new Type(typeToken, typeName, new ArrayList<>(), arrayDims, new ArrayList<>());
                return new MethodReferenceExpression(reader.previous(), 
                    new ClassLiteralExpression(typeToken, type), typeArgs, methodName);
            }
            
            Type type = new Type(typeToken, typeName, new ArrayList<>(), arrayDims, new ArrayList<>());
            return new ClassLiteralExpression(typeToken, type);
        }
        
        if (reader.match(TokenType.INT_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        }
        if (reader.match(TokenType.LONG_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        }
        if (reader.match(TokenType.FLOAT_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        }
        if (reader.match(TokenType.DOUBLE_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        }
        if (reader.match(TokenType.CHAR_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        }
        if (reader.match(TokenType.STRING_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        }
        if (reader.match(TokenType.BOOLEAN_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        }
        if (reader.match(TokenType.NULL_LITERAL)) {
            return new LiteralExpression(reader.previous(), null);
        }
        
        if (reader.match(TokenType.IDENTIFIER)) {
            Token identifier = reader.previous();
            
            if (reader.match(TokenType.ARROW)) {
                List<LambdaExpression.LambdaParameter> params = new ArrayList<>();
                params.add(new LambdaExpression.LambdaParameter(null, identifier.getLexeme()));
                return parseLambdaExpression(params);
            }
            
            if (reader.match(TokenType.LPAREN)) {
                List<TypeArgument> typeArgs = new ArrayList<>();
                List<Expression> args = new ArrayList<>();
                
                if (!reader.check(TokenType.RPAREN)) {
                    args = parseArgumentList();
                }
                reader.consume(TokenType.RPAREN, "Expected ')' after arguments");
                
                return new MethodInvocationExpression(identifier, null, typeArgs, identifier.getLexeme(), args);
            }
            return new IdentifierExpression(identifier, identifier.getLexeme());
        }
        
        throw new Parser.ParseError(reader.peek(), "Expected expression");
    }
    
    public Expression parseParenthesizedOrCastExpression() {
        int save = reader.getCurrentPosition();
        
        if (reader.check(TokenType.RPAREN)) {
            reader.match(TokenType.RPAREN);
            if (reader.match(TokenType.ARROW)) {
                return parseLambdaExpression(new ArrayList<>());
            }
            reader.setCurrentPosition(save);
        }
        
        if (reader.check(TokenType.IDENTIFIER)) {
            int paramSave = reader.getCurrentPosition();
            try {
                List<LambdaExpression.LambdaParameter> params = new ArrayList<>();
                String name = reader.advance().getLexeme();
                params.add(new LambdaExpression.LambdaParameter(null, name));
                
                if (reader.match(TokenType.RPAREN) && reader.match(TokenType.ARROW)) {
                    return parseLambdaExpression(params);
                }
            } catch (Exception e) {
            }
            reader.setCurrentPosition(paramSave);
        }
        
        try {
            Type type = typeParser.parseType();
            if (reader.match(TokenType.RPAREN)) {
                if (reader.match(TokenType.ARROW)) {
                    List<LambdaExpression.LambdaParameter> params = new ArrayList<>();
                    params.add(new LambdaExpression.LambdaParameter(type, null));
                    return parseLambdaExpression(params);
                }
                Expression expression = parseUnaryExpression();
                return new CastExpression(reader.previous(), type, expression);
            }
        } catch (Exception e) {
        }
        
        reader.setCurrentPosition(save);
        
        List<LambdaExpression.LambdaParameter> params = tryParseLambdaParameters();
        if (params != null && reader.match(TokenType.ARROW)) {
            return parseLambdaExpression(params);
        }
        
        reader.setCurrentPosition(save);
        Expression expression = parseExpression();
        reader.consume(TokenType.RPAREN, "Expected ')' after expression");
        return new ParenthesizedExpression(reader.previous(), expression);
    }
    
    private List<LambdaExpression.LambdaParameter> tryParseLambdaParameters() {
        try {
            List<LambdaExpression.LambdaParameter> params = new ArrayList<>();
            
            if (reader.check(TokenType.RPAREN)) {
                return params;
            }
            
            do {
                Type paramType = null;
                if (reader.check(TokenType.INT) || reader.check(TokenType.LONG) || reader.check(TokenType.SHORT) ||
                    reader.check(TokenType.BYTE) || reader.check(TokenType.CHAR) || reader.check(TokenType.BOOLEAN) ||
                    reader.check(TokenType.FLOAT) || reader.check(TokenType.DOUBLE)) {
                    paramType = typeParser.parseType();
                } else if (reader.check(TokenType.IDENTIFIER) && reader.checkNext(TokenType.IDENTIFIER)) {
                    paramType = typeParser.parseType();
                }
                String name = reader.consume(TokenType.IDENTIFIER, "Expected parameter name").getLexeme();
                params.add(new LambdaExpression.LambdaParameter(paramType, name));
            } while (reader.match(TokenType.COMMA));
            
            if (reader.match(TokenType.RPAREN)) {
                return params;
            }
        } catch (Exception e) {
        }
        return null;
    }
    
    public Expression parseLambdaExpression(List<LambdaExpression.LambdaParameter> params) {
        ASTNode body;
        if (reader.match(TokenType.LBRACE)) {
            body = statementParser.parseBlock();
        } else {
            body = parseExpression();
        }
        return new LambdaExpression(reader.previous(), params, body);
    }
    
    public Expression parseNewExpression() {
        Token token = reader.previous();
        
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        Token typeToken = reader.peek();
        
        String name;
        if (reader.match(TokenType.INT)) name = "int";
        else if (reader.match(TokenType.LONG)) name = "long";
        else if (reader.match(TokenType.SHORT)) name = "short";
        else if (reader.match(TokenType.BYTE)) name = "byte";
        else if (reader.match(TokenType.CHAR)) name = "char";
        else if (reader.match(TokenType.BOOLEAN)) name = "boolean";
        else if (reader.match(TokenType.FLOAT)) name = "float";
        else if (reader.match(TokenType.DOUBLE)) name = "double";
        else {
            name = parseQualifiedName();
        }
        
        List<TypeArgument> typeArguments = new ArrayList<>();
        if (reader.match(TokenType.LT)) {
            typeArguments = typeParser.parseTypeArguments();
        }
        
        Type type = new Type(typeToken, name, typeArguments, 0, annotations);
        
        if (reader.match(TokenType.LBRACKET)) {
            return parseNewArrayExpression(token, type);
        }
        
        if (reader.match(TokenType.LBRACE)) {
            ArrayInitializerExpression initializer = parseArrayInitializer();
            return new NewArrayExpression(token, type, new ArrayList<>(), initializer);
        }
        
        return parseNewObjectExpression(token, type);
    }
    
    private Expression parseNewArrayExpression(Token token, Type type) {
        List<Expression> dimensions = new ArrayList<>();
        
        if (!reader.check(TokenType.RBRACKET)) {
            dimensions.add(parseExpression());
        }
        reader.consume(TokenType.RBRACKET, "Expected ']' after dimension");
        
        while (reader.match(TokenType.LBRACKET)) {
            if (!reader.check(TokenType.RBRACKET)) {
                dimensions.add(parseExpression());
            }
            reader.consume(TokenType.RBRACKET, "Expected ']' after dimension");
        }
        
        ArrayInitializerExpression initializer = null;
        if (reader.match(TokenType.LBRACE)) {
            initializer = parseArrayInitializer();
        }
        
        return new NewArrayExpression(token, type, dimensions, initializer);
    }
    
    public Expression parseNewObjectExpression(Token token, Type type) {
        List<TypeArgument> typeArgs = new ArrayList<>();
        if (reader.match(TokenType.LT)) {
            typeArgs = typeParser.parseTypeArguments();
        }
        
        reader.consume(TokenType.LPAREN, "Expected '(' after type");
        List<Expression> arguments = new ArrayList<>();
        if (!reader.check(TokenType.RPAREN)) {
            arguments = parseArgumentList();
        }
        reader.consume(TokenType.RPAREN, "Expected ')' after arguments");
        
        List<ASTNode> anonymousClassBody = null;
        if (reader.match(TokenType.LBRACE)) {
            anonymousClassBody = new ArrayList<>();
            while (!reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
                anonymousClassBody.add(declarationParser.parseClassMember(type.getName()));
            }
            reader.consume(TokenType.RBRACE, "Expected '}' after anonymous class body");
        }
        
        return new NewObjectExpression(token, type, typeArgs, arguments, anonymousClassBody);
    }
    
    public Expression parseElementValue() {
        if (reader.match(TokenType.AT)) {
            return modifierAndAnnotationParser.parseAnnotation();
        }
        
        if (reader.match(TokenType.LBRACE)) {
            return modifierAndAnnotationParser.parseAnnotationArrayInitializer();
        }
        
        return parseExpression();
    }
    
    public Expression parseInnerClassCreation(Expression target) {
        Token token = reader.previous();
        Type type = typeParser.parseType();
        
        List<TypeArgument> typeArgs = new ArrayList<>();
        if (reader.match(TokenType.LT)) {
            typeArgs = typeParser.parseTypeArguments();
        }
        
        reader.consume(TokenType.LPAREN, "Expected '(' after type");
        List<Expression> arguments = new ArrayList<>();
        if (!reader.check(TokenType.RPAREN)) {
            arguments = parseArgumentList();
        }
        reader.consume(TokenType.RPAREN, "Expected ')' after arguments");
        
        return new NewObjectExpression(token, type, typeArgs, arguments, null);
    }
    
    public ArrayInitializerExpression parseArrayInitializer() {
        Token token = reader.previous();
        List<Expression> elements = new ArrayList<>();
        
        if (!reader.check(TokenType.RBRACE)) {
            do {
                if (reader.match(TokenType.LBRACE)) {
                    elements.add(parseArrayInitializer());
                } else {
                    elements.add(parseExpression());
                }
            } while (reader.match(TokenType.COMMA));
        }
        
        reader.consume(TokenType.RBRACE, "Expected '}' after array initializer");
        return new ArrayInitializerExpression(token, elements);
    }
    
    public Expression parseSwitchExpression() {
        Token token = reader.previous();
        reader.consume(TokenType.LPAREN, "Expected '(' after 'switch'");
        Expression selector = parseExpression();
        reader.consume(TokenType.RPAREN, "Expected ')' after selector");
        reader.consume(TokenType.LBRACE, "Expected '{' after switch");
        
        List<SwitchExpression.SwitchCase> cases = new ArrayList<>();
        
        while (!reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
            List<CaseLabel> labels = new ArrayList<>();
            
            if (reader.match(TokenType.DEFAULT)) {
                labels.add(new CaseLabel(reader.previous(), true, new ArrayList<>()));
            } else {
                reader.consume(TokenType.CASE, "Expected 'case' or 'default'");
                List<Expression> values = new ArrayList<>();
                
                do {
                    values.add(parseLiteralOrIdentifier());
                } while (reader.match(TokenType.COMMA));
                
                labels.add(new CaseLabel(token, false, values));
            }
            
            boolean isArrow = reader.match(TokenType.ARROW);
            ASTNode body;
            
            if (isArrow) {
                if (reader.check(TokenType.LBRACE)) {
                    reader.match(TokenType.LBRACE);
                    body = statementParser.parseBlock();
                } else if (!reader.check(TokenType.CASE) && !reader.check(TokenType.DEFAULT) &&
                          !reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
                    body = parseExpression();
                } else {
                    body = new LiteralExpression(reader.peek(), null);
                }
                reader.match(TokenType.SEMICOLON);
            } else {
                reader.consume(TokenType.COLON, "Expected ':' in switch expression");
                List<Statement> statements = new ArrayList<>();
                while (!reader.check(TokenType.CASE) && !reader.check(TokenType.DEFAULT) && 
                       !reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
                    statements.add(statementParser.parseStatement());
                }
                body = new BlockStatement(token, statements);
            }
            
            cases.add(new SwitchExpression.SwitchCase(labels, body, isArrow));
        }
        
        reader.consume(TokenType.RBRACE, "Expected '}' after switch");
        return new SwitchExpression(token, selector, cases);
    }
    
    private CaseLabel parseCaseLabelForExpression() {
        Token token = reader.peek();
        
        if (reader.match(TokenType.DEFAULT)) {
            return new CaseLabel(token, true, new ArrayList<>());
        }
        
        reader.consume(TokenType.CASE, "Expected 'case' or 'default'");
        List<Expression> values = new ArrayList<>();
        
        if (reader.check(TokenType.NULL)) {
            reader.advance();
            values.add(new LiteralExpression(reader.previous(), null));
        } else {
            values.add(parseLiteralOrIdentifier());
        }
        
        return new CaseLabel(token, false, values);
    }
    
    private Expression parseLiteralOrIdentifier() {
        if (reader.match(TokenType.INT_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        } else if (reader.match(TokenType.LONG_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        } else if (reader.match(TokenType.FLOAT_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        } else if (reader.match(TokenType.DOUBLE_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        } else if (reader.match(TokenType.CHAR_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        } else if (reader.match(TokenType.STRING_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        } else if (reader.match(TokenType.BOOLEAN_LITERAL)) {
            return new LiteralExpression(reader.previous(), reader.previous().getLiteral());
        } else if (reader.check(TokenType.IDENTIFIER)) {
            Token token = reader.advance();
            return new IdentifierExpression(token, token.getLexeme());
        }
        
        return parsePrimaryExpression();
    }
    
    public List<Expression> parseArgumentList() {
        List<Expression> arguments = new ArrayList<>();
        
        do {
            arguments.add(parseExpression());
        } while (reader.match(TokenType.COMMA));
        
        return arguments;
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
