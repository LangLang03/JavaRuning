package cn.langlang.javainterpreter.parser;

import cn.langlang.javainterpreter.lexer.Token;
import cn.langlang.javainterpreter.lexer.TokenType;
import cn.langlang.javainterpreter.ast.base.ASTNode;
import cn.langlang.javainterpreter.ast.expression.*;
import cn.langlang.javainterpreter.ast.misc.*;
import cn.langlang.javainterpreter.ast.statement.BlockStatement;
import cn.langlang.javainterpreter.ast.type.*;
import cn.langlang.javainterpreter.ast.declaration.*;
import java.util.*;

public class ExpressionParser {
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
                left = new InstanceOfExpression(reader.previous(), left, type);
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
                return 12;
            case PLUS:
            case MINUS:
                return 11;
            case LSHIFT:
            case RSHIFT:
            case URSHIFT:
                return 10;
            case LT:
            case GT:
            case LE:
            case GE:
            case INSTANCEOF:
                return 9;
            case EQ:
            case NE:
                return 8;
            case AMPERSAND:
                return 7;
            case CARET:
                return 6;
            case PIPE:
                return 5;
            case AND:
                return 4;
            case OR:
                return 3;
            default:
                return -1;
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
            if (reader.match(TokenType.GT)) {
                typeArguments = new ArrayList<>();
            } else {
                typeArguments = typeParser.parseTypeArguments();
            }
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
