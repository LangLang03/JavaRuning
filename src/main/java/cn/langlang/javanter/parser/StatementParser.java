package cn.langlang.javanter.parser;

import cn.langlang.javanter.lexer.Token;
import cn.langlang.javanter.lexer.TokenType;
import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.declaration.*;
import cn.langlang.javanter.ast.expression.*;
import cn.langlang.javanter.ast.misc.*;
import cn.langlang.javanter.ast.statement.*;
import cn.langlang.javanter.ast.type.*;
import java.util.*;

public class StatementParser {
    private final TokenReader reader;
    private final ModifierAndAnnotationParser modifierAndAnnotationParser;
    private final TypeParser typeParser;
    private ExpressionParser expressionParser;
    
    public StatementParser(TokenReader reader, ModifierAndAnnotationParser modifierAndAnnotationParser, TypeParser typeParser) {
        this.reader = reader;
        this.modifierAndAnnotationParser = modifierAndAnnotationParser;
        this.typeParser = typeParser;
    }
    
    public void setExpressionParser(ExpressionParser expressionParser) {
        this.expressionParser = expressionParser;
    }
    
    public BlockStatement parseBlock() {
        Token token = reader.previous();
        List<Statement> statements = new ArrayList<>();
        
        while (!reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
            statements.add(parseStatement());
        }
        
        reader.consume(TokenType.RBRACE, "Expected '}' after block");
        return new BlockStatement(token, statements);
    }
    
    public Statement parseStatement() {
        if (reader.match(TokenType.LBRACE)) {
            return parseBlock();
        }
        
        if (reader.match(TokenType.IF)) {
            return parseIfStatement();
        }
        
        if (reader.match(TokenType.WHILE)) {
            return parseWhileStatement();
        }
        
        if (reader.match(TokenType.DO)) {
            return parseDoStatement();
        }
        
        if (reader.match(TokenType.FOR)) {
            return parseForStatement();
        }
        
        if (reader.match(TokenType.SWITCH)) {
            return parseSwitchStatement();
        }
        
        if (reader.match(TokenType.RETURN)) {
            return parseReturnStatement();
        }
        
        if (reader.match(TokenType.THROW)) {
            return parseThrowStatement();
        }
        
        if (reader.match(TokenType.TRY)) {
            return parseTryStatement();
        }
        
        if (reader.match(TokenType.SYNCHRONIZED)) {
            return parseSynchronizedStatement();
        }
        
        if (reader.match(TokenType.ASSERT)) {
            return parseAssertStatement();
        }
        
        if (reader.match(TokenType.BREAK)) {
            return parseBreakStatement();
        }
        
        if (reader.match(TokenType.CONTINUE)) {
            return parseContinueStatement();
        }
        
        if (reader.match(TokenType.SEMICOLON)) {
            return new EmptyStatement(reader.previous());
        }
        
        if (reader.match(TokenType.THIS)) {
            if (reader.match(TokenType.LPAREN)) {
                List<Expression> args = new ArrayList<>();
                if (!reader.check(TokenType.RPAREN)) {
                    args = expressionParser.parseArgumentList();
                }
                reader.consume(TokenType.RPAREN, "Expected ')' after this call");
                reader.consume(TokenType.SEMICOLON, "Expected ';' after this call");
                return new ExpressionStatement(reader.previous(), 
                    new MethodInvocationExpression(reader.previous(), new ThisExpression(reader.previous(), null), 
                        new ArrayList<>(), "this", args));
            } else {
                return parseExpressionStatementWithPrefix(new ThisExpression(reader.previous(), null));
            }
        }
        
        if (checkIdentifier()) {
            if (reader.checkNext(TokenType.COLON)) {
                return parseLabelStatement();
            }
            
            if (isLocalVariableDeclaration()) {
                return parseLocalVariableDeclaration();
            }
        }
        
        if (reader.check(TokenType.AT) && isLocalVariableDeclaration()) {
            return parseLocalVariableDeclaration();
        }
        
        if (isLocalVariableDeclaration()) {
            return parseLocalVariableDeclaration();
        }
        
        if (reader.check(TokenType.CLASS)) {
            TypeDeclaration typeDecl = parseTypeDeclaration();
            if (typeDecl instanceof ClassDeclaration) {
                return new LocalClassDeclarationStatement(typeDecl.getToken(), (ClassDeclaration) typeDecl);
            }
        }
        
        return parseExpressionStatement();
    }
    
