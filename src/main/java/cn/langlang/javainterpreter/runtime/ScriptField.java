package cn.langlang.javainterpreter.runtime;

import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.parser.Modifier;

public class ScriptField {
    private final String name;
    private final int modifiers;
    private final Type type;
    private final Expression initializer;
    private final ScriptClass declaringClass;
    
    public ScriptField(String name, int modifiers, Type type,
                      Expression initializer, ScriptClass declaringClass) {
        this.name = name;
        this.modifiers = modifiers;
        this.type = type;
        this.initializer = initializer;
        this.declaringClass = declaringClass;
    }
    
    public String getName() { return name; }
    public int getModifiers() { return modifiers; }
    public Type getType() { return type; }
    public Expression getInitializer() { return initializer; }
    public ScriptClass getDeclaringClass() { return declaringClass; }
    
    public boolean isStatic() {
        return (modifiers & Modifier.STATIC) != 0;
    }
    
    public boolean isFinal() {
        return (modifiers & Modifier.FINAL) != 0;
    }
}
