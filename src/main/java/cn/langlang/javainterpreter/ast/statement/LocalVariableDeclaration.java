package cn.langlang.javainterpreter.ast.statement;

import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.ast.expression.Expression;
import cn.langlang.javainterpreter.ast.misc.Annotation;
import cn.langlang.javainterpreter.lexer.Token;
import java.util.*;

public class LocalVariableDeclaration extends Statement {
    private final int modifiers;
    private final Type type;
    private final List<VariableDeclarator> declarators;
    private final List<Annotation> annotations;
    
    public LocalVariableDeclaration(Token token, int modifiers, Type type,
                                   List<VariableDeclarator> declarators, List<Annotation> annotations) {
        super(token);
        this.modifiers = modifiers;
        this.type = type;
        this.declarators = declarators != null ? declarators : new ArrayList<>();
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }
    
    public int getModifiers() { return modifiers; }
    public Type getType() { return type; }
    public List<VariableDeclarator> getDeclarators() { return declarators; }
    public List<Annotation> getAnnotations() { return annotations; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitLocalVariableDeclaration(this);
    }
    
    public static class VariableDeclarator {
        private final String name;
        private final Expression initializer;
        private final int arrayDimensions;
        
        public VariableDeclarator(String name, Expression initializer, int arrayDimensions) {
            this.name = name;
            this.initializer = initializer;
            this.arrayDimensions = arrayDimensions;
        }
        
        public String getName() { return name; }
        public Expression getInitializer() { return initializer; }
        public int getArrayDimensions() { return arrayDimensions; }
    }
}
