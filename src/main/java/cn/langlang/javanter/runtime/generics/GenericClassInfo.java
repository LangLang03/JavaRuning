package cn.langlang.javanter.runtime.generics;

import cn.langlang.javanter.ast.declaration.FieldDeclaration;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.type.TypeParameter;
import cn.langlang.javanter.runtime.model.ScriptClass;
import cn.langlang.javanter.runtime.model.ScriptField;
import cn.langlang.javanter.runtime.model.ScriptMethod;
import java.util.*;

public class GenericClassInfo {
    private final ScriptClass scriptClass;
    private final List<TypeParameter> typeParameters;
    private final GenericType genericSuperClass;
    private final List<GenericType> genericInterfaces;
    private final Map<String, TypeVariableImpl> typeVariableMap;
    private final Map<String, GenericType> typeBindings;
    private final Map<String, GenericMethodInfo> genericMethods;
    private final Map<String, GenericType> genericFields;
    private final List<ScriptMethod> bridgeMethods;
    
    public GenericClassInfo(ScriptClass scriptClass, List<TypeParameter> typeParameters,
                           GenericType genericSuperClass, List<GenericType> genericInterfaces) {
        this.scriptClass = scriptClass;
        this.typeParameters = typeParameters != null ? new ArrayList<>(typeParameters) : new ArrayList<>();
        this.genericSuperClass = genericSuperClass;
        this.genericInterfaces = genericInterfaces != null ? 
            new ArrayList<>(genericInterfaces) : new ArrayList<>();
        this.typeVariableMap = new HashMap<>();
        this.typeBindings = new HashMap<>();
        this.genericMethods = new HashMap<>();
        this.genericFields = new HashMap<>();
        this.bridgeMethods = new ArrayList<>();
        
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
    
    public ScriptClass getScriptClass() {
        return scriptClass;
    }
    
    public List<TypeParameter> getTypeParameters() {
        return Collections.unmodifiableList(typeParameters);
    }
    
    public GenericType getGenericSuperClass() {
        return genericSuperClass;
    }
    
    public List<GenericType> getGenericInterfaces() {
        return Collections.unmodifiableList(genericInterfaces);
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
    
    public void registerGenericMethod(String name, GenericMethodInfo methodInfo) {
        genericMethods.put(name, methodInfo);
    }
    
    public GenericMethodInfo getGenericMethod(String name) {
        return genericMethods.get(name);
    }
    
    public void registerGenericField(String name, GenericType fieldType) {
        genericFields.put(name, fieldType);
    }
    
    public GenericType getGenericFieldType(String name) {
        return genericFields.get(name);
    }
    
    public void addBridgeMethod(ScriptMethod bridgeMethod) {
        bridgeMethods.add(bridgeMethod);
    }
    
    public List<ScriptMethod> getBridgeMethods() {
        return Collections.unmodifiableList(bridgeMethods);
    }
    
    public boolean isGenericClass() {
        return !typeParameters.isEmpty();
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
        
        if (type.isParameterized()) {
            ParameterizedTypeImpl paramType = (ParameterizedTypeImpl) type;
            List<GenericType> resolvedArgs = new ArrayList<>();
            for (GenericType arg : paramType.getTypeArguments()) {
                resolvedArgs.add(resolveType(arg));
            }
            return new ParameterizedTypeImpl(paramType.getRawType(), resolvedArgs);
        }
        
        return type;
    }
    
    public GenericClassInfo withTypeBindings(Map<String, GenericType> bindings) {
        GenericClassInfo copy = new GenericClassInfo(scriptClass, typeParameters, 
            genericSuperClass, genericInterfaces);
        copy.typeBindings.putAll(this.typeBindings);
        copy.typeBindings.putAll(bindings);
        copy.genericMethods.putAll(this.genericMethods);
        copy.genericFields.putAll(this.genericFields);
        copy.bridgeMethods.addAll(this.bridgeMethods);
        return copy;
    }
    
    public GenericType getErasedSuperClass() {
        if (genericSuperClass == null) return null;
        GenericType resolved = resolveType(genericSuperClass);
        return resolved.getErasedType();
    }
    
    public List<GenericType> getErasedInterfaces() {
        List<GenericType> erased = new ArrayList<>();
        for (GenericType iface : genericInterfaces) {
            GenericType resolved = resolveType(iface);
            erased.add(resolved.getErasedType());
        }
        return erased;
    }
    
    public String getGenericSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(scriptClass.getName());
        if (!typeParameters.isEmpty()) {
            sb.append("<");
            for (int i = 0; i < typeParameters.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(typeParameters.get(i).getName());
            }
            sb.append(">");
        }
        return sb.toString();
    }
}
