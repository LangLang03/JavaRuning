package cn.langlang.javanter.parser;

import cn.langlang.javanter.lexer.Token;
import cn.langlang.javanter.lexer.TokenType;
import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.declaration.*;
import cn.langlang.javanter.ast.expression.*;
import cn.langlang.javanter.ast.misc.*;
import cn.langlang.javanter.ast.statement.BlockStatement;
import cn.langlang.javanter.ast.type.*;
import java.util.*;

public class DeclarationParser {
    private final TokenReader reader;
    private final ModifierAndAnnotationParser modifierAndAnnotationParser;
    private final TypeParser typeParser;
    private ExpressionParser expressionParser;
    private StatementParser statementParser;
    
    public DeclarationParser(TokenReader reader, ModifierAndAnnotationParser modifierAndAnnotationParser, TypeParser typeParser) {
        this.reader = reader;
        this.modifierAndAnnotationParser = modifierAndAnnotationParser;
        this.typeParser = typeParser;
    }
    
    public void setExpressionParser(ExpressionParser expressionParser) {
        this.expressionParser = expressionParser;
    }
    
    public void setStatementParser(StatementParser statementParser) {
        this.statementParser = statementParser;
    }
    
    public TypeDeclaration parseTypeDeclaration() {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        int modifiers = modifierAndAnnotationParser.parseModifiers();
        
        return parseTypeDeclarationWithModifiers(annotations, modifiers);
    }
    
    public TypeDeclaration parseTypeDeclarationWithModifiers(List<Annotation> annotations, int modifiers) {
        if (reader.match(TokenType.CLASS)) {
            return parseClassDeclaration(annotations, modifiers);
        } else if (reader.match(TokenType.INTERFACE)) {
            return parseInterfaceDeclaration(annotations, modifiers);
        } else if (reader.match(TokenType.ENUM)) {
            return parseEnumDeclaration(annotations, modifiers);
        } else if (reader.match(TokenType.RECORD)) {
            return parseRecordDeclaration(annotations, modifiers);
        } else if (reader.match(TokenType.AT)) {
            reader.consume(TokenType.INTERFACE, "Expected 'interface' after '@'");
            return parseAnnotationDeclaration(annotations, modifiers);
        }
        
        throw new Parser.ParseError(reader.peek(), "Expected class, interface, enum, record, or annotation declaration");
    }
    
    public ClassDeclaration parseClassDeclaration(List<Annotation> annotations, int modifiers) {
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
        
        boolean isSealed = (modifiers & Modifier.SEALED) != 0;
        boolean isNonSealed = (modifiers & Modifier.NON_SEALED) != 0;
        List<Type> permittedSubtypes = new ArrayList<>();
        if (isSealed && reader.match(TokenType.PERMITS)) {
            permittedSubtypes = typeParser.parseTypeList();
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
                                   initializers, nestedTypes, isSealed, isNonSealed, permittedSubtypes);
    }
    