    public ExpressionStatement parseExpressionStatementWithPrefix(Expression prefix) {
        Expression expression = expressionParser.parsePostfixExpressionWithPrefix(prefix);
        expression = expressionParser.parseBinaryExpressionRest(expression, 0);
        expression = expressionParser.parseTernaryExpressionRest(expression);
        
        if (expressionParser.isAssignmentOperator(reader.peek().getType())) {
            Token token = reader.advance();
            Expression value = expressionParser.parseAssignmentExpression();
            expression = new AssignmentExpression(token, expression, token.getType(), value);
        }
        
        reader.consume(TokenType.SEMICOLON, "Expected ';' after expression");
        return new ExpressionStatement(expression.getToken(), expression);
    }
    
    public boolean isLocalVariableDeclaration() {
        int save = reader.getCurrentPosition();
        try {
            modifierAndAnnotationParser.parseAnnotations();
            modifierAndAnnotationParser.parseModifiers();
            
            if (reader.check(TokenType.INT) || reader.check(TokenType.LONG) || reader.check(TokenType.SHORT) ||
                reader.check(TokenType.BYTE) || reader.check(TokenType.CHAR) || reader.check(TokenType.BOOLEAN) ||
                reader.check(TokenType.FLOAT) || reader.check(TokenType.DOUBLE)) {
                return true;
            }
            
            if (reader.check(TokenType.IDENTIFIER)) {
                reader.advance();
                while (reader.match(TokenType.DOT)) {
                    if (!reader.match(TokenType.IDENTIFIER)) return false;
                }
                
                if (reader.match(TokenType.LT)) {
                    int depth = 1;
                    while (depth > 0 && !reader.check(TokenType.EOF)) {
                        int closed = consumeClosingAngleBrackets();
                        if (closed > 0) {
                            depth -= closed;
                        } else if (reader.match(TokenType.LT)) {
                            depth++;
                        } else {
                            reader.advance();
                        }
                    }
                }
                
                while (true) {
                    modifierAndAnnotationParser.parseAnnotations();
                    if (reader.match(TokenType.LBRACKET)) {
                        modifierAndAnnotationParser.parseAnnotations();
                        if (!reader.match(TokenType.RBRACKET)) return false;
                    } else {
                        break;
                    }
                }
                
                return reader.check(TokenType.IDENTIFIER);
            }
            
            return false;
        } finally {
            reader.setCurrentPosition(save);
        }
    }
    
    private int consumeClosingAngleBrackets() {
        TokenType type = reader.peek().getType();
        if (type == TokenType.GT) {
            reader.advance();
            return 1;
        } else if (type == TokenType.RSHIFT) {
            reader.advance();
            return 2;
        } else if (type == TokenType.URSHIFT) {
            reader.advance();
            return 3;
        }
        return 0;
    }
    
    public boolean isForEachLoop() {
        int save = reader.getCurrentPosition();
        try {
            modifierAndAnnotationParser.parseAnnotations();
            modifierAndAnnotationParser.parseModifiers();
            
            if (reader.check(TokenType.INT) || reader.check(TokenType.LONG) || reader.check(TokenType.SHORT) ||
                reader.check(TokenType.BYTE) || reader.check(TokenType.CHAR) || reader.check(TokenType.BOOLEAN) ||
                reader.check(TokenType.FLOAT) || reader.check(TokenType.DOUBLE)) {
                reader.advance();
                while (reader.match(TokenType.LBRACKET)) {
                    if (!reader.match(TokenType.RBRACKET)) return false;
                }
                if (reader.check(TokenType.IDENTIFIER) && reader.checkNext(TokenType.COLON)) {
                    return true;
                }
                return false;
            }
            
            if (reader.check(TokenType.IDENTIFIER)) {
                reader.advance();
                while (reader.check(TokenType.DOT) && reader.checkNext(TokenType.IDENTIFIER)) {
                    reader.match(TokenType.DOT);
                    reader.advance();
                }
                
                if (reader.match(TokenType.LT)) {
                    int depth = 1;
                    while (depth > 0 && !reader.check(TokenType.EOF)) {
                        int closed = consumeClosingAngleBrackets();
                        if (closed > 0) {
                            depth -= closed;
                        } else if (reader.match(TokenType.LT)) {
                            depth++;
                        } else {
                            reader.advance();
                        }
                    }
                }
                
                while (reader.match(TokenType.LBRACKET)) {
                    if (!reader.match(TokenType.RBRACKET)) return false;
                }
                
                if (reader.check(TokenType.IDENTIFIER) && reader.checkNext(TokenType.COLON)) {
                    return true;
                }
            }
            
            return false;
        } finally {
            reader.setCurrentPosition(save);
        }
    }
    
