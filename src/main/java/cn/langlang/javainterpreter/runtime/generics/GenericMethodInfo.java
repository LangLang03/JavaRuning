package cn.langlang.javainterpreter.runtime.generics;

import cn.langlang.javainterpreter.ast.declaration.MethodDeclaration;
import cn.langlang.javainterpreter.ast.declaration.ParameterDeclaration;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.ast.type.TypeParameter;
import cn.langlang.javainterpreter.runtime.model.ScriptClass;
import cn.langlang.javainterpreter.runtime.model.ScriptMethod;
import java.util.*;

public class GenericMethodInfo {
    private final ScriptMethod scriptMethod;
    private final List<TypeParameter> typeParameters;
    private final GenericType genericReturnType;
    private final List<GenericType> genericParameterTypes;
    private final Map<String, TypeVariableImpl> typeVariableMap;
    private final Map<String, GenericType> typeBindings;
    
    public GenericMethodInfo(ScriptMethod scriptMethod, List<TypeParameter> typeParameters,
                            GenericType genericReturnType, List<GenericType> genericParameterTypes) {
        this.scriptMethod = scriptMethod;
        this.typeParameters = typeParameters != null ? new ArrayList<>(typeParameters) : new ArrayList<>();
        this.genericReturnType = genericReturnType;
        this.genericParameterTypes = genericParameterTypes != null ? 
            new ArrayList<>(genericParameterTypes) : new ArrayList<>();
        this.typeVariableMap = new HashMap<>();
        this.typeBindings = new HashMap<>();
        
        initializeTypeVariables();
    }
    
    private void initializeTypeVariables() {
        for (int i = 0; i < typeParameters.size(); i++) {
            TypeParameter tp = typeParameters.get(i);
            List<GenericType> bounds = new ArrayList<>();
            for (Type bound : tp.getBounds()) {
                bounds.add(new ClassTypeImpl(bound.getName()));
            }
            TypeVariableImpl typeVar = new TypeVariableImpl(tp.getName(), bounds, this, i);
            typeVariableMap.put(tp.getName(), typeVar);
        }
    }
    
    public ScriptMethod getScriptMethod() {
        return scriptMethod;
    }
    
    public List<TypeParameter> getTypeParameters() {
        return Collections.unmodifiableList(typeParameters);
    }
    
    public GenericType getGenericReturnType() {
        return genericReturnType;
    }
    
    public List<GenericType> getGenericParameterTypes() {
        return Collections.unmodifiableList(genericParameterTypes);
    }
    
    public TypeVariableImpl getTypeVariable(String name) {
        return typeVariableMap.get(name);
    }
    
    public Map<String, TypeVariableImpl> getTypeVariableMap() {
        return Collections.unmodifiableMap(typeVariableMap);
    }
    
    public void bindTypeVariable(String name, GenericType type) {
        typeBindings.put(name, type);
    }
    
    public GenericType getBinding(String name) {
        return typeBindings.get(name);
    }
    
    public GenericType resolveType(GenericType type) {
        if (type == null) return null;
        
        if (type.isTypeVariable()) {
            TypeVariableImpl typeVar = (TypeVariableImpl) type;
            GenericType binding = typeBindings.get(typeVar.getName());
            if (binding != null) {
                return binding;
            }
            return type;
        }
        
        return type;
    }
    
    public GenericType getErasedReturnType() {
        if (genericReturnType == null) return null;
        GenericType resolved = resolveType(genericReturnType);
        return resolved.getErasedType();
    }
    
    public List<GenericType> getErasedParameterTypes() {
        List<GenericType> erased = new ArrayList<>();
        for (GenericType paramType : genericParameterTypes) {
            GenericType resolved = resolveType(paramType);
            erased.add(resolved.getErasedType());
        }
        return erased;
    }
    
    public boolean isGenericMethod() {
        return !typeParameters.isEmpty();
    }
    
    public GenericMethodInfo withTypeBindings(Map<String, GenericType> bindings) {
        GenericMethodInfo copy = new GenericMethodInfo(scriptMethod, typeParameters, 
            genericReturnType, genericParameterTypes);
        copy.typeBindings.putAll(this.typeBindings);
        copy.typeBindings.putAll(bindings);
        return copy;
    }
    
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(scriptMethod.getName()).append("(");
        for (int i = 0; i < genericParameterTypes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(genericParameterTypes.get(i).getTypeName());
        }
        sb.append(")");
        if (genericReturnType != null) {
            sb.append(":").append(genericReturnType.getTypeName());
        }
        return sb.toString();
    }
}
