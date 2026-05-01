package cn.langlang.javanter.analyzer;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.base.AbstractASTVisitor;
import cn.langlang.javanter.ast.declaration.*;
import cn.langlang.javanter.ast.expression.*;
import cn.langlang.javanter.ast.misc.*;
import cn.langlang.javanter.ast.statement.*;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.parser.Modifier;
import java.util.*;

/**
 * Static analyzer (linter) for Java code.
 *
 * <p>This class performs static analysis on Java source code represented as an AST
 * without executing it. It detects common programming errors such as:</p>
 * <ul>
 *   <li>Unresolved symbol references</li>
 *   <li>Access to non-static members from static context</li>
 *   <li>Invalid use of {@code this} and {@code super} keywords</li>
 *   <li>Calling non-static methods from static context</li>
 *   <li>Accessing non-static fields from static context</li>
 * </ul>
 *
 * <p>Analysis is performed in two passes:</p>
 * <ol>
 *   <li><b>First pass</b> - Collects all type declarations (classes, interfaces, enums)
 *       and builds a symbol table</li>
 *   <li><b>Second pass</b> - Visits each node and validates usage against the symbol table</li>
 * </ol>
 *
 * <p>Error reporting:</p>
 * <ul>
 *   <li>Errors halt execution and must be fixed before running</li>
 *   <li>Warnings are informational but don't prevent execution</li>
 *   <li>Both include file location, class, and method context</li>
 * </ul>
 *
 * @see AnalysisResult
 * @author Javanter Development Team
 */
public class StaticAnalyzer extends AbstractASTVisitor<Void> {
    private final List<LintError> errors;
    private final List<LintWarning> warnings;
    private String currentFileName;
    private ScriptClassInfo currentClass;
    private MethodInfo currentMethod;
    private boolean inStaticContext;
    private final Set<String> definedVariables;
    private final Map<String, ScriptClassInfo> classes;
    private final Set<String> importedClasses;
    