    public LocalVariableDeclaration parseLocalVariableDeclaration() {
        Token token = reader.peek();
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        int modifiers = modifierAndAnnotationParser.parseModifiers();
        Type type = typeParser.parseType();
        
        List<LocalVariableDeclaration.VariableDeclarator> declarators = new ArrayList<>();
        
        do {
            String name = reader.consume(TokenType.IDENTIFIER, "Expected variable name").getLexeme();
            
            int arrayDims = 0;
            while (reader.match(TokenType.LBRACKET)) {
                reader.consume(TokenType.RBRACKET, "Expected ']' after '['");
                arrayDims++;
            }
            
            Expression initializer = null;
            if (reader.match(TokenType.ASSIGN)) {
                if (reader.match(TokenType.LBRACE)) {
                    initializer = expressionParser.parseArrayInitializer();
                } else {
                    initializer = expressionParser.parseExpression();
                }
            }
            
            declarators.add(new LocalVariableDeclaration.VariableDeclarator(name, initializer, arrayDims));
        } while (reader.match(TokenType.COMMA));
        
        reader.consume(TokenType.SEMICOLON, "Expected ';' after variable declaration");
        
        return new LocalVariableDeclaration(token, modifiers, type, declarators, annotations);
    }
    
    public IfStatement parseIfStatement() {
        Token token = reader.previous();
        reader.consume(TokenType.LPAREN, "Expected '(' after 'if'");
        Expression condition = expressionParser.parseExpression();
        reader.consume(TokenType.RPAREN, "Expected ')' after condition");
        
        Statement thenStatement = parseStatement();
        Statement elseStatement = null;
        
        if (reader.match(TokenType.ELSE)) {
            elseStatement = parseStatement();
        }
        
        return new IfStatement(token, condition, thenStatement, elseStatement);
    }
    
    public WhileStatement parseWhileStatement() {
        Token token = reader.previous();
        reader.consume(TokenType.LPAREN, "Expected '(' after 'while'");
        Expression condition = expressionParser.parseExpression();
        reader.consume(TokenType.RPAREN, "Expected ')' after condition");
        
        Statement body = parseStatement();
        return new WhileStatement(token, condition, body);
    }
    
    public DoStatement parseDoStatement() {
        Token token = reader.previous();
        Statement body = parseStatement();
        
        reader.consume(TokenType.WHILE, "Expected 'while' after do body");
        reader.consume(TokenType.LPAREN, "Expected '(' after 'while'");
        Expression condition = expressionParser.parseExpression();
        reader.consume(TokenType.RPAREN, "Expected ')' after condition");
        reader.consume(TokenType.SEMICOLON, "Expected ';' after do-while");
        
        return new DoStatement(token, body, condition);
    }
    
    public Statement parseForStatement() {
        Token token = reader.previous();
        reader.consume(TokenType.LPAREN, "Expected '(' after 'for'");
        
        if (isForEachLoop()) {
            return parseForEachStatement(token);
        }
        
        Statement init = null;
        if (!reader.check(TokenType.SEMICOLON)) {
            if (isLocalVariableDeclaration()) {
                init = parseLocalVariableDeclaration();
            } else {
                init = parseExpressionStatement();
            }
        } else {
            reader.consume(TokenType.SEMICOLON, "Expected ';' after for init");
        }
        
        Expression condition = null;
        if (!reader.check(TokenType.SEMICOLON)) {
            condition = expressionParser.parseExpression();
        }
        reader.consume(TokenType.SEMICOLON, "Expected ';' after for condition");
        
        Expression update = null;
        if (!reader.check(TokenType.RPAREN)) {
            update = expressionParser.parseExpression();
        }
        reader.consume(TokenType.RPAREN, "Expected ')' after for clauses");
        
        Statement body = parseStatement();
        
        return new ForStatement(token, init, condition, update, body);
    }
    
    public ForEachStatement parseForEachStatement(Token token) {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        int modifiers = modifierAndAnnotationParser.parseModifiers();
        Type type = typeParser.parseType();
        
        String name = reader.consume(TokenType.IDENTIFIER, "Expected variable name").getLexeme();
        LocalVariableDeclaration.VariableDeclarator declarator = 
            new LocalVariableDeclaration.VariableDeclarator(name, null, 0);
        LocalVariableDeclaration variable = new LocalVariableDeclaration(
            token, modifiers, type, Arrays.asList(declarator), annotations);
        
        reader.consume(TokenType.COLON, "Expected ':' in for-each");
        Expression iterable = expressionParser.parseExpression();
        reader.consume(TokenType.RPAREN, "Expected ')' after for-each");
        
        Statement body = parseStatement();
        return new ForEachStatement(token, variable, iterable, body);
    }
    