    public ASTNode parseClassMember(String className) {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        
        if (reader.match(TokenType.LBRACE)) {
            return parseInitializerBlock(false, annotations);
        }
        
        int modifiers = 0;
        if (reader.match(TokenType.STATIC)) {
            modifiers |= Modifier.STATIC;
            if (reader.match(TokenType.LBRACE)) {
                return parseInitializerBlock(true, annotations);
            }
        }
        
        modifiers |= modifierAndAnnotationParser.parseModifiers();
        
        if (reader.check(TokenType.CLASS) || reader.check(TokenType.INTERFACE) || 
            reader.check(TokenType.ENUM) || reader.check(TokenType.AT) ||
            reader.check(TokenType.SEALED) || reader.check(TokenType.NON_SEALED) ||
            reader.check(TokenType.RECORD)) {
            return parseTypeDeclarationWithModifiers(annotations, modifiers);
        }
        
        if (reader.checkIdentifier() && reader.checkNext(TokenType.LPAREN)) {
            return parseConstructorDeclaration(className, modifiers, annotations);
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
    
    public InitializerBlock parseInitializerBlock(boolean isStatic, List<Annotation> annotations) {
        Token token = reader.previous();
        BlockStatement body = statementParser.parseBlock();
        return new InitializerBlock(token, isStatic, body);
    }
    
    public FieldDeclaration parseFieldDeclaration(Type type, String name, 
                                                   int modifiers, List<Annotation> annotations) {
        if (type.getName().equals("var")) {
            throw new Parser.ParseError(type.getToken(), "'var' is not allowed in field declarations");
        }
        
        Token token = reader.previous();
        Expression initializer = null;
        
        if (reader.match(TokenType.ASSIGN)) {
            initializer = expressionParser.parseExpression();
        }
        
        reader.consume(TokenType.SEMICOLON, "Expected ';' after field declaration");
        
        return new FieldDeclaration(token, modifiers, type, name, initializer, annotations);
    }
    
    public MethodDeclaration parseMethodDeclaration(Type returnType, String name,
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
            body = statementParser.parseBlock();
        } else if (!isDefault) {
            reader.consume(TokenType.SEMICOLON, "Expected ';' or method body");
        }
        
        return new MethodDeclaration(token, modifiers, typeParameters, returnType, name,
                                    parameters, isVarArgs, exceptionTypes, body, annotations, isDefault);
    }
    
    public ConstructorDeclaration parseConstructorDeclaration(String className,
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
        BlockStatement body = statementParser.parseBlock();
        
        return new ConstructorDeclaration(token, modifiers, typeParameters, className,
                                         parameters, exceptionTypes, body, annotations);
    }
    
    public List<ParameterDeclaration> parseParameters() {
        List<ParameterDeclaration> parameters = new ArrayList<>();
        
        if (!reader.check(TokenType.RPAREN)) {
            do {
                parameters.add(parseParameter());
            } while (reader.match(TokenType.COMMA));
        }
        
        reader.consume(TokenType.RPAREN, "Expected ')' after parameters");
        return parameters;
    }
    
    public ParameterDeclaration parseParameter() {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        int modifiers = modifierAndAnnotationParser.parseModifiers();
        
        Type type = typeParser.parseType();
        if (type.getName().equals("var")) {
            throw new Parser.ParseError(type.getToken(), "'var' is not allowed in parameter types");
        }
        boolean isVarArgs = reader.match(TokenType.ELLIPSIS);
        
        String name = reader.consume(TokenType.IDENTIFIER, "Expected parameter name").getLexeme();
        
        return new ParameterDeclaration(reader.previous(), modifiers, type, name, isVarArgs, annotations);
    }
    
    public InterfaceDeclaration parseInterfaceDeclaration(List<Annotation> annotations, int modifiers) {
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
        
        boolean isSealed = (modifiers & Modifier.SEALED) != 0;
        boolean isNonSealed = (modifiers & Modifier.NON_SEALED) != 0;
        List<Type> permittedSubtypes = new ArrayList<>();
        if (isSealed && reader.match(TokenType.PERMITS)) {
            permittedSubtypes = typeParser.parseTypeList();
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
                                       extendsInterfaces, methods, constants, nestedTypes,
                                       isSealed, isNonSealed, permittedSubtypes);
    }
    
    public ASTNode parseInterfaceMember() {
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
    
    public EnumDeclaration parseEnumDeclaration(List<Annotation> annotations, int modifiers) {
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
    
    public List<EnumConstant> parseEnumConstants() {
        List<EnumConstant> constants = new ArrayList<>();
        
        do {
            constants.add(parseEnumConstant());
        } while (reader.match(TokenType.COMMA) && !reader.check(TokenType.RBRACE) && !reader.check(TokenType.SEMICOLON));
        
        return constants;
    }
    
    public EnumConstant parseEnumConstant() {
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
    
    public ClassDeclaration parseAnonymousClass(String name) {
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
    
    public AnnotationDeclaration parseAnnotationDeclaration(List<Annotation> annotations, int modifiers) {
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
    
    public AnnotationDeclaration.AnnotationElement parseAnnotationElement() {
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
    
    public RecordDeclaration parseRecordDeclaration(List<Annotation> annotations, int modifiers) {
        Token token = reader.previous();
        String name = reader.consume(TokenType.IDENTIFIER, "Expected record name").getLexeme();
        
        List<TypeParameter> typeParameters = new ArrayList<>();
        if (reader.match(TokenType.LT)) {
            typeParameters = typeParser.parseTypeParameters();
        }
        
        reader.consume(TokenType.LPAREN, "Expected '(' after record name");
        List<RecordDeclaration.RecordComponent> components = new ArrayList<>();
        
        if (!reader.check(TokenType.RPAREN)) {
            do {
                components.add(parseRecordComponent());
            } while (reader.match(TokenType.COMMA));
        }
        
        reader.consume(TokenType.RPAREN, "Expected ')' after record components");
        
        List<Type> implementsInterfaces = new ArrayList<>();
        if (reader.match(TokenType.IMPLEMENTS)) {
            implementsInterfaces = typeParser.parseTypeList();
        }
        
        reader.consume(TokenType.LBRACE, "Expected '{' before record body");
        
        List<MethodDeclaration> methods = new ArrayList<>();
        List<FieldDeclaration> staticFields = new ArrayList<>();
        List<TypeDeclaration> nestedTypes = new ArrayList<>();
        
        while (!reader.check(TokenType.RBRACE) && !reader.check(TokenType.EOF)) {
            ASTNode member = parseRecordMember();
            if (member instanceof MethodDeclaration) {
                methods.add((MethodDeclaration) member);
            } else if (member instanceof FieldDeclaration) {
                staticFields.add((FieldDeclaration) member);
            } else if (member instanceof TypeDeclaration) {
                nestedTypes.add((TypeDeclaration) member);
            }
        }
        
        reader.consume(TokenType.RBRACE, "Expected '}' after record body");
        
        return new RecordDeclaration(token, name, modifiers, annotations, typeParameters,
                                    components, implementsInterfaces, methods,
                                    staticFields, nestedTypes);
    }
    
    private RecordDeclaration.RecordComponent parseRecordComponent() {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        Type type = typeParser.parseType();
        List<Annotation> componentAnnotations = modifierAndAnnotationParser.parseAnnotations();
        String name = reader.consume(TokenType.IDENTIFIER, "Expected component name").getLexeme();
        return new RecordDeclaration.RecordComponent(annotations, type, name, componentAnnotations);
    }
    
    private ASTNode parseRecordMember() {
        List<Annotation> annotations = modifierAndAnnotationParser.parseAnnotations();
        int modifiers = modifierAndAnnotationParser.parseModifiers();
        
        if ((modifiers & Modifier.STATIC) == 0) {
            throw new Parser.ParseError(reader.peek(), "Record members must be static");
        }
        
        if (reader.check(TokenType.CLASS) || reader.check(TokenType.INTERFACE) || 
            reader.check(TokenType.ENUM) || reader.check(TokenType.RECORD) || reader.check(TokenType.AT)) {
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
    
    private boolean checkIdentifier() {
        return reader.check(TokenType.IDENTIFIER);
    }
}
