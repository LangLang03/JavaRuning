package cn.langlang.javanter.runtime.generics;

import cn.langlang.javanter.runtime.model.ScriptClass;
import java.util.*;

public class ClassTypeImpl implements GenericType {
    private final Class<?> javaClass;
    private final ScriptClass scriptClass;
    private final String typeName;
    
    public ClassTypeImpl(Class<?> javaClass) {
        this.javaClass = javaClass;
        this.scriptClass = null;
        this.typeName = null;
    }
    
    public ClassTypeImpl(ScriptClass scriptClass) {
        this.scriptClass = scriptClass;
        this.javaClass = null;
        this.typeName = null;
    }
    
    public ClassTypeImpl(String typeName) {
        this.typeName = typeName;
        this.javaClass = null;
        this.scriptClass = null;
    }
    
    public Class<?> getJavaClass() {
        return javaClass;
    }
    
    public ScriptClass getScriptClass() {
        return scriptClass;
    }
    
    public String getTypeNameStr() {
        return typeName;
    }
    
    @Override
    public String getTypeName() {
        if (javaClass != null) {
            return javaClass.getSimpleName();
        }
        if (scriptClass != null) {
            return scriptClass.getName();
        }
        if (typeName != null) {
            return typeName;
        }
        return "unknown";
    }
    
    @Override
    public Class<?> getRawType() {
        return javaClass;
    }
    
    @Override
    public GenericType getErasedType() {
        return this;
    }
    
    @Override
    public boolean isAssignableFrom(GenericType other) {
        if (other == null) return false;
        
        if (other instanceof ClassTypeImpl) {
            ClassTypeImpl otherClass = (ClassTypeImpl) other;
            if (javaClass != null && otherClass.javaClass != null) {
                return javaClass.isAssignableFrom(otherClass.javaClass);
            }
            if (scriptClass != null && otherClass.scriptClass != null) {
                return scriptClass.isAssignableFrom(otherClass.scriptClass);
            }
            if (typeName != null && otherClass.typeName != null) {
                return typeName.equals(otherClass.typeName);
            }
        }
        
        if (other instanceof ParameterizedTypeImpl) {
            ParameterizedTypeImpl otherParam = (ParameterizedTypeImpl) other;
            if (javaClass != null && otherParam.getRawType() != null) {
                return javaClass.isAssignableFrom(otherParam.getRawType());
            }
            if (scriptClass != null && otherParam.getRawScriptClass() != null) {
                return scriptClass.isAssignableFrom(otherParam.getRawScriptClass());
            }
            if (typeName != null && otherParam.getRawTypeName() != null) {
                return typeName.equals(otherParam.getRawTypeName());
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
        
        if (other instanceof WildcardTypeImpl) {
            WildcardTypeImpl wildcard = (WildcardTypeImpl) other;
            if (wildcard.getKind() == WildcardTypeImpl.WildcardKind.EXTENDS) {
                for (GenericType upperBound : wildcard.getUpperBounds()) {
                    if (isAssignableFrom(upperBound)) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        return false;
    }
    
    @Override
    public boolean isParameterized() {
        return false;
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
        if (!(obj instanceof ClassTypeImpl)) return false;
        ClassTypeImpl other = (ClassTypeImpl) obj;
        return Objects.equals(javaClass, other.javaClass) &&
               Objects.equals(scriptClass, other.scriptClass) &&
               Objects.equals(typeName, other.typeName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(javaClass, scriptClass, typeName);
    }
    
    @Override
    public String toString() {
        return getTypeName();
    }
}