    public SwitchStatement parseSwitchStatement() {
        Token token = reader.previous();
        reader.consume(TokenType.LPAREN, "Expected '(' after 'switch'");
        Expression expression = expressionParser.parseExpression();
        reader.consume(TokenType.RPAREN, "Expected ')' after expression");
        reader.consume(TokenType.LBRACE, "Expected '{' after switch");
        
        List<SwitchStatement.SwitchCase> cases = new ArrayList<>();
        
        while (!reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
            CaseLabel label = parseCaseLabel();
            List<Statement> statements = new ArrayList<>();
            
            while (!reader.check(TokenType.CASE) && !reader.check(TokenType.DEFAULT) && 
                   !reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
                statements.add(parseStatement());
            }
            
            cases.add(new SwitchStatement.SwitchCase(label, statements));
        }
        
        reader.consume(TokenType.RBRACE, "Expected '}' after switch body");
        return new SwitchStatement(token, expression, cases);
    }
    
    public CaseLabel parseCaseLabel() {
        Token token = reader.peek();
        
        if (reader.match(TokenType.DEFAULT)) {
            reader.consume(TokenType.COLON, "Expected ':' after 'default'");
            return new CaseLabel(token, true, new ArrayList<>());
        }
        
        reader.consume(TokenType.CASE, "Expected 'case' or 'default'");
        List<Expression> values = new ArrayList<>();
        
        do {
            values.add(expressionParser.parseExpression());
        } while (reader.match(TokenType.COMMA));
        
        reader.consume(TokenType.COLON, "Expected ':' after case value");
        return new CaseLabel(token, false, values);
    }
    
    public ReturnStatement parseReturnStatement() {
        Token token = reader.previous();
        Expression expression = null;
        
        if (!reader.check(TokenType.SEMICOLON)) {
            expression = expressionParser.parseExpression();
        }
        
        reader.consume(TokenType.SEMICOLON, "Expected ';' after return");
        return new ReturnStatement(token, expression);
    }
    
    public ThrowStatement parseThrowStatement() {
        Token token = reader.previous();
        Expression expression = expressionParser.parseExpression();
        reader.consume(TokenType.SEMICOLON, "Expected ';' after throw");
        return new ThrowStatement(token, expression);
    }
    
    public TryStatement parseTryStatement() {
        Token token = reader.previous();
        
        List<TryStatement.ResourceDeclaration> resources = new ArrayList<>();
        if (reader.match(TokenType.LPAREN)) {
            resources = parseResources();
            reader.consume(TokenType.RPAREN, "Expected ')' after resources");
        }
        
        reader.consume(TokenType.LBRACE, "Expected '{' after try");
        BlockStatement tryBlock = parseBlock();
        
        List<CatchClause> catchClauses = new ArrayList<>();
        while (reader.match(TokenType.CATCH)) {
            catchClauses.add(parseCatchClause());
        }
        
        BlockStatement finallyBlock = null;
        if (reader.match(TokenType.FINALLY)) {
            reader.consume(TokenType.LBRACE, "Expected '{' after 'finally'");
            finallyBlock = parseBlock();
        }
        
        return new TryStatement(token, resources, tryBlock, catchClauses, finallyBlock);
    }
    
    public List<TryStatement.ResourceDeclaration> parseResources() {
        List<TryStatement.ResourceDeclaration> resources = new ArrayList<>();
        
        do {
            resources.add(parseResource());
        } while (reader.match(TokenType.SEMICOLON));
        
        return resources;
    }
    
    public TryStatement.ResourceDeclaration parseResource() {
        Token token = reader.peek();
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        int modifiers = modifierAndAnnotationParser.parseModifiers();
        Type type = typeParser.parseType();
        String name = reader.consume(TokenType.IDENTIFIER, "Expected resource name").getLexeme();
        
        Expression expression = null;
        if (reader.match(TokenType.ASSIGN)) {
            expression = expressionParser.parseExpression();
        }
        
        return new TryStatement.ResourceDeclaration(token, type, name, expression);
    }
    
    public CatchClause parseCatchClause() {
        Token token = reader.previous();
        reader.consume(TokenType.LPAREN, "Expected '(' after 'catch'");
        
        List<Type> exceptionTypes = new ArrayList<>();
        exceptionTypes.add(typeParser.parseType());
        
        while (reader.match(TokenType.PIPE)) {
            exceptionTypes.add(typeParser.parseType());
        }
        
        String exceptionName = reader.consume(TokenType.IDENTIFIER, "Expected exception name").getLexeme();
        reader.consume(TokenType.RPAREN, "Expected ')' after catch parameter");
        
        reader.consume(TokenType.LBRACE, "Expected '{' after catch");
        BlockStatement body = parseBlock();
        
        return new CatchClause(token, exceptionTypes, exceptionName, body);
    }
    
