package cn.langlang.javanter.ast.misc;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.declaration.ClassDeclaration;
import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.lexer.Token;
import java.util.*;

public class EnumConstant extends ASTNode {
    private final String name;
    private final List<Expression> arguments;
    private final ClassDeclaration anonymousClass;
    private final List<Annotation> annotations;
    
    public EnumConstant(Token token, String name, List<Expression> arguments,
                       ClassDeclaration anonymousClass, List<Annotation> annotations) {
        super(token);
        this.name = name;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
        this.anonymousClass = anonymousClass;
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }
    
    public String getName() { return name; }
    public List<Expression> getArguments() { return arguments; }
    public ClassDeclaration getAnonymousClass() { return anonymousClass; }
    public List<Annotation> getAnnotations() { return annotations; }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitEnumConstant(this);
    }
}
