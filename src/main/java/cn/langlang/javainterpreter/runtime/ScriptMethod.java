package cn.langlang.javainterpreter.runtime;

import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.parser.Modifier;
import java.util.*;

public class ScriptMethod {
    private final String name;
    private final int modifiers;
    private final Type returnType;
    private final List<ParameterDeclaration> parameters;
    private final boolean isVarArgs;
    private final BlockStatement body;
    private final ScriptClass declaringClass;
    private final boolean isConstructor;
    private final boolean isDefault;
    
    public ScriptMethod(String name, int modifiers, Type returnType,
                       List<ParameterDeclaration> parameters, boolean isVarArgs,
                       BlockStatement body, ScriptClass declaringClass,
                       boolean isConstructor, boolean isDefault) {
        this.name = name;
        this.modifiers = modifiers;
        this.returnType = returnType;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.isVarArgs = isVarArgs;
        this.body = body;
        this.declaringClass = declaringClass;
        this.isConstructor = isConstructor;
        this.isDefault = isDefault;
    }
    
    public String getName() { return name; }
    public int getModifiers() { return modifiers; }
    public Type getReturnType() { return returnType; }
    public List<ParameterDeclaration> getParameters() { return parameters; }
    public boolean isVarArgs() { return isVarArgs; }
    public BlockStatement getBody() { return body; }
    public ScriptClass getDeclaringClass() { return declaringClass; }
    public boolean isConstructor() { return isConstructor; }
    public boolean isDefault() { return isDefault; }
    
    public boolean isStatic() {
        return (modifiers & Modifier.STATIC) != 0;
    }
    
    public boolean isAbstract() {
        return (modifiers & Modifier.ABSTRACT) != 0;
    }
}