    public SynchronizedStatement parseSynchronizedStatement() {
        Token token = reader.previous();
        reader.consume(TokenType.LPAREN, "Expected '(' after 'synchronized'");
        Expression lock = expressionParser.parseExpression();
        reader.consume(TokenType.RPAREN, "Expected ')' after lock");
        
        reader.consume(TokenType.LBRACE, "Expected '{' after synchronized");
        BlockStatement body = parseBlock();
        
        return new SynchronizedStatement(token, lock, body);
    }
    
    public AssertStatement parseAssertStatement() {
        Token token = reader.previous();
        Expression condition = expressionParser.parseExpression();
        Expression message = null;
        
        if (reader.match(TokenType.COLON)) {
            message = expressionParser.parseExpression();
        }
        
        reader.consume(TokenType.SEMICOLON, "Expected ';' after assert");
        return new AssertStatement(token, condition, message);
    }
    
    public BreakStatement parseBreakStatement() {
        Token token = reader.previous();
        String label = null;
        
        if (reader.check(TokenType.IDENTIFIER)) {
            label = reader.advance().getLexeme();
        }
        
        reader.consume(TokenType.SEMICOLON, "Expected ';' after break");
        return new BreakStatement(token, label);
    }
    
    public ContinueStatement parseContinueStatement() {
        Token token = reader.previous();
        String label = null;
        
        if (reader.check(TokenType.IDENTIFIER)) {
            label = reader.advance().getLexeme();
        }
        
        reader.consume(TokenType.SEMICOLON, "Expected ';' after continue");
        return new ContinueStatement(token, label);
    }
    
    public LabelStatement parseLabelStatement() {
        Token token = reader.peek();
        String label = reader.consume(TokenType.IDENTIFIER, "Expected label name").getLexeme();
        reader.consume(TokenType.COLON, "Expected ':' after label");
        
        Statement statement = parseStatement();
        return new LabelStatement(token, label, statement);
    }
    
    public ExpressionStatement parseExpressionStatement() {
        Token token = reader.peek();
        Expression expression = expressionParser.parseExpression();
        reader.consume(TokenType.SEMICOLON, "Expected ';' after expression");
        return new ExpressionStatement(token, expression);
    }
    
    private boolean checkIdentifier() {
        return reader.check(TokenType.IDENTIFIER);
    }
    
    private TypeDeclaration parseTypeDeclaration() {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        int modifiers = modifierAndAnnotationParser.parseModifiers();
        
        if (reader.match(TokenType.CLASS)) {
            return parseClassDeclaration(annotations, modifiers);
        } else if (reader.match(TokenType.INTERFACE)) {
            return parseInterfaceDeclaration(annotations, modifiers);
        } else if (reader.match(TokenType.ENUM)) {
            return parseEnumDeclaration(annotations, modifiers);
        } else if (reader.match(TokenType.AT)) {
            reader.consume(TokenType.INTERFACE, "Expected 'interface' after '@'");
            return parseAnnotationDeclaration(annotations, modifiers);
        }
        
        throw new Parser.ParseError(reader.peek(), "Expected class, interface, enum, or annotation declaration");
    }
    
    private ClassDeclaration parseClassDeclaration(List<Annotation> annotations, int modifiers) {
        Token token = reader.previous();
        String name = reader.consume(TokenType.IDENTIFIER, "Expected class name").getLexeme();
        
        List<TypeParameter> typeParameters = new ArrayList<>();
        if (reader.match(TokenType.LT)) {
            typeParameters = typeParser.parseTypeParameters();
        }
        
        Type superClass = null;
        if (reader.match(TokenType.EXTENDS)) {
            superClass = typeParser.parseType();
        }
        
        List<Type> interfaces = new ArrayList<>();
        if (reader.match(TokenType.IMPLEMENTS)) {
            interfaces = typeParser.parseTypeList();
        }
        
        reader.consume(TokenType.LBRACE, "Expected '{' before class body");
        
        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();
        List<ConstructorDeclaration> constructors = new ArrayList<>();
        List<InitializerBlock> initializers = new ArrayList<>();
        List<TypeDeclaration> nestedTypes = new ArrayList<>();
        
        while (!reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
            ASTNode member = parseClassMember(name);
            if (member instanceof FieldDeclaration) {
                fields.add((FieldDeclaration) member);
            } else if (member instanceof MethodDeclaration) {
                methods.add((MethodDeclaration) member);
            } else if (member instanceof ConstructorDeclaration) {
                constructors.add((ConstructorDeclaration) member);
            } else if (member instanceof InitializerBlock) {
                initializers.add((InitializerBlock) member);
            } else if (member instanceof TypeDeclaration) {
                nestedTypes.add((TypeDeclaration) member);
            }
        }
        
        reader.consume(TokenType.RBRACE, "Expected '}' after class body");
        
        return new ClassDeclaration(token, name, modifiers, annotations, typeParameters,
                                   superClass, interfaces, fields, methods, constructors,
                                   initializers, nestedTypes);
    }
    