    public StaticAnalyzer() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.definedVariables = new HashSet<>();
        this.classes = new HashMap<>();
        this.importedClasses = new HashSet<>();
        this.inStaticContext = false;
    }
    
    public List<LintError> getErrors() {
        return errors;
    }
    
    public List<LintWarning> getWarnings() {
        return warnings;
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public void setFileName(String fileName) {
        this.currentFileName = fileName;
    }
    
    public AnalysisResult analyze(CompilationUnit unit) {
        errors.clear();
        warnings.clear();
        definedVariables.clear();
        classes.clear();
        importedClasses.clear();
        
        firstPass(unit);
        
        unit.accept(this);
        
        return new AnalysisResult(errors, warnings);
    }
    
    private void firstPass(CompilationUnit unit) {
        for (TypeDeclaration type : unit.getTypeDeclarations()) {
            if (type instanceof ClassDeclaration) {
                registerClass((ClassDeclaration) type);
            } else if (type instanceof InterfaceDeclaration) {
                registerInterface((InterfaceDeclaration) type);
            } else if (type instanceof EnumDeclaration) {
                registerEnum((EnumDeclaration) type);
            } else if (type instanceof RecordDeclaration) {
                registerRecord((RecordDeclaration) type);
            }
        }
    }
    
    private void registerRecord(RecordDeclaration node) {
        ScriptClassInfo info = new ScriptClassInfo(node.getName(), node.getModifiers() | Modifier.FINAL | Modifier.RECORD);
        
        for (RecordDeclaration.RecordComponent component : node.getComponents()) {
            info.addField(component.getName(), Modifier.PUBLIC | Modifier.FINAL);
            definedVariables.add(component.getName());
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            info.addMethod(method.getName(), method.getModifiers(), method.getParameters());
        }
        
        classes.put(node.getName(), info);
    }
    
    private void registerEnum(EnumDeclaration node) {
        ScriptClassInfo info = new ScriptClassInfo(node.getName(), node.getModifiers() | Modifier.ENUM);
        info.setStatic(false);
        
        for (EnumConstant constant : node.getConstants()) {
            importedClasses.add(constant.getName());
        }
        
        for (FieldDeclaration field : node.getFields()) {
            info.addField(field.getName(), field.getModifiers());
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            info.addMethod(method.getName(), method.getModifiers(), method.getParameters());
        }
        
        classes.put(node.getName(), info);
    }
    
    private void registerClass(ClassDeclaration node) {
        ScriptClassInfo info = new ScriptClassInfo(node.getName(), node.getModifiers());
        info.setStatic((node.getModifiers() & Modifier.STATIC) != 0);
        
        if ((node.getModifiers() & Modifier.SEALED) != 0) {
            if (node.getPermittedSubtypes() != null && !node.getPermittedSubtypes().isEmpty()) {
                for (Type permitted : node.getPermittedSubtypes()) {
                    info.addPermittedSubclass(permitted.getName());
                }
            } else {
                warnings.add(new LintWarning(
                    node.getLine(),
                    node.getColumn(),
                    currentFileName,
                    node.getName(),
                    null,
                    "Sealed class should have a permits clause"
                ));
            }
        }
        
        if ((node.getModifiers() & Modifier.NON_SEALED) != 0) {
            if ((node.getModifiers() & Modifier.FINAL) != 0) {
                warnings.add(new LintWarning(
                    node.getLine(),
                    node.getColumn(),
                    currentFileName,
                    node.getName(),
                    null,
                    "Class cannot be both non-sealed and final"
                ));
            }
        }
        
        for (FieldDeclaration field : node.getFields()) {
            info.addField(field.getName(), field.getModifiers());
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            info.addMethod(method.getName(), method.getModifiers(), method.getParameters());
        }
        
        classes.put(node.getName(), info);
    }
    
    private void registerInterface(InterfaceDeclaration node) {
        ScriptClassInfo info = new ScriptClassInfo(node.getName(), node.getModifiers() | Modifier.INTERFACE);
        
        for (MethodDeclaration method : node.getMethods()) {
            int modifiers = method.getModifiers();
            if ((modifiers & Modifier.PRIVATE) != 0) {
                if ((modifiers & Modifier.STATIC) == 0 && method.getBody() == null) {
                    warnings.add(new LintWarning(
                        method.getLine(),
                        method.getColumn(),
                        currentFileName,
                        node.getName(),
                        method.getName(),
                        "Private interface method should have a body"
                    ));
                }
            }
            info.addMethod(method.getName(), modifiers, method.getParameters());
        }
        
        classes.put(node.getName(), info);
    }
    
    private void addError(ASTNode node, String message) {
        errors.add(new LintError(
            node.getLine(),
            node.getColumn(),
            currentFileName,
            currentClass != null ? currentClass.getName() : null,
            currentMethod != null ? currentMethod.getName() : null,
            message
        ));
    }
    
    private void addWarning(ASTNode node, String message) {
        warnings.add(new LintWarning(
            node.getLine(),
            node.getColumn(),
            currentFileName,
            currentClass != null ? currentClass.getName() : null,
            currentMethod != null ? currentMethod.getName() : null,
            message
        ));
    }
    
    @Override
    public Void visitCompilationUnit(CompilationUnit node) {
        for (ImportDeclaration imp : node.getImports()) {
            imp.accept(this);
        }
        
        for (TypeDeclaration type : node.getTypeDeclarations()) {
            type.accept(this);
        }
        
        return null;
    }
    
    @Override
    public Void visitImportDeclaration(ImportDeclaration node) {
        String importName = node.getName();
        if (!node.isAsterisk() && !node.isStatic()) {
            int lastDot = importName.lastIndexOf('.');
            if (lastDot > 0) {
                String simpleName = importName.substring(lastDot + 1);
                importedClasses.add(simpleName);
            }
        }
        return null;
    }
    
    @Override
    public Void visitClassDeclaration(ClassDeclaration node) {
        ScriptClassInfo previousClass = currentClass;
        currentClass = classes.get(node.getName());
        
        for (FieldDeclaration field : node.getFields()) {
            field.accept(this);
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            method.accept(this);
        }
        
        for (ConstructorDeclaration constructor : node.getConstructors()) {
            constructor.accept(this);
        }
        
        for (InitializerBlock init : node.getInitializers()) {
            init.accept(this);
        }
        
        currentClass = previousClass;
        return null;
    }
    
    @Override
    public Void visitInterfaceDeclaration(InterfaceDeclaration node) {
        ScriptClassInfo previousClass = currentClass;
        currentClass = classes.get(node.getName());
        
        for (MethodDeclaration method : node.getMethods()) {
            method.accept(this);
        }
        
        currentClass = previousClass;
        return null;
    }
    
    @Override
    public Void visitEnumDeclaration(EnumDeclaration node) {
        ScriptClassInfo previousClass = currentClass;
        currentClass = classes.get(node.getName());
        
        for (EnumConstant constant : node.getConstants()) {
            definedVariables.add(constant.getName());
            if (constant.getAnonymousClass() != null) {
                for (MethodDeclaration method : constant.getAnonymousClass().getMethods()) {
                    method.accept(this);
                }
            }
        }
        
        for (FieldDeclaration field : node.getFields()) {
            field.accept(this);
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            method.accept(this);
        }
        
        for (ConstructorDeclaration constructor : node.getConstructors()) {
            constructor.accept(this);
        }
        
        currentClass = previousClass;
        return null;
    }
    
    @Override
    public Void visitRecordDeclaration(RecordDeclaration node) {
        ScriptClassInfo previousClass = currentClass;
        currentClass = classes.get(node.getName());
        
        for (RecordDeclaration.RecordComponent component : node.getComponents()) {
            definedVariables.add(component.getName());
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            method.accept(this);
        }
        
        for (TypeDeclaration nested : node.getNestedTypes()) {
            nested.accept(this);
        }
        
        currentClass = previousClass;
        return null;
    }
    
    @Override
    public Void visitEnumConstant(EnumConstant node) {
        return null;
    }
    
    @Override
    public Void visitMethodDeclaration(MethodDeclaration node) {
        MethodInfo previousMethod = currentMethod;
        boolean previousStaticContext = inStaticContext;
        
        currentMethod = currentClass != null ? currentClass.getMethod(node.getName()) : null;
        inStaticContext = (node.getModifiers() & Modifier.STATIC) != 0;
        
        Set<String> previousVars = new HashSet<>(definedVariables);
        
        for (ParameterDeclaration param : node.getParameters()) {
            definedVariables.add(param.getName());
        }
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        definedVariables.clear();
        definedVariables.addAll(previousVars);
        
        currentMethod = previousMethod;
        inStaticContext = previousStaticContext;
        
        return null;
    }
    
    @Override
    public Void visitConstructorDeclaration(ConstructorDeclaration node) {
        MethodInfo previousMethod = currentMethod;
        boolean previousStaticContext = inStaticContext;
        
        currentMethod = currentClass != null ? currentClass.getMethod(node.getName()) : null;
        inStaticContext = false;
        
        Set<String> previousVars = new HashSet<>(definedVariables);
        
        for (ParameterDeclaration param : node.getParameters()) {
            definedVariables.add(param.getName());
        }
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        definedVariables.clear();
        definedVariables.addAll(previousVars);
        
        currentMethod = previousMethod;
        inStaticContext = previousStaticContext;
        
        return null;
    }
    
    @Override
    public Void visitInitializerBlock(InitializerBlock node) {
        boolean previousStaticContext = inStaticContext;
        inStaticContext = node.isStatic();
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        inStaticContext = previousStaticContext;
        return null;
    }
    
    @Override
    public Void visitLocalVariableDeclaration(LocalVariableDeclaration node) {
        for (LocalVariableDeclaration.VariableDeclarator declarator : node.getDeclarators()) {
            definedVariables.add(declarator.getName());
            if (declarator.getInitializer() != null) {
                declarator.getInitializer().accept(this);
            }
        }
        
        if (node.getType() != null && "var".equals(node.getType().getName())) {
            for (LocalVariableDeclaration.VariableDeclarator declarator : node.getDeclarators()) {
                if (declarator.getInitializer() == null) {
                    addError(node, "Variable declared with 'var' must have an initializer");
                }
            }
        }
        return null;
    }
    
    @Override
    public Void visitBlockStatement(BlockStatement node) {
        for (ASTNode stmt : node.getStatements()) {
            stmt.accept(this);
        }
        return null;
    }
    
    @Override
    public Void visitIdentifierExpression(IdentifierExpression node) {
        String name = node.getName();
        
        if (inStaticContext && currentClass != null) {
            FieldInfo field = currentClass.getField(name);
            if (field != null && !field.isStatic()) {
                addError(node, "Cannot access non-static field '" + name + "' from static context");
            }
        }
        
        if (!isDefined(name)) {
            addError(node, "Cannot resolve symbol '" + name + "'");
        }
        
        return null;
    }
    
    private boolean isDefined(String name) {
        if (definedVariables.contains(name)) {
            return true;
        }
        if (importedClasses.contains(name)) {
            return true;
        }
        if (classes.containsKey(name)) {
            return true;
        }
        if (currentClass != null && currentClass.hasField(name)) {
            return true;
        }
        if (isPredefinedClass(name)) {
            return true;
        }
        return false;
    }
    
    private boolean isPredefinedClass(String name) {
        Set<String> predefined = new HashSet<>(Arrays.asList(
            "System", "Math", "String", "Integer", "Long", "Double", "Float",
            "Boolean", "Character", "Byte", "Short", "Object", "Class",
            "Exception", "RuntimeException", "Error", "Throwable",
            "List", "ArrayList", "Map", "HashMap", "Set", "HashSet",
            "Arrays", "Collections", "Objects", "Optional",
            "Runnable", "Thread", "StringBuilder", "StringBuffer",
            "File", "Path", "Paths", "Files", "IOException",
            "InputStream", "OutputStream", "Reader", "Writer",
            "Scanner", "PrintStream", "PrintWriter",
            "Date", "Calendar", "TimeZone",
            "Pattern", "Matcher", "Regex",
            "Function", "Consumer", "Supplier", "Predicate", "BiFunction",
            "Stream", "IntStream", "LongStream", "DoubleStream",
            "Collectors", "OptionalInt", "OptionalLong", "OptionalDouble",
            "UUID", "Random", "Timer", "TimerTask",
            "out", "err", "in", "PI", "E"
        ));
        return predefined.contains(name);
    }
    
    @Override
    public Void visitThisExpression(ThisExpression node) {
        if (inStaticContext) {
            addError(node, "Cannot use 'this' in static context");
        }
        return null;
    }
    
    @Override
    public Void visitSuperExpression(SuperExpression node) {
        if (inStaticContext) {
            addError(node, "Cannot use 'super' in static context");
        }
        return null;
    }
    
    @Override
    public Void visitFieldAccessExpression(FieldAccessExpression node) {
        node.getTarget().accept(this);
        
        if (node.getTarget() instanceof IdentifierExpression) {
            String targetName = ((IdentifierExpression) node.getTarget()).getName();
            
            if (classes.containsKey(targetName)) {
                ScriptClassInfo targetClass = classes.get(targetName);
                FieldInfo field = targetClass.getField(node.getFieldName());
                
                if (field != null && !field.isStatic()) {
                    addError(node, "Cannot access non-static field '" + node.getFieldName() + 
                        "' from class '" + targetName + "'");
                }
            }
        }
        
        return null;
    }
    
    @Override
    public Void visitMethodInvocationExpression(MethodInvocationExpression node) {
        if (node.getTarget() != null) {
            node.getTarget().accept(this);
            
            if (node.getTarget() instanceof IdentifierExpression) {
                String targetName = ((IdentifierExpression) node.getTarget()).getName();
                
                if (classes.containsKey(targetName)) {
                    ScriptClassInfo targetClass = classes.get(targetName);
                    MethodInfo method = targetClass.getMethod(node.getMethodName());
                    
                    if (method != null && !method.isStatic()) {
                        addError(node, "Cannot call non-static method '" + node.getMethodName() + 
                            "' from class '" + targetName + "'");
                    }
                }
            }
        } else {
            if (inStaticContext && currentClass != null) {
                MethodInfo method = currentClass.getMethod(node.getMethodName());
                if (method != null && !method.isStatic()) {
                    addError(node, "Cannot call non-static method '" + node.getMethodName() + 
                        "' from static context");
                }
            }
        }
        
        for (Expression arg : node.getArguments()) {
            arg.accept(this);
        }
        
        return null;
    }
    
    @Override
    public Void visitBinaryExpression(BinaryExpression node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        return null;
    }
    
    @Override
    public Void visitUnaryExpression(UnaryExpression node) {
        node.getOperand().accept(this);
        return null;
    }
    
    @Override
    public Void visitAssignmentExpression(AssignmentExpression node) {
        node.getTarget().accept(this);
        node.getValue().accept(this);
        return null;
    }
    
    @Override
    public Void visitTernaryExpression(TernaryExpression node) {
        node.getCondition().accept(this);
        node.getTrueExpression().accept(this);
        node.getFalseExpression().accept(this);
        return null;
    }
    
    @Override
    public Void visitNewObjectExpression(NewObjectExpression node) {
        for (Expression arg : node.getArguments()) {
            arg.accept(this);
        }
        return null;
    }
    
    @Override
    public Void visitNewArrayExpression(NewArrayExpression node) {
        for (Expression dim : node.getDimensions()) {
            dim.accept(this);
        }
        if (node.getInitializer() != null) {
            node.getInitializer().accept(this);
        }
        return null;
    }
    
    @Override
    public Void visitArrayAccessExpression(ArrayAccessExpression node) {
        node.getArray().accept(this);
        node.getIndex().accept(this);
        return null;
    }
    
    @Override
    public Void visitCastExpression(CastExpression node) {
        node.getExpression().accept(this);
        return null;
    }
    
    @Override
    public Void visitInstanceOfExpression(InstanceOfExpression node) {
        node.getExpression().accept(this);
        if (node.getPatternVariable() != null) {
            definedVariables.add(node.getPatternVariable());
        }
        return null;
    }
    
    @Override
    public Void visitLambdaExpression(LambdaExpression node) {
        Set<String> previousVars = new HashSet<>(definedVariables);
        
        for (LambdaExpression.LambdaParameter param : node.getParameters()) {
            definedVariables.add(param.getName());
        }
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        definedVariables.clear();
        definedVariables.addAll(previousVars);
        
        return null;
    }
    
    @Override
    public Void visitMethodReferenceExpression(MethodReferenceExpression node) {
        return null;
    }
    
    @Override
    public Void visitParenthesizedExpression(ParenthesizedExpression node) {
        node.getExpression().accept(this);
        return null;
    }
    
    @Override
    public Void visitIfStatement(IfStatement node) {
        node.getCondition().accept(this);
        node.getThenStatement().accept(this);
        if (node.getElseStatement() != null) {
            node.getElseStatement().accept(this);
        }
        return null;
    }
    
    @Override
    public Void visitWhileStatement(WhileStatement node) {
        node.getCondition().accept(this);
        node.getBody().accept(this);
        return null;
    }
    
    @Override
    public Void visitDoStatement(DoStatement node) {
        node.getBody().accept(this);
        node.getCondition().accept(this);
        return null;
    }
    
    @Override
    public Void visitForStatement(ForStatement node) {
        Set<String> previousVars = new HashSet<>(definedVariables);
        
        if (node.getInit() != null) {
            node.getInit().accept(this);
        }
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        if (node.getUpdate() != null) {
            node.getUpdate().accept(this);
        }
        node.getBody().accept(this);
        
        definedVariables.clear();
        definedVariables.addAll(previousVars);
        
        return null;
    }
    
    @Override
    public Void visitForEachStatement(ForEachStatement node) {
        Set<String> previousVars = new HashSet<>(definedVariables);
        
        if (node.getVariable() != null) {
            for (LocalVariableDeclaration.VariableDeclarator declarator : node.getVariable().getDeclarators()) {
                definedVariables.add(declarator.getName());
            }
        }
        node.getIterable().accept(this);
        node.getBody().accept(this);
        
        definedVariables.clear();
        definedVariables.addAll(previousVars);
        
        return null;
    }
    
    @Override
    public Void visitSwitchStatement(SwitchStatement node) {
        node.getExpression().accept(this);
        for (SwitchStatement.SwitchCase switchCase : node.getCases()) {
            if (switchCase.getLabel() != null) {
                switchCase.getLabel().accept(this);
            }
            for (Statement stmt : switchCase.getStatements()) {
                stmt.accept(this);
            }
        }
        return null;
    }
    
    @Override
    public Void visitSwitchExpression(SwitchExpression node) {
        node.getSelector().accept(this);
        for (SwitchExpression.SwitchCase switchCase : node.getCases()) {
            for (CaseLabel label : switchCase.getLabels()) {
                label.accept(this);
            }
            if (switchCase.isArrow() && switchCase.getBody() instanceof Expression) {
                ((Expression) switchCase.getBody()).accept(this);
            } else if (switchCase.getBody() instanceof BlockStatement) {
                ((BlockStatement) switchCase.getBody()).accept(this);
            } else if (switchCase.getBody() instanceof Statement) {
                ((Statement) switchCase.getBody()).accept(this);
            }
        }
        return null;
    }
    
    @Override
    public Void visitYieldStatement(YieldStatement node) {
        if (node.getValue() != null) {
            node.getValue().accept(this);
        }
        return null;
    }
    
    @Override
    public Void visitCaseLabel(CaseLabel node) {
        for (Expression value : node.getValues()) {
            value.accept(this);
        }
        return null;
    }
    
    @Override
    public Void visitReturnStatement(ReturnStatement node) {
        if (node.getExpression() != null) {
            node.getExpression().accept(this);
        }
        return null;
    }
    
    @Override
    public Void visitThrowStatement(ThrowStatement node) {
        node.getExpression().accept(this);
        return null;
    }
    
    @Override
    public Void visitTryStatement(TryStatement node) {
        node.getTryBlock().accept(this);
        for (CatchClause clause : node.getCatchClauses()) {
            clause.accept(this);
        }
        if (node.getFinallyBlock() != null) {
            node.getFinallyBlock().accept(this);
        }
        return null;
    }
    
    @Override
    public Void visitCatchClause(CatchClause node) {
        Set<String> previousVars = new HashSet<>(definedVariables);
        definedVariables.add(node.getExceptionName());
        node.getBody().accept(this);
        definedVariables.clear();
        definedVariables.addAll(previousVars);
        return null;
    }
    
    @Override
    public Void visitSynchronizedStatement(SynchronizedStatement node) {
        node.getLock().accept(this);
        node.getBody().accept(this);
        return null;
    }
    
    @Override
    public Void visitAssertStatement(AssertStatement node) {
        node.getCondition().accept(this);
        if (node.getMessage() != null) {
            node.getMessage().accept(this);
        }
        return null;
    }
    
    @Override
    public Void visitExpressionStatement(ExpressionStatement node) {
        node.getExpression().accept(this);
        return null;
    }
    
    @Override
    public Void visitArrayInitializerExpression(ArrayInitializerExpression node) {
        for (Expression elem : node.getElements()) {
            elem.accept(this);
        }
        return null;
    }

    /**
     * Represents a static analysis error that prevents code execution.
     *
     * <p>Errors include information about location and context to help
     * developers quickly identify and fix issues.</p>
     */
    public static class LintError {
        private final int line;
        private final int column;
        private final String fileName;
        private final String className;
        private final String methodName;
        private final String message;
        
        public LintError(int line, int column, String fileName, String className, String methodName, String message) {
            this.line = line;
            this.column = column;
            this.fileName = fileName;
            this.className = className;
            this.methodName = methodName;
            this.message = message;
        }
        
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public String getFileName() { return fileName; }
        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (fileName != null) {
                sb.append(fileName).append(":");
            }
            sb.append(line).append(":").append(column).append(": error: ");
            sb.append(message);
            if (className != null) {
                sb.append(" (in ").append(className);
                if (methodName != null) {
                    sb.append(".").append(methodName);
                }
                sb.append(")");
            }
            return sb.toString();
        }
    }

    /**
     * Represents a static analysis warning that doesn't prevent execution.
     *
     * <p>Warnings indicate potential issues or non-optimal code patterns
     * that should be reviewed by the developer.</p>
     */
    public static class LintWarning {
        private final int line;
        private final int column;
        private final String fileName;
        private final String className;
        private final String methodName;
        private final String message;
        
        public LintWarning(int line, int column, String fileName, String className, String methodName, String message) {
            this.line = line;
            this.column = column;
            this.fileName = fileName;
            this.className = className;
            this.methodName = methodName;
            this.message = message;
        }
        
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public String getFileName() { return fileName; }
        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (fileName != null) {
                sb.append(fileName).append(":");
            }
            sb.append(line).append(":").append(column).append(": warning: ");
            sb.append(message);
            return sb.toString();
        }
    }

    /**
     * Container for the results of static analysis.
     *
     * <p>Contains all errors and warnings found during analysis and provides
     * utility methods to check analysis status and print formatted reports.</p>
     *
     * @see LintError
     * @see LintWarning
     */
    public static class AnalysisResult {
        private final List<LintError> errors;
        private final List<LintWarning> warnings;
        
        public AnalysisResult(List<LintError> errors, List<LintWarning> warnings) {
            this.errors = errors;
            this.warnings = warnings;
        }
        
        public List<LintError> getErrors() { return errors; }
        public List<LintWarning> getWarnings() { return warnings; }
        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        
        public void printReport() {
            for (LintError error : errors) {
                System.err.println(error);
            }
            for (LintWarning warning : warnings) {
                System.out.println(warning);
            }
            if (!errors.isEmpty()) {
                System.err.println(errors.size() + " error(s), " + warnings.size() + " warning(s)");
            } else if (!warnings.isEmpty()) {
                System.out.println(warnings.size() + " warning(s)");
            }
        }
    }
    
    private static class ScriptClassInfo {
        private final String name;
        private final int modifiers;
        private final Map<String, FieldInfo> fields;
        private final Map<String, List<MethodInfo>> methods;
        private final Set<String> permittedSubclasses;
        private boolean isStatic;
        
        public ScriptClassInfo(String name, int modifiers) {
            this.name = name;
            this.modifiers = modifiers;
            this.fields = new HashMap<>();
            this.methods = new HashMap<>();
            this.permittedSubclasses = new HashSet<>();
        }
        
        public String getName() { return name; }
        public int getModifiers() { return modifiers; }
        public boolean isStatic() { return isStatic; }
        public void setStatic(boolean isStatic) { this.isStatic = isStatic; }
        public boolean isSealed() { return (modifiers & Modifier.SEALED) != 0; }
        public boolean isFinal() { return (modifiers & Modifier.FINAL) != 0; }
        
        public void addPermittedSubclass(String name) {
            permittedSubclasses.add(name);
        }
        
        public boolean isPermittedSubclass(String name) {
            return permittedSubclasses.contains(name);
        }
        
        public void addField(String name, int modifiers) {
            fields.put(name, new FieldInfo(name, modifiers));
        }
        
        public FieldInfo getField(String name) {
            return fields.get(name);
        }
        
        public boolean hasField(String name) {
            return fields.containsKey(name);
        }
        
        public void addMethod(String name, int modifiers, List<ParameterDeclaration> params) {
            methods.computeIfAbsent(name, k -> new ArrayList<>())
                .add(new MethodInfo(name, modifiers, params != null ? params.size() : 0));
        }
        
        public MethodInfo getMethod(String name) {
            List<MethodInfo> methodList = methods.get(name);
            return methodList != null && !methodList.isEmpty() ? methodList.get(0) : null;
        }
    }
    
    private static class FieldInfo {
        private final String name;
        private final int modifiers;
        
        public FieldInfo(String name, int modifiers) {
            this.name = name;
            this.modifiers = modifiers;
        }
        
        public String getName() { return name; }
        public int getModifiers() { return modifiers; }
        public boolean isStatic() { return (modifiers & Modifier.STATIC) != 0; }
    }
    
    private static class MethodInfo {
        private final String name;
        private final int modifiers;
        private final int paramCount;
        
        public MethodInfo(String name, int modifiers, int paramCount) {
            this.name = name;
            this.modifiers = modifiers;
            this.paramCount = paramCount;
        }
        
        public String getName() { return name; }
        public int getModifiers() { return modifiers; }
        public int getParamCount() { return paramCount; }
        public boolean isStatic() { return (modifiers & Modifier.STATIC) != 0; }
    }
}
