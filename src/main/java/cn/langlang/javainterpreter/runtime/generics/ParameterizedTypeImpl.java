package cn.langlang.javainterpreter.runtime.generics;

import cn.langlang.javainterpreter.runtime.model.ScriptClass;
import java.util.*;

public class ParameterizedTypeImpl implements GenericType {
    private final Class<?> rawType;
    private final ScriptClass rawScriptClass;
    private final String rawTypeName;
    private final List<GenericType> typeArguments;
    private final GenericType ownerType;
    
    public ParameterizedTypeImpl(Class<?> rawType, List<GenericType> typeArguments) {
        this(rawType, typeArguments, null);
    }
    
    public ParameterizedTypeImpl(Class<?> rawType, List<GenericType> typeArguments, GenericType ownerType) {
        this.rawType = rawType;
        this.rawScriptClass = null;
        this.rawTypeName = null;
        this.typeArguments = typeArguments != null ? new ArrayList<>(typeArguments) : new ArrayList<>();
        this.ownerType = ownerType;
    }
    
    public ParameterizedTypeImpl(ScriptClass rawScriptClass, List<GenericType> typeArguments) {
        this.rawScriptClass = rawScriptClass;
        this.rawType = null;
        this.rawTypeName = null;
        this.typeArguments = typeArguments != null ? new ArrayList<>(typeArguments) : new ArrayList<>();
        this.ownerType = null;
    }
    
    public ParameterizedTypeImpl(String rawTypeName, List<GenericType> typeArguments) {
        this.rawTypeName = rawTypeName;
        this.rawType = null;
        this.rawScriptClass = null;
        this.typeArguments = typeArguments != null ? new ArrayList<>(typeArguments) : new ArrayList<>();
        this.ownerType = null;
    }
    
    @Override
    public String getTypeName() {
        StringBuilder sb = new StringBuilder();
        if (rawType != null) {
            sb.append(rawType.getSimpleName());
        } else if (rawScriptClass != null) {
            sb.append(rawScriptClass.getName());
        } else if (rawTypeName != null) {
            sb.append(rawTypeName);
        }
        
        if (!typeArguments.isEmpty()) {
            sb.append("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(typeArguments.get(i).getTypeName());
            }
            sb.append(">");
        }
        return sb.toString();
    }
    
    @Override
    public Class<?> getRawType() {
        return rawType;
    }
    
    public ScriptClass getRawScriptClass() {
        return rawScriptClass;
    }
    
    public String getRawTypeName() {
        return rawTypeName;
    }
    
    public List<GenericType> getTypeArguments() {
        return Collections.unmodifiableList(typeArguments);
    }
    
    public GenericType getOwnerType() {
        return ownerType;
    }
    
    @Override
    public GenericType getErasedType() {
        if (rawType != null) {
            return new ClassTypeImpl(rawType);
        } else if (rawScriptClass != null) {
            return new ClassTypeImpl(rawScriptClass);
        } else if (rawTypeName != null) {
            return new ClassTypeImpl(rawTypeName);
        }
        return this;
    }
    
    @Override
    public boolean isAssignableFrom(GenericType other) {
        if (other == null) return false;
        
        if (other instanceof ParameterizedTypeImpl) {
            ParameterizedTypeImpl otherParam = (ParameterizedTypeImpl) other;
            
            if (rawType != null && otherParam.rawType != null) {
                if (!rawType.equals(otherParam.rawType)) {
                    if (rawType.isAssignableFrom(otherParam.rawType)) {
                        return checkTypeArgumentsForSubclass(otherParam);
                    }
                    return false;
                }
                return checkTypeArguments(typeArguments, otherParam.typeArguments);
            }
            
            if (rawScriptClass != null && otherParam.rawScriptClass != null) {
                if (!rawScriptClass.equals(otherParam.rawScriptClass)) {
                    return false;
                }
                return checkTypeArguments(typeArguments, otherParam.typeArguments);
            }
            
            if (rawTypeName != null && otherParam.rawTypeName != null) {
                if (!rawTypeName.equals(otherParam.rawTypeName)) {
                    return false;
                }
                return checkTypeArguments(typeArguments, otherParam.typeArguments);
            }
        }
        
        if (other instanceof ClassTypeImpl) {
            ClassTypeImpl otherClass = (ClassTypeImpl) other;
            if (rawType != null && otherClass.getJavaClass() != null) {
                return rawType.equals(otherClass.getJavaClass());
            }
            if (rawScriptClass != null && otherClass.getScriptClass() != null) {
                return rawScriptClass.equals(otherClass.getScriptClass());
            }
            if (rawTypeName != null && otherClass.getTypeName() != null) {
                return rawTypeName.equals(otherClass.getTypeName());
            }
        }
        
        if (other instanceof TypeVariableImpl) {
            TypeVariableImpl var = (TypeVariableImpl) other;
            for (GenericType bound : var.getBounds()) {
                if (isAssignableFrom(bound)) {
                    return true;
                }
            }
            return false;
        }
        
        return false;
    }
    
    private boolean checkTypeArgumentsForSubclass(ParameterizedTypeImpl other) {
        return true;
    }
    
    private boolean checkTypeArguments(List<GenericType> args1, List<GenericType> args2) {
        if (args1.size() != args2.size()) {
            return false;
        }
        
        for (int i = 0; i < args1.size(); i++) {
            GenericType arg1 = args1.get(i);
            GenericType arg2 = args2.get(i);
            
            if (!isTypeArgumentCompatible(arg1, arg2)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isTypeArgumentCompatible(GenericType target, GenericType source) {
        if (target instanceof WildcardTypeImpl) {
            WildcardTypeImpl wildcard = (WildcardTypeImpl) target;
            return wildcard.isAssignableFrom(source);
        }
        
        if (target instanceof TypeVariableImpl) {
            return true;
        }
        
        if (target.isParameterized() && source.isParameterized()) {
            return target.isAssignableFrom(source);
        }
        
        if (!target.isParameterized() && !source.isParameterized()) {
            return target.getTypeName().equals(source.getTypeName());
        }
        
        return false;
    }
    
    @Override
    public boolean isParameterized() {
        return true;
    }
    
    @Override
    public boolean isTypeVariable() {
        return false;
    }
    
    @Override
    public boolean isWildcard() {
        return false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ParameterizedTypeImpl)) return false;
        ParameterizedTypeImpl other = (ParameterizedTypeImpl) obj;
        return Objects.equals(rawType, other.rawType) &&
               Objects.equals(rawScriptClass, other.rawScriptClass) &&
               Objects.equals(rawTypeName, other.rawTypeName) &&
               typeArguments.equals(other.typeArguments);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(rawType, rawScriptClass, rawTypeName, typeArguments);
    }
    
    @Override
    public String toString() {
        return getTypeName();
    }
}