    private ASTNode parseClassMember(String className) {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        
        if (reader.match(TokenType.LBRACE)) {
            return parseInitializerBlock(false, annotations);
        }
        
        int memberModifiers = 0;
        if (reader.match(TokenType.STATIC)) {
            memberModifiers |= Modifier.STATIC;
            if (reader.match(TokenType.LBRACE)) {
                return parseInitializerBlock(true, annotations);
            }
        }
        
        memberModifiers |= modifierAndAnnotationParser.parseModifiers();
        
        if (reader.check(TokenType.CLASS) || reader.check(TokenType.INTERFACE) || 
            reader.check(TokenType.ENUM) || reader.check(TokenType.AT)) {
            return parseTypeDeclaration();
        }
        
        if (reader.checkIdentifier() && reader.checkNext(TokenType.LPAREN)) {
            return parseConstructorDeclaration(className, memberModifiers, annotations);
        }
        
        List<TypeParameter> typeParameters = new ArrayList<>();
        if (reader.match(TokenType.LT)) {
            typeParameters = typeParser.parseTypeParameters();
        }
        
        Type type = typeParser.parseType();
        String memberName = reader.consume(TokenType.IDENTIFIER, "Expected member name").getLexeme();
        
        if (reader.match(TokenType.LPAREN)) {
            return parseMethodDeclaration(type, memberName, memberModifiers, annotations, typeParameters);
        }
        
        return parseFieldDeclaration(type, memberName, memberModifiers, annotations);
    }
    
    private InitializerBlock parseInitializerBlock(boolean isStatic, List<Annotation> annotations) {
        Token token = reader.previous();
        BlockStatement body = parseBlock();
        return new InitializerBlock(token, isStatic, body);
    }
    
    private FieldDeclaration parseFieldDeclaration(Type type, String name, 
                                                   int modifiers, List<Annotation> annotations) {
        Token token = reader.previous();
        Expression initializer = null;
        
        if (reader.match(TokenType.ASSIGN)) {
            initializer = expressionParser.parseExpression();
        }
        
        reader.consume(TokenType.SEMICOLON, "Expected ';' after field declaration");
        
        return new FieldDeclaration(token, modifiers, type, name, initializer, annotations);
    }
    
    private MethodDeclaration parseMethodDeclaration(Type returnType, String name,
                                                     int modifiers, List<Annotation> annotations,
                                                     List<TypeParameter> typeParameters) {
        Token token = reader.previous();
        
        List<ParameterDeclaration> parameters = parseParameters();
        boolean isVarArgs = !parameters.isEmpty() && parameters.get(parameters.size() - 1).isVarArgs();
        
        List<Type> exceptionTypes = new ArrayList<>();
        if (reader.match(TokenType.THROWS)) {
            exceptionTypes = typeParser.parseTypeList();
        }
        
        boolean isDefault = (modifiers & Modifier.DEFAULT) != 0;
        
        BlockStatement body = null;
        if (reader.match(TokenType.LBRACE)) {
            body = parseBlock();
        } else if (!isDefault) {
            reader.consume(TokenType.SEMICOLON, "Expected ';' or method body");
        }
        
        return new MethodDeclaration(token, modifiers, typeParameters, returnType, name,
                                    parameters, isVarArgs, exceptionTypes, body, annotations, isDefault);
    }
    
    private ConstructorDeclaration parseConstructorDeclaration(String className,
                                                               int modifiers, List<Annotation> annotations) {
        Token token = reader.previous();
        
        List<TypeParameter> typeParameters = new ArrayList<>();
        if (reader.match(TokenType.LT)) {
            typeParameters = typeParser.parseTypeParameters();
        }
        
        reader.consume(TokenType.IDENTIFIER, "Expected constructor name");
        reader.consume(TokenType.LPAREN, "Expected '(' after constructor name");
        
        List<ParameterDeclaration> parameters = parseParameters();
        
        List<Type> exceptionTypes = new ArrayList<>();
        if (reader.match(TokenType.THROWS)) {
            exceptionTypes = typeParser.parseTypeList();
        }
        
        reader.consume(TokenType.LBRACE, "Expected '{' before constructor body");
        BlockStatement body = parseBlock();
        
        return new ConstructorDeclaration(token, modifiers, typeParameters, className,
                                         parameters, exceptionTypes, body, annotations);
    }
    
