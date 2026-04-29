package cn.langlang.javainterpreter.runtime.model;

import cn.langlang.javainterpreter.ast.declaration.ParameterDeclaration;
import cn.langlang.javainterpreter.ast.statement.BlockStatement;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.ast.type.TypeParameter;
import cn.langlang.javainterpreter.ast.misc.Annotation;
import cn.langlang.javainterpreter.interpreter.ExecutionContext;
import cn.langlang.javainterpreter.parser.Modifier;
import cn.langlang.javainterpreter.runtime.generics.*;
import java.util.*;
import java.util.function.Function;

public class ScriptMethod implements Callable {
    private final String name;
    private final int modifiers;
    private final Type returnType;
    private final List<ParameterDeclaration> parameters;
    private final boolean isVarArgs;
    private final BlockStatement body;
    private final ScriptClass declaringClass;
    private final boolean isConstructor;
    private final boolean isDefault;
    private final List<Annotation> annotations;
    private Function<Object[], Object> nativeImplementation;
    
    private List<TypeParameter> typeParameters;
    private GenericMethodInfo genericInfo;
    private Map<String, GenericType> typeBindings;
    
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
        this.annotations = new ArrayList<>();
        this.typeParameters = new ArrayList<>();
        this.typeBindings = new HashMap<>();
    }
    
    public ScriptMethod(String name, int modifiers, Type returnType,
                       List<ParameterDeclaration> parameters, boolean isVarArgs,
                       BlockStatement body, ScriptClass declaringClass,
                       boolean isConstructor, boolean isDefault,
                       List<Annotation> annotations) {
        this.name = name;
        this.modifiers = modifiers;
        this.returnType = returnType;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.isVarArgs = isVarArgs;
        this.body = body;
        this.declaringClass = declaringClass;
        this.isConstructor = isConstructor;
        this.isDefault = isDefault;
        this.annotations = annotations != null ? annotations : new ArrayList<>();
        this.typeParameters = new ArrayList<>();
        this.typeBindings = new HashMap<>();
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
    public List<Annotation> getAnnotations() { return annotations; }
    
    public List<TypeParameter> getTypeParameters() { return typeParameters; }
    public GenericMethodInfo getGenericInfo() { return genericInfo; }
    public Map<String, GenericType> getTypeBindings() { return typeBindings; }
    
    public void setTypeParameters(List<TypeParameter> typeParameters) {
        this.typeParameters = typeParameters != null ? new ArrayList<>(typeParameters) : new ArrayList<>();
    }
    
    public void setGenericInfo(GenericMethodInfo genericInfo) {
        this.genericInfo = genericInfo;
    }
    
    public void bindTypeParameter(String name, GenericType type) {
        typeBindings.put(name, type);
        if (genericInfo != null) {
            genericInfo.bindTypeVariable(name, type);
        }
    }
    
    public GenericType getTypeBinding(String name) {
        return typeBindings.get(name);
    }
    
    public boolean isGenericMethod() {
        return typeParameters != null && !typeParameters.isEmpty();
    }
    
    public ScriptMethod withTypeBindings(Map<String, GenericType> bindings) {
        ScriptMethod copy = new ScriptMethod(name, modifiers, returnType, parameters, isVarArgs, 
            body, declaringClass, isConstructor, isDefault, annotations);
        copy.nativeImplementation = this.nativeImplementation;
        copy.typeParameters = this.typeParameters;
        copy.genericInfo = this.genericInfo;
        copy.typeBindings.putAll(this.typeBindings);
        copy.typeBindings.putAll(bindings);
        return copy;
    }
    
    public GenericType resolveReturnType() {
        if (returnType == null) return null;
        
        String typeName = returnType.getName();
        
        if (typeBindings.containsKey(typeName)) {
            return typeBindings.get(typeName);
        }
        
        if (declaringClass != null && declaringClass.getTypeBinding(typeName) != null) {
            return declaringClass.getTypeBinding(typeName);
        }
        
        return new ClassTypeImpl(typeName);
    }
    
    public List<GenericType> resolveParameterTypes() {
        List<GenericType> resolved = new ArrayList<>();
        for (ParameterDeclaration param : parameters) {
            Type paramType = param.getType();
            String typeName = paramType.getName();
            
            if (typeBindings.containsKey(typeName)) {
                resolved.add(typeBindings.get(typeName));
            } else if (declaringClass != null && declaringClass.getTypeBinding(typeName) != null) {
                resolved.add(declaringClass.getTypeBinding(typeName));
            } else {
                resolved.add(new ClassTypeImpl(typeName));
            }
        }
        return resolved;
    }
    
    public Function<Object[], Object> getNativeImplementation() {
        return nativeImplementation;
    }
    
    public void setNativeImplementation(Function<Object[], Object> nativeImplementation) {
        this.nativeImplementation = nativeImplementation;
    }
    
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
    
    public boolean isAbstract() {
        return (modifiers & Modifier.ABSTRACT) != 0;
    }
    
    public boolean isFinal() {
        return (modifiers & Modifier.FINAL) != 0;
    }
    
    public boolean isPrivate() {
        return (modifiers & Modifier.PRIVATE) != 0;
    }
    
    public boolean isBridge() {
        return (modifiers & Modifier.BRIDGE) != 0;
    }
    
    public boolean isSynthetic() {
        return (modifiers & Modifier.SYNTHETIC) != 0;
    }
    
    @Override
    public Object call(ExecutionContext context, Object target, List<Object> arguments) {
        return context.invokeMethod(target, this, arguments);
    }
}
