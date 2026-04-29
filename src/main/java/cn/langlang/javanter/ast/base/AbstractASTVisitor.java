package cn.langlang.javanter.ast.base;

import cn.langlang.javanter.ast.declaration.*;
import cn.langlang.javanter.ast.statement.*;
import cn.langlang.javanter.ast.expression.*;
import cn.langlang.javanter.ast.type.*;
import cn.langlang.javanter.ast.misc.*;

public abstract class AbstractASTVisitor<R> implements ASTVisitor<R> {
    @Override public R visitCompilationUnit(CompilationUnit node) { return null; }
    @Override public R visitPackageDeclaration(PackageDeclaration node) { return null; }
    @Override public R visitImportDeclaration(ImportDeclaration node) { return null; }
    @Override public R visitClassDeclaration(ClassDeclaration node) { return null; }
    @Override public R visitInterfaceDeclaration(InterfaceDeclaration node) { return null; }
    @Override public R visitEnumDeclaration(EnumDeclaration node) { return null; }
    @Override public R visitAnnotationDeclaration(AnnotationDeclaration node) { return null; }
    @Override public R visitFieldDeclaration(FieldDeclaration node) { return null; }
    @Override public R visitMethodDeclaration(MethodDeclaration node) { return null; }
    @Override public R visitConstructorDeclaration(ConstructorDeclaration node) { return null; }
    @Override public R visitInitializerBlock(InitializerBlock node) { return null; }
    @Override public R visitParameterDeclaration(ParameterDeclaration node) { return null; }
    @Override public R visitLocalVariableDeclaration(LocalVariableDeclaration node) { return null; }
    @Override public R visitBlockStatement(BlockStatement node) { return null; }
    @Override public R visitIfStatement(IfStatement node) { return null; }
    @Override public R visitWhileStatement(WhileStatement node) { return null; }
    @Override public R visitDoStatement(DoStatement node) { return null; }
    @Override public R visitForStatement(ForStatement node) { return null; }
    @Override public R visitForEachStatement(ForEachStatement node) { return null; }
    @Override public R visitSwitchStatement(SwitchStatement node) { return null; }
    @Override public R visitCaseLabel(CaseLabel node) { return null; }
    @Override public R visitReturnStatement(ReturnStatement node) { return null; }
    @Override public R visitThrowStatement(ThrowStatement node) { return null; }
    @Override public R visitTryStatement(TryStatement node) { return null; }
    @Override public R visitCatchClause(CatchClause node) { return null; }
    @Override public R visitSynchronizedStatement(SynchronizedStatement node) { return null; }
    @Override public R visitAssertStatement(AssertStatement node) { return null; }
    @Override public R visitBreakStatement(BreakStatement node) { return null; }
    @Override public R visitContinueStatement(ContinueStatement node) { return null; }
    @Override public R visitLabelStatement(LabelStatement node) { return null; }
    @Override public R visitExpressionStatement(ExpressionStatement node) { return null; }
    @Override public R visitEmptyStatement(EmptyStatement node) { return null; }
    @Override public R visitLocalClassDeclarationStatement(LocalClassDeclarationStatement node) { return null; }
    @Override public R visitLiteralExpression(LiteralExpression node) { return null; }
    @Override public R visitIdentifierExpression(IdentifierExpression node) { return null; }
    @Override public R visitBinaryExpression(BinaryExpression node) { return null; }
    @Override public R visitUnaryExpression(UnaryExpression node) { return null; }
    @Override public R visitTernaryExpression(TernaryExpression node) { return null; }
    @Override public R visitAssignmentExpression(AssignmentExpression node) { return null; }
    @Override public R visitMethodInvocationExpression(MethodInvocationExpression node) { return null; }
    @Override public R visitFieldAccessExpression(FieldAccessExpression node) { return null; }
    @Override public R visitArrayAccessExpression(ArrayAccessExpression node) { return null; }
    @Override public R visitNewObjectExpression(NewObjectExpression node) { return null; }
    @Override public R visitNewArrayExpression(NewArrayExpression node) { return null; }
    @Override public R visitArrayInitializerExpression(ArrayInitializerExpression node) { return null; }
    @Override public R visitCastExpression(CastExpression node) { return null; }
    @Override public R visitInstanceOfExpression(InstanceOfExpression node) { return null; }
    @Override public R visitThisExpression(ThisExpression node) { return null; }
    @Override public R visitSuperExpression(SuperExpression node) { return null; }
    @Override public R visitClassLiteralExpression(ClassLiteralExpression node) { return null; }
    @Override public R visitLambdaExpression(LambdaExpression node) { return null; }
    @Override public R visitMethodReferenceExpression(MethodReferenceExpression node) { return null; }
    @Override public R visitParenthesizedExpression(ParenthesizedExpression node) { return null; }
    @Override public R visitTypeParameter(TypeParameter node) { return null; }
    @Override public R visitTypeArgument(TypeArgument node) { return null; }
    @Override public R visitAnnotation(Annotation node) { return null; }
    @Override public R visitEnumConstant(EnumConstant node) { return null; }
}