    private List<ParameterDeclaration> parseParameters() {
        List<ParameterDeclaration> parameters = new ArrayList<>();
        
        if (!reader.check(TokenType.RPAREN)) {
            do {
                parameters.add(parseParameter());
            } while (reader.match(TokenType.COMMA));
        }
        
        reader.consume(TokenType.RPAREN, "Expected ')' after parameters");
        return parameters;
    }
    
    private ParameterDeclaration parseParameter() {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        int modifiers = modifierAndAnnotationParser.parseModifiers();
        
        Type type = typeParser.parseType();
        boolean isVarArgs = reader.match(TokenType.ELLIPSIS);
        
        String name = reader.consume(TokenType.IDENTIFIER, "Expected parameter name").getLexeme();
        
        return new ParameterDeclaration(reader.previous(), modifiers, type, name, isVarArgs, annotations);
    }
    
    private InterfaceDeclaration parseInterfaceDeclaration(List<Annotation> annotations, int modifiers) {
        Token token = reader.previous();
        String name = reader.consume(TokenType.IDENTIFIER, "Expected interface name").getLexeme();
        
        List<TypeParameter> typeParameters = new ArrayList<>();
        if (reader.match(TokenType.LT)) {
            typeParameters = typeParser.parseTypeParameters();
        }
        
        List<Type> extendsInterfaces = new ArrayList<>();
        if (reader.match(TokenType.EXTENDS)) {
            extendsInterfaces = typeParser.parseTypeList();
        }
        
        reader.consume(TokenType.LBRACE, "Expected '{' before interface body");
        
        List<MethodDeclaration> methods = new ArrayList<>();
        List<FieldDeclaration> constants = new ArrayList<>();
        List<TypeDeclaration> nestedTypes = new ArrayList<>();
        
        while (!reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
            ASTNode member = parseInterfaceMember();
            if (member instanceof MethodDeclaration) {
                methods.add((MethodDeclaration) member);
            } else if (member instanceof FieldDeclaration) {
                constants.add((FieldDeclaration) member);
            } else if (member instanceof TypeDeclaration) {
                nestedTypes.add((TypeDeclaration) member);
            }
        }
        
        reader.consume(TokenType.RBRACE, "Expected '}' after interface body");
        
        return new InterfaceDeclaration(token, name, modifiers, annotations, typeParameters,
                                       extendsInterfaces, methods, constants, nestedTypes);
    }
    
    private ASTNode parseInterfaceMember() {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        int modifiers = modifierAndAnnotationParser.parseModifiers();
        
        if (reader.check(TokenType.CLASS) || reader.check(TokenType.INTERFACE) || 
            reader.check(TokenType.ENUM) || reader.check(TokenType.AT)) {
            return parseTypeDeclaration();
        }
        
        List<TypeParameter> typeParameters = new ArrayList<>();
        if (reader.match(TokenType.LT)) {
            typeParameters = typeParser.parseTypeParameters();
        }
        
        Type type = typeParser.parseType();
        String name = reader.consume(TokenType.IDENTIFIER, "Expected member name").getLexeme();
        
        if (reader.match(TokenType.LPAREN)) {
            return parseMethodDeclaration(type, name, modifiers, annotations, typeParameters);
        }
        
        return parseFieldDeclaration(type, name, modifiers, annotations);
    }
    
    private EnumDeclaration parseEnumDeclaration(List<Annotation> annotations, int modifiers) {
        Token token = reader.previous();
        String name = reader.consume(TokenType.IDENTIFIER, "Expected enum name").getLexeme();
        
        List<Type> interfaces = new ArrayList<>();
        if (reader.match(TokenType.IMPLEMENTS)) {
            interfaces = typeParser.parseTypeList();
        }
        
        reader.consume(TokenType.LBRACE, "Expected '{' before enum body");
        
        List<EnumConstant> constants = new ArrayList<>();
        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();
        List<ConstructorDeclaration> constructors = new ArrayList<>();
        List<TypeDeclaration> nestedTypes = new ArrayList<>();
        
        if (!reader.check(TokenType.RBRACE)) {
            constants = parseEnumConstants();
            
            if (reader.match(TokenType.SEMICOLON)) {
                while (!reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
                    ASTNode member = parseClassMember(name);
                    if (member instanceof FieldDeclaration) {
                        fields.add((FieldDeclaration) member);
                    } else if (member instanceof MethodDeclaration) {
                        methods.add((MethodDeclaration) member);
                    } else if (member instanceof ConstructorDeclaration) {
                        constructors.add((ConstructorDeclaration) member);
                    } else if (member instanceof TypeDeclaration) {
                        nestedTypes.add((TypeDeclaration) member);
                    }
                }
            }
        }
        
        reader.consume(TokenType.RBRACE, "Expected '}' after enum body");
        
        return new EnumDeclaration(token, name, modifiers, annotations, interfaces, constants,
                                  fields, methods, constructors, nestedTypes);
    }
    
