package cn.langlang.javanter.runtime.nativesupport;

import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.runtime.model.ScriptClass;
import cn.langlang.javanter.runtime.model.ScriptMethod;
import cn.langlang.javanter.parser.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MethodBuilder {
    private final ScriptClass targetClass;
    private String name;
    private int modifiers = Modifier.PUBLIC;
    private String returnType = "void";
    private final List<String> paramTypes = new ArrayList<>();
    private final List<String> paramNames = new ArrayList<>();
    private boolean varArgs = false;
    private Function<Object[], Object> nativeImplementation;
    private boolean isStatic = false;
    
    public MethodBuilder(ScriptClass targetClass) {
        this.targetClass = targetClass;
    }
    
    public static MethodBuilder create(ScriptClass targetClass) {
        return new MethodBuilder(targetClass);
    }
    
    public MethodBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    public MethodBuilder modifiers(int modifiers) {
        this.modifiers = modifiers;
        return this;
    }
    
    public MethodBuilder makeStatic() {
        this.isStatic = true;
        this.modifiers |= Modifier.STATIC;
        return this;
    }
    
    public MethodBuilder returnType(String returnType) {
        this.returnType = returnType;
        return this;
    }
    
    public MethodBuilder param(String type, String name) {
        this.paramTypes.add(type);
        this.paramNames.add(name);
        return this;
    }
    
    public MethodBuilder params(String... typeNames) {
        for (int i = 0; i < typeNames.length; i++) {
            this.paramTypes.add(typeNames[i]);
            this.paramNames.add("arg" + i);
        }
        return this;
    }
    
    public MethodBuilder varArgs(boolean varArgs) {
        this.varArgs = varArgs;
        return this;
    }
    
    public MethodBuilder nativeImpl(Function<Object[], Object> implementation) {
        this.nativeImplementation = implementation;
        return this;
    }
    
    public ScriptMethod build() {
        List<cn.langlang.javanter.ast.declaration.ParameterDeclaration> params = new ArrayList<>();
        for (int i = 0; i < paramTypes.size(); i++) {
            params.add(new cn.langlang.javanter.ast.declaration.ParameterDeclaration(
                null, 0, 
                new Type(null, paramTypes.get(i), null, 0, null),
                paramNames.get(i),
                varArgs && i == paramTypes.size() - 1,
                new ArrayList<>()
            ));
        }
        
        ScriptMethod method = new ScriptMethod(
            name,
            modifiers,
            new Type(null, returnType, null, 0, null),
            params,
            varArgs,
            null,
            targetClass,
            false,
            false,
            new ArrayList<>()
        );
        
        if (nativeImplementation != null) {
            method.setNativeImplementation(nativeImplementation);
        }
        
        return method;
    }
    
    public void register() {
        ScriptMethod method = build();
        targetClass.addMethod(method);
    }
}
