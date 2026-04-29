package cn.langlang.javanter.runtime.model;

import cn.langlang.javanter.ast.expression.Expression;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.misc.Annotation;
import cn.langlang.javanter.parser.Modifier;
import java.util.*;

public class ScriptField {
    private final String name;
    private final int modifiers;
    private final Type type;
    private final Expression initializer;
    private final ScriptClass declaringClass;
    private final List<Annotation> annotations;
    
    public ScriptField(String name, int modifiers, Type type,
                      Expression initializer, ScriptClass declaringClass) {
        this.name = name;
        this.modifiers = modifiers;
        this.type = type;
        this.initializer = initializer;
        this.declaringClass = declaringClass;
        this.annotations = new ArrayList<>();
    }
    
    public ScriptField(String name, int modifiers, Type type,
                      Expression initializer, ScriptClass declaringClass,
                      List<Annotation> annotations) {
        this.name = name;
        this.modifiers = modifiers;
        this.type = type;
        this.initializer = initializer;
        this.declaringClass = declaringClass;
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }
    
    public String getName() { return name; }
    public int getModifiers() { return modifiers; }
    public Type getType() { return type; }
    public Expression getInitializer() { return initializer; }
    public ScriptClass getDeclaringClass() { return declaringClass; }
    public List<Annotation> getAnnotations() { return annotations; }
    
    public Annotation getAnnotation(String annotationName) {
        for (Annotation ann : annotations) {
            if (ann.getTypeName().equals(annotationName) || 
                ann.getTypeName().endsWith("." + annotationName)) {
                return ann;
            }
        }
        return null;
    }
    
    public boolean isStatic() {
        return (modifiers & Modifier.STATIC) != 0;
    }
    
    public boolean isFinal() {
        return (modifiers & Modifier.FINAL) != 0;
    }
}
