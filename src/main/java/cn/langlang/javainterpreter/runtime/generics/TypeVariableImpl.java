package cn.langlang.javainterpreter.runtime.generics;

import cn.langlang.javainterpreter.runtime.model.ScriptClass;
import java.util.*;

public class TypeVariableImpl implements GenericType {
    private final String name;
    private final List<GenericType> bounds;
    private final Object genericDeclaration;
    private final int index;
    
    public TypeVariableImpl(String name, List<GenericType> bounds) {
        this(name, bounds, null, -1);
    }
    
    public TypeVariableImpl(String name, List<GenericType> bounds, Object genericDeclaration, int index) {
        this.name = name;
        this.bounds = bounds != null ? new ArrayList<>(bounds) : new ArrayList<>();
        this.genericDeclaration = genericDeclaration;
        this.index = index;
    }
    
    public String getName() {
        return name;
    }
    
    public List<GenericType> getBounds() {
        return Collections.unmodifiableList(bounds);
    }
    
    public Object getGenericDeclaration() {
        return genericDeclaration;
    }
    
    public int getIndex() {
        return index;
    }
    
    @Override
    public String getTypeName() {
        return name;
    }
    
    @Override
    public Class<?> getRawType() {
        if (bounds.isEmpty()) {
            return Object.class;
        }
        return bounds.get(0).getRawType();
    }
    
    @Override
    public GenericType getErasedType() {
        if (bounds.isEmpty()) {
            return new ClassTypeImpl(Object.class);
        }
        return bounds.get(0).getErasedType();
    }
    
    @Override
    public boolean isAssignableFrom(GenericType other) {
        if (other == null) return false;
        
        for (GenericType bound : bounds) {
            if (bound.isAssignableFrom(other)) {
                return true;
            }
        }
        
        if (bounds.isEmpty()) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean isParameterized() {
        return false;
    }
    
    @Override
    public boolean isTypeVariable() {
        return true;
    }
    
    @Override
    public boolean isWildcard() {
        return false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TypeVariableImpl)) return false;
        TypeVariableImpl other = (TypeVariableImpl) obj;
        return name.equals(other.name) &&
               bounds.equals(other.bounds);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, bounds);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        if (!bounds.isEmpty()) {
            sb.append(" extends ");
            for (int i = 0; i < bounds.size(); i++) {
                if (i > 0) sb.append(" & ");
                sb.append(bounds.get(i).getTypeName());
            }
        }
        return sb.toString();
    }
}