    private List<EnumConstant> parseEnumConstants() {
        List<EnumConstant> constants = new ArrayList<>();
        
        do {
            constants.add(parseEnumConstant());
        } while (reader.match(TokenType.COMMA) && !reader.check(TokenType.RBRACE) && !reader.check(TokenType.SEMICOLON));
        
        return constants;
    }
    
    private EnumConstant parseEnumConstant() {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        Token token = reader.peek();
        String name = reader.consume(TokenType.IDENTIFIER, "Expected enum constant name").getLexeme();
        
        List<Expression> arguments = new ArrayList<>();
        if (reader.match(TokenType.LPAREN)) {
            if (!reader.check(TokenType.RPAREN)) {
                arguments = expressionParser.parseArgumentList();
            }
            reader.consume(TokenType.RPAREN, "Expected ')' after arguments");
        }
        
        ClassDeclaration anonymousClass = null;
        if (reader.match(TokenType.LBRACE)) {
            anonymousClass = parseAnonymousClass(name);
        }
        
        return new EnumConstant(token, name, arguments, anonymousClass, annotations);
    }
    
    private ClassDeclaration parseAnonymousClass(String name) {
        Token token = reader.previous();
        
        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();
        List<ConstructorDeclaration> constructors = new ArrayList<>();
        List<InitializerBlock> initializers = new ArrayList<>();
        List<TypeDeclaration> nestedTypes = new ArrayList<>();
        
        while (!reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
            ASTNode member = parseClassMember(name);
            if (member instanceof FieldDeclaration) {
                fields.add((FieldDeclaration) member);
            } else if (member instanceof MethodDeclaration) {
                methods.add((MethodDeclaration) member);
            } else if (member instanceof ConstructorDeclaration) {
                constructors.add((ConstructorDeclaration) member);
            } else if (member instanceof InitializerBlock) {
                initializers.add((InitializerBlock) member);
            } else if (member instanceof TypeDeclaration) {
                nestedTypes.add((TypeDeclaration) member);
            }
        }
        
        reader.consume(TokenType.RBRACE, "Expected '}' after anonymous class body");
        
        return new ClassDeclaration(token, name, 0, new ArrayList<>(), new ArrayList<>(),
                                   null, new ArrayList<>(), fields, methods, constructors,
                                   initializers, nestedTypes);
    }
    
    private AnnotationDeclaration parseAnnotationDeclaration(List<Annotation> annotations, int modifiers) {
        Token token = reader.previous();
        String name = reader.consume(TokenType.IDENTIFIER, "Expected annotation name").getLexeme();
        
        reader.consume(TokenType.LBRACE, "Expected '{' before annotation body");
        
        List<AnnotationDeclaration.AnnotationElement> elements = new ArrayList<>();
        
        while (!reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
            elements.add(parseAnnotationElement());
        }
        
        reader.consume(TokenType.RBRACE, "Expected '}' after annotation body");
        
        return new AnnotationDeclaration(token, name, modifiers, annotations, elements);
    }
    
    private AnnotationDeclaration.AnnotationElement parseAnnotationElement() {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        int modifiers = modifierAndAnnotationParser.parseModifiers();
        
        Type type = typeParser.parseType();
        String name = reader.consume(TokenType.IDENTIFIER, "Expected element name").getLexeme();
        
        reader.consume(TokenType.LPAREN, "Expected '(' after element name");
        reader.consume(TokenType.RPAREN, "Expected ')' after '('");
        
        Expression defaultValue = null;
        if (reader.match(TokenType.DEFAULT)) {
            defaultValue = expressionParser.parseElementValue();
        }
        
        reader.consume(TokenType.SEMICOLON, "Expected ';' after annotation element");
        
        return new AnnotationDeclaration.AnnotationElement(reader.previous(), name, type, defaultValue);
    }
}
