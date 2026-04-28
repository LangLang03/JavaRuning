package cn.langlang.javainterpreter.parser;

import cn.langlang.javainterpreter.lexer.*;
import cn.langlang.javainterpreter.ast.*;
import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    
    public CompilationUnit parseCompilationUnit() {
        Token token = peek();
        PackageDeclaration packageDecl = null;
        List<ImportDeclaration> imports = new ArrayList<>();
        List<TypeDeclaration> types = new ArrayList<>();
        
        if (match(TokenType.PACKAGE)) {
            packageDecl = parsePackageDeclaration();
        }
        
        while (match(TokenType.IMPORT)) {
            imports.add(parseImportDeclaration());
        }
        
        while (!check(TokenType.EOF)) {
            types.add(parseTypeDeclaration());
        }
        
        return new CompilationUnit(token, packageDecl, imports, types);
    }
    
    private PackageDeclaration parsePackageDeclaration() {
        Token token = previous();
        String name = parseQualifiedName();
        consume(TokenType.SEMICOLON, "Expected ';' after package declaration");
        return new PackageDeclaration(token, name);
    }
    
    private ImportDeclaration parseImportDeclaration() {
        Token token = previous();
        boolean isStatic = match(TokenType.STATIC);
        String name = parseQualifiedName();
        boolean isAsterisk = false;
        
        if (check(TokenType.DOT) && checkNext(TokenType.STAR)) {
            advance();
            advance();
            isAsterisk = true;
        }
        
        consume(TokenType.SEMICOLON, "Expected ';' after import declaration");
        return new ImportDeclaration(token, name, isStatic, isAsterisk);
    }
    
    private String parseQualifiedName() {
        StringBuilder sb = new StringBuilder();
        sb.append(consume(TokenType.IDENTIFIER, "Expected identifier").getLexeme());
        
        while (check(TokenType.DOT) && checkNext(TokenType.IDENTIFIER)) {
            match(TokenType.DOT);
            sb.append(".");
            sb.append(advance().getLexeme());
        }
        
        return sb.toString();
    }
    
    private TypeDeclaration parseTypeDeclaration() {
        List<Annotation> annotations = parseAnnotations();
        int modifiers = parseModifiers();
        
        if (match(TokenType.CLASS)) {
            return parseClassDeclaration(annotations, modifiers);
        } else if (match(TokenType.INTERFACE)) {
            return parseInterfaceDeclaration(annotations, modifiers);
        } else if (match(TokenType.ENUM)) {
            return parseEnumDeclaration(annotations, modifiers);
        } else if (match(TokenType.AT)) {
            consume(TokenType.INTERFACE, "Expected 'interface' after '@'");
            return parseAnnotationDeclaration(annotations, modifiers);
        }
        
        throw error(peek(), "Expected class, interface, enum, or annotation declaration");
    }
    
    private ClassDeclaration parseClassDeclaration(List<Annotation> annotations, int modifiers) {
        Token token = previous();
        String name = consume(TokenType.IDENTIFIER, "Expected class name").getLexeme();
        
        List<TypeParameter> typeParameters = new ArrayList<>();
        if (match(TokenType.LT)) {
            typeParameters = parseTypeParameters();
        }
        
        Type superClass = null;
        if (match(TokenType.EXTENDS)) {
            superClass = parseType();
        }
        
        List<Type> interfaces = new ArrayList<>();
        if (match(TokenType.IMPLEMENTS)) {
            interfaces = parseTypeList();
        }
        
        consume(TokenType.LBRACE, "Expected '{' before class body");
        
        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();
        List<ConstructorDeclaration> constructors = new ArrayList<>();
        List<InitializerBlock> initializers = new ArrayList<>();
        List<TypeDeclaration> nestedTypes = new ArrayList<>();
        
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
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
        
        consume(TokenType.RBRACE, "Expected '}' after class body");
        
        return new ClassDeclaration(token, name, modifiers, annotations, typeParameters,
                                   superClass, interfaces, fields, methods, constructors,
                                   initializers, nestedTypes);
    }
    
    private ASTNode parseClassMember(String className) {
        List<Annotation> annotations = parseAnnotations();
        
        if (match(TokenType.LBRACE)) {
            return parseInitializerBlock(false, annotations);
        }
        
        int modifiers = 0;
        if (match(TokenType.STATIC)) {
            modifiers |= Modifier.STATIC;
            if (match(TokenType.LBRACE)) {
                return parseInitializerBlock(true, annotations);
            }
        }
        
        modifiers |= parseModifiers();
        
        if (check(TokenType.CLASS) || check(TokenType.INTERFACE) || 
            check(TokenType.ENUM) || check(TokenType.AT)) {
            return parseTypeDeclaration();
        }
        
        if (checkIdentifier() && checkNext(TokenType.LPAREN)) {
            return parseConstructorDeclaration(className, modifiers, annotations);
        }
        
        List<TypeParameter> typeParameters = new ArrayList<>();
        if (match(TokenType.LT)) {
            typeParameters = parseTypeParameters();
        }
        
        Type type = parseType();
        String name = consume(TokenType.IDENTIFIER, "Expected member name").getLexeme();
        
        if (match(TokenType.LPAREN)) {
            return parseMethodDeclaration(type, name, modifiers, annotations, typeParameters);
        }
        
        return parseFieldDeclaration(type, name, modifiers, annotations);
    }
    
    private InitializerBlock parseInitializerBlock(boolean isStatic, List<Annotation> annotations) {
        Token token = previous();
        BlockStatement body = parseBlock();
        return new InitializerBlock(token, isStatic, body);
    }
    
    private FieldDeclaration parseFieldDeclaration(Type type, String name, 
                                                   int modifiers, List<Annotation> annotations) {
        Token token = previous();
        Expression initializer = null;
        
        if (match(TokenType.ASSIGN)) {
            initializer = parseExpression();
        }
        
        consume(TokenType.SEMICOLON, "Expected ';' after field declaration");
        
        return new FieldDeclaration(token, modifiers, type, name, initializer, annotations);
    }
    
    private MethodDeclaration parseMethodDeclaration(Type returnType, String name,
                                                     int modifiers, List<Annotation> annotations,
                                                     List<TypeParameter> typeParameters) {
        Token token = previous();
        
        List<ParameterDeclaration> parameters = parseParameters();
        boolean isVarArgs = !parameters.isEmpty() && parameters.get(parameters.size() - 1).isVarArgs();
        
        List<Type> exceptionTypes = new ArrayList<>();
        if (match(TokenType.THROWS)) {
            exceptionTypes = parseTypeList();
        }
        
        boolean isDefault = match(TokenType.DEFAULT);
        
        BlockStatement body = null;
        if (match(TokenType.LBRACE)) {
            body = parseBlock();
        } else if (!isDefault) {
            consume(TokenType.SEMICOLON, "Expected ';' or method body");
        }
        
        return new MethodDeclaration(token, modifiers, typeParameters, returnType, name,
                                    parameters, isVarArgs, exceptionTypes, body, annotations, isDefault);
    }
    
    private ConstructorDeclaration parseConstructorDeclaration(String className,
                                                               int modifiers, List<Annotation> annotations) {
        Token token = previous();
        
        List<TypeParameter> typeParameters = new ArrayList<>();
        if (match(TokenType.LT)) {
            typeParameters = parseTypeParameters();
        }
        
        consume(TokenType.IDENTIFIER, "Expected constructor name");
        consume(TokenType.LPAREN, "Expected '(' after constructor name");
        
        List<ParameterDeclaration> parameters = parseParameters();
        
        List<Type> exceptionTypes = new ArrayList<>();
        if (match(TokenType.THROWS)) {
            exceptionTypes = parseTypeList();
        }
        
        consume(TokenType.LBRACE, "Expected '{' before constructor body");
        BlockStatement body = parseBlock();
        
        return new ConstructorDeclaration(token, modifiers, typeParameters, className,
                                         parameters, exceptionTypes, body, annotations);
    }
    
    private InterfaceDeclaration parseInterfaceDeclaration(List<Annotation> annotations, int modifiers) {
        Token token = previous();
        String name = consume(TokenType.IDENTIFIER, "Expected interface name").getLexeme();
        
        List<TypeParameter> typeParameters = new ArrayList<>();
        if (match(TokenType.LT)) {
            typeParameters = parseTypeParameters();
        }
        
        List<Type> extendsInterfaces = new ArrayList<>();
        if (match(TokenType.EXTENDS)) {
            extendsInterfaces = parseTypeList();
        }
        
        consume(TokenType.LBRACE, "Expected '{' before interface body");
        
        List<MethodDeclaration> methods = new ArrayList<>();
        List<FieldDeclaration> constants = new ArrayList<>();
        List<TypeDeclaration> nestedTypes = new ArrayList<>();
        
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            ASTNode member = parseInterfaceMember();
            if (member instanceof MethodDeclaration) {
                methods.add((MethodDeclaration) member);
            } else if (member instanceof FieldDeclaration) {
                constants.add((FieldDeclaration) member);
            } else if (member instanceof TypeDeclaration) {
                nestedTypes.add((TypeDeclaration) member);
            }
        }
        
        consume(TokenType.RBRACE, "Expected '}' after interface body");
        
        return new InterfaceDeclaration(token, name, modifiers, annotations, typeParameters,
                                       extendsInterfaces, methods, constants, nestedTypes);
    }
    
    private ASTNode parseInterfaceMember() {
        List<Annotation> annotations = parseAnnotations();
        int modifiers = parseModifiers();
        
        if (check(TokenType.CLASS) || check(TokenType.INTERFACE) || 
            check(TokenType.ENUM) || check(TokenType.AT)) {
            return parseTypeDeclaration();
        }
        
        List<TypeParameter> typeParameters = new ArrayList<>();
        if (match(TokenType.LT)) {
            typeParameters = parseTypeParameters();
        }
        
        Type type = parseType();
        String name = consume(TokenType.IDENTIFIER, "Expected member name").getLexeme();
        
        if (match(TokenType.LPAREN)) {
            return parseMethodDeclaration(type, name, modifiers, annotations, typeParameters);
        }
        
        return parseFieldDeclaration(type, name, modifiers, annotations);
    }
    
    private EnumDeclaration parseEnumDeclaration(List<Annotation> annotations, int modifiers) {
        Token token = previous();
        String name = consume(TokenType.IDENTIFIER, "Expected enum name").getLexeme();
        
        List<Type> interfaces = new ArrayList<>();
        if (match(TokenType.IMPLEMENTS)) {
            interfaces = parseTypeList();
        }
        
        consume(TokenType.LBRACE, "Expected '{' before enum body");
        
        List<EnumConstant> constants = new ArrayList<>();
        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();
        List<ConstructorDeclaration> constructors = new ArrayList<>();
        List<TypeDeclaration> nestedTypes = new ArrayList<>();
        
        if (!check(TokenType.RBRACE)) {
            constants = parseEnumConstants();
            
            if (match(TokenType.SEMICOLON)) {
                while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
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
        
        consume(TokenType.RBRACE, "Expected '}' after enum body");
        
        return new EnumDeclaration(token, name, modifiers, annotations, interfaces, constants,
                                  fields, methods, constructors, nestedTypes);
    }
    
    private List<EnumConstant> parseEnumConstants() {
        List<EnumConstant> constants = new ArrayList<>();
        
        do {
            constants.add(parseEnumConstant());
        } while (match(TokenType.COMMA) && !check(TokenType.RBRACE) && !check(TokenType.SEMICOLON));
        
        return constants;
    }
    
    private EnumConstant parseEnumConstant() {
        List<Annotation> annotations = parseAnnotations();
        Token token = peek();
        String name = consume(TokenType.IDENTIFIER, "Expected enum constant name").getLexeme();
        
        List<Expression> arguments = new ArrayList<>();
        if (match(TokenType.LPAREN)) {
            if (!check(TokenType.RPAREN)) {
                arguments = parseArgumentList();
            }
            consume(TokenType.RPAREN, "Expected ')' after arguments");
        }
        
        ClassDeclaration anonymousClass = null;
        if (match(TokenType.LBRACE)) {
            anonymousClass = parseAnonymousClass(name);
        }
        
        return new EnumConstant(token, name, arguments, anonymousClass, annotations);
    }
    
    private ClassDeclaration parseAnonymousClass(String name) {
        Token token = previous();
        
        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();
        List<ConstructorDeclaration> constructors = new ArrayList<>();
        List<InitializerBlock> initializers = new ArrayList<>();
        List<TypeDeclaration> nestedTypes = new ArrayList<>();
        
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
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
        
        consume(TokenType.RBRACE, "Expected '}' after anonymous class body");
        
        return new ClassDeclaration(token, name, 0, new ArrayList<>(), new ArrayList<>(),
                                   null, new ArrayList<>(), fields, methods, constructors,
                                   initializers, nestedTypes);
    }
    
    private AnnotationDeclaration parseAnnotationDeclaration(List<Annotation> annotations, int modifiers) {
        Token token = previous();
        String name = consume(TokenType.IDENTIFIER, "Expected annotation name").getLexeme();
        
        consume(TokenType.LBRACE, "Expected '{' before annotation body");
        
        List<AnnotationDeclaration.AnnotationElement> elements = new ArrayList<>();
        
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            elements.add(parseAnnotationElement());
        }
        
        consume(TokenType.RBRACE, "Expected '}' after annotation body");
        
        return new AnnotationDeclaration(token, name, modifiers, annotations, elements);
    }
    
    private AnnotationDeclaration.AnnotationElement parseAnnotationElement() {
        List<Annotation> annotations = parseAnnotations();
        int modifiers = parseModifiers();
        
        Type type = parseType();
        String name = consume(TokenType.IDENTIFIER, "Expected element name").getLexeme();
        
        consume(TokenType.LPAREN, "Expected '(' after element name");
        consume(TokenType.RPAREN, "Expected ')' after '('");
        
        Expression defaultValue = null;
        if (match(TokenType.DEFAULT)) {
            defaultValue = parseElementValue();
        }
        
        consume(TokenType.SEMICOLON, "Expected ';' after annotation element");
        
        return new AnnotationDeclaration.AnnotationElement(previous(), name, type, defaultValue);
    }
    
    private List<ParameterDeclaration> parseParameters() {
        List<ParameterDeclaration> parameters = new ArrayList<>();
        
        if (!check(TokenType.RPAREN)) {
            do {
                parameters.add(parseParameter());
            } while (match(TokenType.COMMA));
        }
        
        consume(TokenType.RPAREN, "Expected ')' after parameters");
        return parameters;
    }
    
    private ParameterDeclaration parseParameter() {
        List<Annotation> annotations = parseAnnotations();
        int modifiers = parseModifiers();
        
        Type type = parseType();
        boolean isVarArgs = match(TokenType.ELLIPSIS);
        
        String name = consume(TokenType.IDENTIFIER, "Expected parameter name").getLexeme();
        
        return new ParameterDeclaration(previous(), modifiers, type, name, isVarArgs, annotations);
    }
    
    private List<Annotation> parseAnnotations() {
        List<Annotation> annotations = new ArrayList<>();
        
        while (check(TokenType.AT) && !checkNext(TokenType.INTERFACE)) {
            match(TokenType.AT);
            annotations.add(parseAnnotation());
        }
        
        return annotations;
    }
    
    private Annotation parseAnnotation() {
        Token token = previous();
        String typeName = parseQualifiedName();
        
        Map<String, Expression> elementValues = new HashMap<>();
        boolean isSingleElement = false;
        
        if (match(TokenType.LPAREN)) {
            if (!check(TokenType.RPAREN)) {
                if (check(TokenType.IDENTIFIER) && checkNext(TokenType.ASSIGN)) {
                    do {
                        String name = consume(TokenType.IDENTIFIER, "Expected element name").getLexeme();
                        consume(TokenType.ASSIGN, "Expected '=' after element name");
                        Expression value = parseElementValue();
                        elementValues.put(name, value);
                    } while (match(TokenType.COMMA));
                } else {
                    isSingleElement = true;
                    elementValues.put("value", parseElementValue());
                }
            }
            consume(TokenType.RPAREN, "Expected ')' after annotation elements");
        }
        
        return new Annotation(token, typeName, elementValues, isSingleElement);
    }
    
    private Expression parseElementValue() {
        if (match(TokenType.AT)) {
            return parseAnnotation();
        }
        
        return parseExpression();
    }
    
    private int parseModifiers() {
        int modifiers = 0;
        
        while (true) {
            if (match(TokenType.PUBLIC)) modifiers |= Modifier.PUBLIC;
            else if (match(TokenType.PRIVATE)) modifiers |= Modifier.PRIVATE;
            else if (match(TokenType.PROTECTED)) modifiers |= Modifier.PROTECTED;
            else if (match(TokenType.STATIC)) modifiers |= Modifier.STATIC;
            else if (match(TokenType.FINAL)) modifiers |= Modifier.FINAL;
            else if (match(TokenType.SYNCHRONIZED)) modifiers |= Modifier.SYNCHRONIZED;
            else if (match(TokenType.VOLATILE)) modifiers |= Modifier.VOLATILE;
            else if (match(TokenType.TRANSIENT)) modifiers |= Modifier.TRANSIENT;
            else if (match(TokenType.NATIVE)) modifiers |= Modifier.NATIVE;
            else if (match(TokenType.ABSTRACT)) modifiers |= Modifier.ABSTRACT;
            else if (match(TokenType.STRICTFP)) modifiers |= Modifier.STRICTFP;
            else if (match(TokenType.DEFAULT)) modifiers |= Modifier.DEFAULT;
            else break;
        }
        
        return modifiers;
    }
    
    private Type parseType() {
        List<Annotation> annotations = parseAnnotations();
        Token token = peek();
        
        String name;
        if (match(TokenType.INT)) name = "int";
        else if (match(TokenType.LONG)) name = "long";
        else if (match(TokenType.SHORT)) name = "short";
        else if (match(TokenType.BYTE)) name = "byte";
        else if (match(TokenType.CHAR)) name = "char";
        else if (match(TokenType.BOOLEAN)) name = "boolean";
        else if (match(TokenType.FLOAT)) name = "float";
        else if (match(TokenType.DOUBLE)) name = "double";
        else if (match(TokenType.VOID)) name = "void";
        else {
            name = parseQualifiedName();
        }
        
        List<TypeArgument> typeArguments = new ArrayList<>();
        if (match(TokenType.LT)) {
            if (match(TokenType.GT)) {
                typeArguments = new ArrayList<>();
            } else {
                typeArguments = parseTypeArguments();
            }
        }
        
        int arrayDimensions = 0;
        while (match(TokenType.LBRACKET)) {
            parseAnnotations();
            consume(TokenType.RBRACKET, "Expected ']' after '['");
            arrayDimensions++;
        }
        
        return new Type(token, name, typeArguments, arrayDimensions, annotations);
    }
    
    private List<TypeArgument> parseTypeArguments() {
        List<TypeArgument> arguments = new ArrayList<>();
        
        do {
            arguments.add(parseTypeArgument());
        } while (match(TokenType.COMMA));
        
        consume(TokenType.GT, "Expected '>' after type arguments");
        return arguments;
    }
    
    private TypeArgument parseTypeArgument() {
        Token token = peek();
        List<Annotation> annotations = parseAnnotations();
        
        if (match(TokenType.QUESTION)) {
            if (match(TokenType.EXTENDS)) {
                Type boundType = parseType();
                return new TypeArgument(token, null, TypeArgument.WildcardKind.EXTENDS, boundType, annotations);
            } else if (match(TokenType.SUPER)) {
                Type boundType = parseType();
                return new TypeArgument(token, null, TypeArgument.WildcardKind.SUPER, boundType, annotations);
            } else {
                return new TypeArgument(token, null, TypeArgument.WildcardKind.UNBOUNDED, null, annotations);
            }
        }
        
        Type type = parseType();
        return new TypeArgument(token, type, TypeArgument.WildcardKind.NONE, null, annotations);
    }
    
    private List<TypeParameter> parseTypeParameters() {
        List<TypeParameter> parameters = new ArrayList<>();
        
        do {
            parameters.add(parseTypeParameter());
        } while (match(TokenType.COMMA));
        
        consume(TokenType.GT, "Expected '>' after type parameters");
        return parameters;
    }
    
    private TypeParameter parseTypeParameter() {
        Token token = peek();
        List<Annotation> annotations = parseAnnotations();
        
        String name = consume(TokenType.IDENTIFIER, "Expected type parameter name").getLexeme();
        
        List<Type> bounds = new ArrayList<>();
        if (match(TokenType.EXTENDS)) {
            bounds.add(parseType());
            while (match(TokenType.AMPERSAND)) {
                bounds.add(parseType());
            }
        }
        
        return new TypeParameter(token, name, bounds, annotations);
    }
    
    private List<Type> parseTypeList() {
        List<Type> types = new ArrayList<>();
        
        do {
            types.add(parseType());
        } while (match(TokenType.COMMA));
        
        return types;
    }
    
    private BlockStatement parseBlock() {
        Token token = previous();
        List<Statement> statements = new ArrayList<>();
        
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            statements.add(parseStatement());
        }
        
        consume(TokenType.RBRACE, "Expected '}' after block");
        return new BlockStatement(token, statements);
    }
    
    private Statement parseStatement() {
        if (match(TokenType.LBRACE)) {
            return parseBlock();
        }
        
        if (match(TokenType.IF)) {
            return parseIfStatement();
        }
        
        if (match(TokenType.WHILE)) {
            return parseWhileStatement();
        }
        
        if (match(TokenType.DO)) {
            return parseDoStatement();
        }
        
        if (match(TokenType.FOR)) {
            return parseForStatement();
        }
        
        if (match(TokenType.SWITCH)) {
            return parseSwitchStatement();
        }
        
        if (match(TokenType.RETURN)) {
            return parseReturnStatement();
        }
        
        if (match(TokenType.THROW)) {
            return parseThrowStatement();
        }
        
        if (match(TokenType.TRY)) {
            return parseTryStatement();
        }
        
        if (match(TokenType.SYNCHRONIZED)) {
            return parseSynchronizedStatement();
        }
        
        if (match(TokenType.ASSERT)) {
            return parseAssertStatement();
        }
        
        if (match(TokenType.BREAK)) {
            return parseBreakStatement();
        }
        
        if (match(TokenType.CONTINUE)) {
            return parseContinueStatement();
        }
        
        if (match(TokenType.SEMICOLON)) {
            return new EmptyStatement(previous());
        }
        
        if (match(TokenType.THIS)) {
            if (match(TokenType.LPAREN)) {
                List<Expression> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    args = parseArgumentList();
                }
                consume(TokenType.RPAREN, "Expected ')' after this call");
                consume(TokenType.SEMICOLON, "Expected ';' after this call");
                return new ExpressionStatement(previous(), 
                    new MethodInvocationExpression(previous(), new ThisExpression(previous(), null), 
                        new ArrayList<>(), "this", args));
            } else {
                return parseExpressionStatementWithPrefix(new ThisExpression(previous(), null));
            }
        }
        
        if (checkIdentifier()) {
            if (checkNext(TokenType.COLON)) {
                return parseLabelStatement();
            }
            
            if (isLocalVariableDeclaration()) {
                return parseLocalVariableDeclaration();
            }
        }
        
        if (check(TokenType.AT) && isLocalVariableDeclaration()) {
            return parseLocalVariableDeclaration();
        }
        
        if (isLocalVariableDeclaration()) {
            return parseLocalVariableDeclaration();
        }
        
        if (check(TokenType.CLASS)) {
            TypeDeclaration typeDecl = parseTypeDeclaration();
            if (typeDecl instanceof ClassDeclaration) {
                return new LocalClassDeclarationStatement(typeDecl.getToken(), (ClassDeclaration) typeDecl);
            }
        }
        
        return parseExpressionStatement();
    }
    
    private ExpressionStatement parseExpressionStatementWithPrefix(Expression prefix) {
        Expression expression = parsePostfixExpressionWithPrefix(prefix);
        expression = parseBinaryExpressionRest(expression, 0);
        expression = parseTernaryExpressionRest(expression);
        
        if (isAssignmentOperator(peek().getType())) {
            Token token = advance();
            Expression value = parseAssignmentExpression();
            expression = new AssignmentExpression(token, expression, token.getType(), value);
        }
        
        consume(TokenType.SEMICOLON, "Expected ';' after expression");
        return new ExpressionStatement(expression.getToken(), expression);
    }
    
    private Expression parsePostfixExpressionWithPrefix(Expression prefix) {
        Expression expression = prefix;
        
        while (true) {
            if (match(TokenType.DOT)) {
                List<Annotation> annotations = parseAnnotations();
                
                if (match(TokenType.NEW)) {
                    expression = parseInnerClassCreation(expression);
                } else if (match(TokenType.SUPER)) {
                    SuperExpression superExpr = new SuperExpression(previous(), 
                        expression instanceof IdentifierExpression ? 
                            ((IdentifierExpression) expression).getName() : null);
                    
                    if (match(TokenType.DOT)) {
                        String name = consume(TokenType.IDENTIFIER, "Expected method or field name").getLexeme();
                        
                        if (match(TokenType.LPAREN)) {
                            List<TypeArgument> typeArgs = new ArrayList<>();
                            List<Expression> args = new ArrayList<>();
                            
                            if (!check(TokenType.RPAREN)) {
                                args = parseArgumentList();
                            }
                            consume(TokenType.RPAREN, "Expected ')' after arguments");
                            
                            expression = new MethodInvocationExpression(previous(), superExpr, typeArgs, name, args);
                        } else {
                            expression = new FieldAccessExpression(previous(), superExpr, name);
                        }
                    } else {
                        expression = superExpr;
                    }
                } else if (match(TokenType.THIS)) {
                    expression = new ThisExpression(previous(), 
                        ((IdentifierExpression) expression).getName());
                } else if (match(TokenType.CLASS)) {
                    expression = new ClassLiteralExpression(previous(), 
                        new Type(expression.getToken(), ((IdentifierExpression) expression).getName(), 
                                new ArrayList<>(), 0, annotations));
                } else {
                    String name = consume(TokenType.IDENTIFIER, "Expected identifier").getLexeme();
                    
                    if (match(TokenType.LPAREN)) {
                        List<TypeArgument> typeArgs = new ArrayList<>();
                        List<Expression> args = new ArrayList<>();
                        
                        if (!check(TokenType.RPAREN)) {
                            args = parseArgumentList();
                        }
                        consume(TokenType.RPAREN, "Expected ')' after arguments");
                        
                        expression = new MethodInvocationExpression(previous(), expression, typeArgs, name, args);
                    } else {
                        expression = new FieldAccessExpression(previous(), expression, name);
                    }
                }
            } else if (match(TokenType.LBRACKET)) {
                Expression index = parseExpression();
                consume(TokenType.RBRACKET, "Expected ']' after index");
                expression = new ArrayAccessExpression(previous(), expression, index);
            } else if (match(TokenType.COLONCOLON)) {
                List<TypeArgument> typeArgs = new ArrayList<>();
                if (match(TokenType.LT)) {
                    typeArgs = parseTypeArguments();
                }
                
                String methodName;
                if (match(TokenType.NEW)) {
                    methodName = "new";
                } else {
                    methodName = consume(TokenType.IDENTIFIER, "Expected method name").getLexeme();
                }
                
                expression = new MethodReferenceExpression(previous(), expression, typeArgs, methodName);
            } else {
                break;
            }
        }
        
        return expression;
    }
    
    private Expression parseBinaryExpressionRest(Expression left, int precedence) {
        while (true) {
            TokenType op = peek().getType();
            int opPrecedence = getOperatorPrecedence(op);
            
            if (opPrecedence < precedence) break;
            
            if (op == TokenType.INSTANCEOF) {
                advance();
                Type type = parseType();
                left = new InstanceOfExpression(previous(), left, type);
            } else {
                advance();
                Expression right = parseBinaryExpression(opPrecedence + 1);
                left = new BinaryExpression(previous(), left, op, right);
            }
        }
        
        return left;
    }
    
    private Expression parseTernaryExpressionRest(Expression expression) {
        if (match(TokenType.QUESTION)) {
            Expression trueExpr = parseExpression();
            consume(TokenType.COLON, "Expected ':' in ternary expression");
            Expression falseExpr = parseTernaryExpression();
            return new TernaryExpression(expression.getToken(), expression, trueExpr, falseExpr);
        }
        
        return expression;
    }
    
    private boolean isLocalVariableDeclaration() {
        int save = current;
        try {
            parseAnnotations();
            parseModifiers();
            
            if (check(TokenType.INT) || check(TokenType.LONG) || check(TokenType.SHORT) ||
                check(TokenType.BYTE) || check(TokenType.CHAR) || check(TokenType.BOOLEAN) ||
                check(TokenType.FLOAT) || check(TokenType.DOUBLE)) {
                return true;
            }
            
            if (check(TokenType.IDENTIFIER)) {
                advance();
                while (match(TokenType.DOT)) {
                    if (!match(TokenType.IDENTIFIER)) return false;
                }
                
                if (match(TokenType.LT)) {
                    int depth = 1;
                    while (depth > 0 && !check(TokenType.EOF)) {
                        if (match(TokenType.LT)) depth++;
                        else if (match(TokenType.GT)) depth--;
                        else advance();
                    }
                }
                
                while (match(TokenType.LBRACKET)) {
                    parseAnnotations();
                    if (!match(TokenType.RBRACKET)) return false;
                }
                
                return check(TokenType.IDENTIFIER);
            }
            
            return false;
        } finally {
            current = save;
        }
    }
    
    private boolean isForEachLoop() {
        int save = current;
        try {
            parseAnnotations();
            parseModifiers();
            
            if (check(TokenType.INT) || check(TokenType.LONG) || check(TokenType.SHORT) ||
                check(TokenType.BYTE) || check(TokenType.CHAR) || check(TokenType.BOOLEAN) ||
                check(TokenType.FLOAT) || check(TokenType.DOUBLE)) {
                advance();
                while (match(TokenType.LBRACKET)) {
                    if (!match(TokenType.RBRACKET)) return false;
                }
                if (check(TokenType.IDENTIFIER) && checkNext(TokenType.COLON)) {
                    return true;
                }
                return false;
            }
            
            if (check(TokenType.IDENTIFIER)) {
                advance();
                while (check(TokenType.DOT) && checkNext(TokenType.IDENTIFIER)) {
                    match(TokenType.DOT);
                    advance();
                }
                
                if (match(TokenType.LT)) {
                    int depth = 1;
                    while (depth > 0 && !check(TokenType.EOF)) {
                        if (match(TokenType.LT)) depth++;
                        else if (match(TokenType.GT)) depth--;
                        else advance();
                    }
                }
                
                while (match(TokenType.LBRACKET)) {
                    if (!match(TokenType.RBRACKET)) return false;
                }
                
                if (check(TokenType.IDENTIFIER) && checkNext(TokenType.COLON)) {
                    return true;
                }
            }
            
            return false;
        } finally {
            current = save;
        }
    }
    
    private LocalVariableDeclaration parseLocalVariableDeclaration() {
        Token token = peek();
        List<Annotation> annotations = parseAnnotations();
        int modifiers = parseModifiers();
        Type type = parseType();
        
        List<LocalVariableDeclaration.VariableDeclarator> declarators = new ArrayList<>();
        
        do {
            String name = consume(TokenType.IDENTIFIER, "Expected variable name").getLexeme();
            
            int arrayDims = 0;
            while (match(TokenType.LBRACKET)) {
                consume(TokenType.RBRACKET, "Expected ']' after '['");
                arrayDims++;
            }
            
            Expression initializer = null;
            if (match(TokenType.ASSIGN)) {
                if (match(TokenType.LBRACE)) {
                    initializer = parseArrayInitializer();
                } else {
                    initializer = parseExpression();
                }
            }
            
            declarators.add(new LocalVariableDeclaration.VariableDeclarator(name, initializer, arrayDims));
        } while (match(TokenType.COMMA));
        
        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration");
        
        return new LocalVariableDeclaration(token, modifiers, type, declarators, annotations);
    }
    
    private IfStatement parseIfStatement() {
        Token token = previous();
        consume(TokenType.LPAREN, "Expected '(' after 'if'");
        Expression condition = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after condition");
        
        Statement thenStatement = parseStatement();
        Statement elseStatement = null;
        
        if (match(TokenType.ELSE)) {
            elseStatement = parseStatement();
        }
        
        return new IfStatement(token, condition, thenStatement, elseStatement);
    }
    
    private WhileStatement parseWhileStatement() {
        Token token = previous();
        consume(TokenType.LPAREN, "Expected '(' after 'while'");
        Expression condition = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after condition");
        
        Statement body = parseStatement();
        return new WhileStatement(token, condition, body);
    }
    
    private DoStatement parseDoStatement() {
        Token token = previous();
        Statement body = parseStatement();
        
        consume(TokenType.WHILE, "Expected 'while' after do body");
        consume(TokenType.LPAREN, "Expected '(' after 'while'");
        Expression condition = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after condition");
        consume(TokenType.SEMICOLON, "Expected ';' after do-while");
        
        return new DoStatement(token, body, condition);
    }
    
    private Statement parseForStatement() {
        Token token = previous();
        consume(TokenType.LPAREN, "Expected '(' after 'for'");
        
        if (isForEachLoop()) {
            return parseForEachStatement(token);
        }
        
        Statement init = null;
        if (!check(TokenType.SEMICOLON)) {
            if (isLocalVariableDeclaration()) {
                init = parseLocalVariableDeclaration();
            } else {
                init = parseExpressionStatement();
            }
        } else {
            consume(TokenType.SEMICOLON, "Expected ';' after for init");
        }
        
        Expression condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = parseExpression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after for condition");
        
        Expression update = null;
        if (!check(TokenType.RPAREN)) {
            update = parseExpression();
        }
        consume(TokenType.RPAREN, "Expected ')' after for clauses");
        
        Statement body = parseStatement();
        
        return new ForStatement(token, init, condition, update, body);
    }
    
    private ForEachStatement parseForEachStatement(Token token) {
        List<Annotation> annotations = parseAnnotations();
        int modifiers = parseModifiers();
        Type type = parseType();
        
        String name = consume(TokenType.IDENTIFIER, "Expected variable name").getLexeme();
        LocalVariableDeclaration.VariableDeclarator declarator = 
            new LocalVariableDeclaration.VariableDeclarator(name, null, 0);
        LocalVariableDeclaration variable = new LocalVariableDeclaration(
            token, modifiers, type, Arrays.asList(declarator), annotations);
        
        consume(TokenType.COLON, "Expected ':' in for-each");
        Expression iterable = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after for-each");
        
        Statement body = parseStatement();
        return new ForEachStatement(token, variable, iterable, body);
    }
    
    private SwitchStatement parseSwitchStatement() {
        Token token = previous();
        consume(TokenType.LPAREN, "Expected '(' after 'switch'");
        Expression expression = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after expression");
        consume(TokenType.LBRACE, "Expected '{' after switch");
        
        List<SwitchStatement.SwitchCase> cases = new ArrayList<>();
        
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            CaseLabel label = parseCaseLabel();
            List<Statement> statements = new ArrayList<>();
            
            while (!check(TokenType.CASE) && !check(TokenType.DEFAULT) && 
                   !check(TokenType.RBRACE) && !check(TokenType.EOF)) {
                statements.add(parseStatement());
            }
            
            cases.add(new SwitchStatement.SwitchCase(label, statements));
        }
        
        consume(TokenType.RBRACE, "Expected '}' after switch body");
        return new SwitchStatement(token, expression, cases);
    }
    
    private CaseLabel parseCaseLabel() {
        Token token = peek();
        
        if (match(TokenType.DEFAULT)) {
            consume(TokenType.COLON, "Expected ':' after 'default'");
            return new CaseLabel(token, true, new ArrayList<>());
        }
        
        consume(TokenType.CASE, "Expected 'case' or 'default'");
        List<Expression> values = new ArrayList<>();
        
        do {
            values.add(parseExpression());
        } while (match(TokenType.COMMA));
        
        consume(TokenType.COLON, "Expected ':' after case value");
        return new CaseLabel(token, false, values);
    }
    
    private ReturnStatement parseReturnStatement() {
        Token token = previous();
        Expression expression = null;
        
        if (!check(TokenType.SEMICOLON)) {
            expression = parseExpression();
        }
        
        consume(TokenType.SEMICOLON, "Expected ';' after return");
        return new ReturnStatement(token, expression);
    }
    
    private ThrowStatement parseThrowStatement() {
        Token token = previous();
        Expression expression = parseExpression();
        consume(TokenType.SEMICOLON, "Expected ';' after throw");
        return new ThrowStatement(token, expression);
    }
    
    private TryStatement parseTryStatement() {
        Token token = previous();
        
        List<TryStatement.ResourceDeclaration> resources = new ArrayList<>();
        if (match(TokenType.LPAREN)) {
            resources = parseResources();
            consume(TokenType.RPAREN, "Expected ')' after resources");
        }
        
        consume(TokenType.LBRACE, "Expected '{' after try");
        BlockStatement tryBlock = parseBlock();
        
        List<CatchClause> catchClauses = new ArrayList<>();
        while (match(TokenType.CATCH)) {
            catchClauses.add(parseCatchClause());
        }
        
        BlockStatement finallyBlock = null;
        if (match(TokenType.FINALLY)) {
            consume(TokenType.LBRACE, "Expected '{' after 'finally'");
            finallyBlock = parseBlock();
        }
        
        return new TryStatement(token, resources, tryBlock, catchClauses, finallyBlock);
    }
    
    private List<TryStatement.ResourceDeclaration> parseResources() {
        List<TryStatement.ResourceDeclaration> resources = new ArrayList<>();
        
        do {
            resources.add(parseResource());
        } while (match(TokenType.SEMICOLON));
        
        return resources;
    }
    
    private TryStatement.ResourceDeclaration parseResource() {
        Token token = peek();
        List<Annotation> annotations = parseAnnotations();
        int modifiers = parseModifiers();
        Type type = parseType();
        String name = consume(TokenType.IDENTIFIER, "Expected resource name").getLexeme();
        
        Expression expression = null;
        if (match(TokenType.ASSIGN)) {
            expression = parseExpression();
        }
        
        return new TryStatement.ResourceDeclaration(token, type, name, expression);
    }
    
    private CatchClause parseCatchClause() {
        Token token = previous();
        consume(TokenType.LPAREN, "Expected '(' after 'catch'");
        
        List<Type> exceptionTypes = new ArrayList<>();
        exceptionTypes.add(parseType());
        
        while (match(TokenType.PIPE)) {
            exceptionTypes.add(parseType());
        }
        
        String exceptionName = consume(TokenType.IDENTIFIER, "Expected exception name").getLexeme();
        consume(TokenType.RPAREN, "Expected ')' after catch parameter");
        
        consume(TokenType.LBRACE, "Expected '{' after catch");
        BlockStatement body = parseBlock();
        
        return new CatchClause(token, exceptionTypes, exceptionName, body);
    }
    
    private SynchronizedStatement parseSynchronizedStatement() {
        Token token = previous();
        consume(TokenType.LPAREN, "Expected '(' after 'synchronized'");
        Expression lock = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after lock");
        
        consume(TokenType.LBRACE, "Expected '{' after synchronized");
        BlockStatement body = parseBlock();
        
        return new SynchronizedStatement(token, lock, body);
    }
    
    private AssertStatement parseAssertStatement() {
        Token token = previous();
        Expression condition = parseExpression();
        Expression message = null;
        
        if (match(TokenType.COLON)) {
            message = parseExpression();
        }
        
        consume(TokenType.SEMICOLON, "Expected ';' after assert");
        return new AssertStatement(token, condition, message);
    }
    
    private BreakStatement parseBreakStatement() {
        Token token = previous();
        String label = null;
        
        if (check(TokenType.IDENTIFIER)) {
            label = advance().getLexeme();
        }
        
        consume(TokenType.SEMICOLON, "Expected ';' after break");
        return new BreakStatement(token, label);
    }
    
    private ContinueStatement parseContinueStatement() {
        Token token = previous();
        String label = null;
        
        if (check(TokenType.IDENTIFIER)) {
            label = advance().getLexeme();
        }
        
        consume(TokenType.SEMICOLON, "Expected ';' after continue");
        return new ContinueStatement(token, label);
    }
    
    private LabelStatement parseLabelStatement() {
        Token token = peek();
        String label = consume(TokenType.IDENTIFIER, "Expected label name").getLexeme();
        consume(TokenType.COLON, "Expected ':' after label");
        
        Statement statement = parseStatement();
        return new LabelStatement(token, label, statement);
    }
    
    private ExpressionStatement parseExpressionStatement() {
        Token token = peek();
        Expression expression = parseExpression();
        consume(TokenType.SEMICOLON, "Expected ';' after expression");
        return new ExpressionStatement(token, expression);
    }
    
    private Expression parseExpression() {
        return parseAssignmentExpression();
    }
    
    private Expression parseAssignmentExpression() {
        Expression expression = parseTernaryExpression();
        
        if (isAssignmentOperator(peek().getType())) {
            Token token = advance();
            Expression value = parseAssignmentExpression();
            return new AssignmentExpression(token, expression, token.getType(), value);
        }
        
        return expression;
    }
    
    private boolean isAssignmentOperator(TokenType type) {
        return type == TokenType.ASSIGN || type == TokenType.PLUS_ASSIGN ||
               type == TokenType.MINUS_ASSIGN || type == TokenType.STAR_ASSIGN ||
               type == TokenType.SLASH_ASSIGN || type == TokenType.PERCENT_ASSIGN ||
               type == TokenType.AND_ASSIGN || type == TokenType.OR_ASSIGN ||
               type == TokenType.XOR_ASSIGN || type == TokenType.LSHIFT_ASSIGN ||
               type == TokenType.RSHIFT_ASSIGN || type == TokenType.URSHIFT_ASSIGN;
    }
    
    private Expression parseTernaryExpression() {
        Expression expression = parseBinaryExpression(0);
        
        if (match(TokenType.QUESTION)) {
            Expression trueExpr = parseExpression();
            consume(TokenType.COLON, "Expected ':' in ternary expression");
            Expression falseExpr = parseTernaryExpression();
            return new TernaryExpression(expression.getToken(), expression, trueExpr, falseExpr);
        }
        
        return expression;
    }
    
    private Expression parseBinaryExpression(int precedence) {
        Expression left = parseUnaryExpression();
        
        while (true) {
            TokenType op = peek().getType();
            int opPrecedence = getOperatorPrecedence(op);
            
            if (opPrecedence < precedence) break;
            
            if (op == TokenType.INSTANCEOF) {
                advance();
                Type type = parseType();
                left = new InstanceOfExpression(previous(), left, type);
            } else {
                advance();
                Expression right = parseBinaryExpression(opPrecedence + 1);
                left = new BinaryExpression(previous(), left, op, right);
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
    
    private Expression parseUnaryExpression() {
        if (match(TokenType.PLUS)) {
            return new UnaryExpression(previous(), TokenType.PLUS, parseUnaryExpression(), true);
        }
        if (match(TokenType.MINUS)) {
            return new UnaryExpression(previous(), TokenType.MINUS, parseUnaryExpression(), true);
        }
        if (match(TokenType.PLUSPLUS)) {
            return new UnaryExpression(previous(), TokenType.PLUSPLUS, parseUnaryExpression(), true);
        }
        if (match(TokenType.MINUSMINUS)) {
            return new UnaryExpression(previous(), TokenType.MINUSMINUS, parseUnaryExpression(), true);
        }
        if (match(TokenType.NOT)) {
            return new UnaryExpression(previous(), TokenType.NOT, parseUnaryExpression(), true);
        }
        if (match(TokenType.TILDE)) {
            return new UnaryExpression(previous(), TokenType.TILDE, parseUnaryExpression(), true);
        }
        
        Expression expression = parsePostfixExpression();
        
        if (match(TokenType.PLUSPLUS)) {
            return new UnaryExpression(previous(), TokenType.PLUSPLUS, expression, false);
        }
        if (match(TokenType.MINUSMINUS)) {
            return new UnaryExpression(previous(), TokenType.MINUSMINUS, expression, false);
        }
        
        return expression;
    }
    
    private Expression parsePostfixExpression() {
        Expression expression = parsePrimaryExpression();
        
        while (true) {
            if (match(TokenType.DOT)) {
                List<Annotation> annotations = parseAnnotations();
                
                if (match(TokenType.NEW)) {
                    expression = parseInnerClassCreation(expression);
                } else if (match(TokenType.SUPER)) {
                    SuperExpression superExpr = new SuperExpression(previous(), 
                        expression instanceof IdentifierExpression ? 
                            ((IdentifierExpression) expression).getName() : null);
                    
                    if (match(TokenType.DOT)) {
                        String name = consume(TokenType.IDENTIFIER, "Expected method or field name").getLexeme();
                        
                        if (match(TokenType.LPAREN)) {
                            List<TypeArgument> typeArgs = new ArrayList<>();
                            List<Expression> args = new ArrayList<>();
                            
                            if (!check(TokenType.RPAREN)) {
                                args = parseArgumentList();
                            }
                            consume(TokenType.RPAREN, "Expected ')' after arguments");
                            
                            expression = new MethodInvocationExpression(previous(), superExpr, typeArgs, name, args);
                        } else {
                            expression = new FieldAccessExpression(previous(), superExpr, name);
                        }
                    } else {
                        expression = superExpr;
                    }
                } else if (match(TokenType.THIS)) {
                    expression = new ThisExpression(previous(), 
                        ((IdentifierExpression) expression).getName());
                } else if (match(TokenType.CLASS)) {
                    expression = new ClassLiteralExpression(previous(), 
                        new Type(expression.getToken(), ((IdentifierExpression) expression).getName(), 
                                new ArrayList<>(), 0, annotations));
                } else {
                    String name = consume(TokenType.IDENTIFIER, "Expected identifier").getLexeme();
                    
                    if (match(TokenType.LPAREN)) {
                        List<TypeArgument> typeArgs = new ArrayList<>();
                        List<Expression> args = new ArrayList<>();
                        
                        if (!check(TokenType.RPAREN)) {
                            args = parseArgumentList();
                        }
                        consume(TokenType.RPAREN, "Expected ')' after arguments");
                        
                        expression = new MethodInvocationExpression(previous(), expression, typeArgs, name, args);
                    } else {
                        expression = new FieldAccessExpression(previous(), expression, name);
                    }
                }
            } else if (match(TokenType.LBRACKET)) {
                Expression index = parseExpression();
                consume(TokenType.RBRACKET, "Expected ']' after index");
                expression = new ArrayAccessExpression(previous(), expression, index);
            } else if (match(TokenType.COLONCOLON)) {
                List<TypeArgument> typeArgs = new ArrayList<>();
                if (match(TokenType.LT)) {
                    typeArgs = parseTypeArguments();
                }
                
                String methodName;
                if (match(TokenType.NEW)) {
                    methodName = "new";
                } else {
                    methodName = consume(TokenType.IDENTIFIER, "Expected method name").getLexeme();
                }
                
                expression = new MethodReferenceExpression(previous(), expression, typeArgs, methodName);
            } else {
                break;
            }
        }
        
        return expression;
    }
    
    private Expression parsePrimaryExpression() {
        if (match(TokenType.LPAREN)) {
            return parseParenthesizedOrCastExpression();
        }
        
        if (match(TokenType.NEW)) {
            return parseNewExpression();
        }
        
        if (match(TokenType.THIS)) {
            return new ThisExpression(previous(), null);
        }
        
        if (match(TokenType.SUPER)) {
            return new SuperExpression(previous(), null);
        }
        
        if (check(TokenType.INT) || check(TokenType.LONG) || check(TokenType.SHORT) ||
            check(TokenType.BYTE) || check(TokenType.CHAR) || check(TokenType.BOOLEAN) ||
            check(TokenType.FLOAT) || check(TokenType.DOUBLE)) {
            Token typeToken = advance();
            String typeName = typeToken.getLexeme();
            
            int arrayDims = 0;
            while (match(TokenType.LBRACKET)) {
                consume(TokenType.RBRACKET, "Expected ']' after '['");
                arrayDims++;
            }
            
            if (match(TokenType.COLONCOLON)) {
                List<TypeArgument> typeArgs = new ArrayList<>();
                if (match(TokenType.LT)) {
                    typeArgs = parseTypeArguments();
                }
                
                String methodName;
                if (match(TokenType.NEW)) {
                    methodName = "new";
                } else {
                    methodName = consume(TokenType.IDENTIFIER, "Expected method name").getLexeme();
                }
                
                Type type = new Type(typeToken, typeName, new ArrayList<>(), arrayDims, new ArrayList<>());
                return new MethodReferenceExpression(previous(), 
                    new ClassLiteralExpression(typeToken, type), typeArgs, methodName);
            }
            
            Type type = new Type(typeToken, typeName, new ArrayList<>(), arrayDims, new ArrayList<>());
            return new ClassLiteralExpression(typeToken, type);
        }
        
        if (match(TokenType.INT_LITERAL)) {
            return new LiteralExpression(previous(), previous().getLiteral());
        }
        if (match(TokenType.LONG_LITERAL)) {
            return new LiteralExpression(previous(), previous().getLiteral());
        }
        if (match(TokenType.FLOAT_LITERAL)) {
            return new LiteralExpression(previous(), previous().getLiteral());
        }
        if (match(TokenType.DOUBLE_LITERAL)) {
            return new LiteralExpression(previous(), previous().getLiteral());
        }
        if (match(TokenType.CHAR_LITERAL)) {
            return new LiteralExpression(previous(), previous().getLiteral());
        }
        if (match(TokenType.STRING_LITERAL)) {
            return new LiteralExpression(previous(), previous().getLiteral());
        }
        if (match(TokenType.BOOLEAN_LITERAL)) {
            return new LiteralExpression(previous(), previous().getLiteral());
        }
        if (match(TokenType.NULL_LITERAL)) {
            return new LiteralExpression(previous(), null);
        }
        
        if (match(TokenType.IDENTIFIER)) {
            Token identifier = previous();
            
            if (match(TokenType.ARROW)) {
                List<LambdaExpression.LambdaParameter> params = new ArrayList<>();
                params.add(new LambdaExpression.LambdaParameter(null, identifier.getLexeme()));
                return parseLambdaExpression(params);
            }
            
            if (match(TokenType.LPAREN)) {
                List<TypeArgument> typeArgs = new ArrayList<>();
                List<Expression> args = new ArrayList<>();
                
                if (!check(TokenType.RPAREN)) {
                    args = parseArgumentList();
                }
                consume(TokenType.RPAREN, "Expected ')' after arguments");
                
                return new MethodInvocationExpression(identifier, null, typeArgs, identifier.getLexeme(), args);
            }
            return new IdentifierExpression(identifier, identifier.getLexeme());
        }
        
        throw error(peek(), "Expected expression");
    }
    
    private Expression parseParenthesizedOrCastExpression() {
        int save = current;
        
        if (check(TokenType.RPAREN)) {
            match(TokenType.RPAREN);
            if (match(TokenType.ARROW)) {
                return parseLambdaExpression(new ArrayList<>());
            }
            current = save;
        }
        
        if (check(TokenType.IDENTIFIER)) {
            int paramSave = current;
            try {
                List<LambdaExpression.LambdaParameter> params = new ArrayList<>();
                String name = advance().getLexeme();
                params.add(new LambdaExpression.LambdaParameter(null, name));
                
                if (match(TokenType.RPAREN) && match(TokenType.ARROW)) {
                    return parseLambdaExpression(params);
                }
            } catch (Exception e) {
            }
            current = paramSave;
        }
        
        try {
            Type type = parseType();
            if (match(TokenType.RPAREN)) {
                if (match(TokenType.ARROW)) {
                    List<LambdaExpression.LambdaParameter> params = new ArrayList<>();
                    params.add(new LambdaExpression.LambdaParameter(type, null));
                    return parseLambdaExpression(params);
                }
                Expression expression = parseUnaryExpression();
                return new CastExpression(previous(), type, expression);
            }
        } catch (Exception e) {
        }
        
        current = save;
        
        List<LambdaExpression.LambdaParameter> params = tryParseLambdaParameters();
        if (params != null && match(TokenType.ARROW)) {
            return parseLambdaExpression(params);
        }
        
        current = save;
        Expression expression = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after expression");
        return new ParenthesizedExpression(previous(), expression);
    }
    
    private List<LambdaExpression.LambdaParameter> tryParseLambdaParameters() {
        try {
            List<LambdaExpression.LambdaParameter> params = new ArrayList<>();
            
            if (check(TokenType.RPAREN)) {
                return params;
            }
            
            do {
                Type paramType = null;
                if (check(TokenType.INT) || check(TokenType.LONG) || check(TokenType.SHORT) ||
                    check(TokenType.BYTE) || check(TokenType.CHAR) || check(TokenType.BOOLEAN) ||
                    check(TokenType.FLOAT) || check(TokenType.DOUBLE)) {
                    paramType = parseType();
                } else if (check(TokenType.IDENTIFIER) && checkNext(TokenType.IDENTIFIER)) {
                    paramType = parseType();
                }
                String name = consume(TokenType.IDENTIFIER, "Expected parameter name").getLexeme();
                params.add(new LambdaExpression.LambdaParameter(paramType, name));
            } while (match(TokenType.COMMA));
            
            if (match(TokenType.RPAREN)) {
                return params;
            }
        } catch (Exception e) {
        }
        return null;
    }
    
    private Expression parseLambdaExpression(List<LambdaExpression.LambdaParameter> params) {
        ASTNode body;
        if (match(TokenType.LBRACE)) {
            body = parseBlock();
        } else {
            body = parseExpression();
        }
        return new LambdaExpression(previous(), params, body);
    }
    
    private Expression parseNewExpression() {
        Token token = previous();
        Type type = parseType();
        
        if (match(TokenType.LBRACKET)) {
            return parseNewArrayExpression(token, type);
        }
        
        if (match(TokenType.LBRACE)) {
            ArrayInitializerExpression initializer = parseArrayInitializer();
            return new NewArrayExpression(token, type, new ArrayList<>(), initializer);
        }
        
        return parseNewObjectExpression(token, type);
    }
    
    private Expression parseNewArrayExpression(Token token, Type type) {
        List<Expression> dimensions = new ArrayList<>();
        
        if (!check(TokenType.RBRACKET)) {
            dimensions.add(parseExpression());
        }
        consume(TokenType.RBRACKET, "Expected ']' after dimension");
        
        while (match(TokenType.LBRACKET)) {
            if (!check(TokenType.RBRACKET)) {
                dimensions.add(parseExpression());
            }
            consume(TokenType.RBRACKET, "Expected ']' after dimension");
        }
        
        ArrayInitializerExpression initializer = null;
        if (match(TokenType.LBRACE)) {
            initializer = parseArrayInitializer();
        }
        
        return new NewArrayExpression(token, type, dimensions, initializer);
    }
    
    private Expression parseNewObjectExpression(Token token, Type type) {
        List<TypeArgument> typeArgs = new ArrayList<>();
        if (match(TokenType.LT)) {
            typeArgs = parseTypeArguments();
        }
        
        consume(TokenType.LPAREN, "Expected '(' after type");
        List<Expression> arguments = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            arguments = parseArgumentList();
        }
        consume(TokenType.RPAREN, "Expected ')' after arguments");
        
        List<ASTNode> anonymousClassBody = null;
        if (match(TokenType.LBRACE)) {
            anonymousClassBody = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
                anonymousClassBody.add(parseClassMember(type.getName()));
            }
            consume(TokenType.RBRACE, "Expected '}' after anonymous class body");
        }
        
        return new NewObjectExpression(token, type, typeArgs, arguments, anonymousClassBody);
    }
    
    private Expression parseInnerClassCreation(Expression target) {
        Token token = previous();
        Type type = parseType();
        
        List<TypeArgument> typeArgs = new ArrayList<>();
        if (match(TokenType.LT)) {
            typeArgs = parseTypeArguments();
        }
        
        consume(TokenType.LPAREN, "Expected '(' after type");
        List<Expression> arguments = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            arguments = parseArgumentList();
        }
        consume(TokenType.RPAREN, "Expected ')' after arguments");
        
        return new NewObjectExpression(token, type, typeArgs, arguments, null);
    }
    
    private ArrayInitializerExpression parseArrayInitializer() {
        Token token = previous();
        List<Expression> elements = new ArrayList<>();
        
        if (!check(TokenType.RBRACE)) {
            do {
                if (match(TokenType.LBRACE)) {
                    elements.add(parseArrayInitializer());
                } else {
                    elements.add(parseExpression());
                }
            } while (match(TokenType.COMMA));
        }
        
        consume(TokenType.RBRACE, "Expected '}' after array initializer");
        return new ArrayInitializerExpression(token, elements);
    }
    
    private List<Expression> parseArgumentList() {
        List<Expression> arguments = new ArrayList<>();
        
        do {
            arguments.add(parseExpression());
        } while (match(TokenType.COMMA));
        
        return arguments;
    }
    
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }
    
    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }
    
    private boolean check(TokenType type) {
        if (isAtEnd()) return type == TokenType.EOF;
        return peek().getType() == type;
    }
    
    private boolean checkIdentifier() {
        return check(TokenType.IDENTIFIER);
    }
    
    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).getType() == type;
    }
    
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    
    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }
    
    private Token peek() {
        return tokens.get(current);
    }
    
    private Token previous() {
        return tokens.get(current - 1);
    }
    
    private ParseError error(Token token, String message) {
        return new ParseError(token, message);
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
