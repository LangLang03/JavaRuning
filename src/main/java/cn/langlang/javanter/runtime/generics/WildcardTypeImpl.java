package cn.langlang.javanter.runtime.generics;

import java.util.*;

public class WildcardTypeImpl implements GenericType {
    public enum WildcardKind {
        UNBOUNDED,
        EXTENDS,
        SUPER
    }
    
    private final WildcardKind kind;
    private final List<GenericType> upperBounds;
    private final GenericType lowerBound;
    
    public WildcardTypeImpl() {
        this(WildcardKind.UNBOUNDED, Collections.emptyList(), null);
    }
    
    public WildcardTypeImpl(WildcardKind kind, List<GenericType> upperBounds, GenericType lowerBound) {
        this.kind = kind;
        this.upperBounds = upperBounds != null ? new ArrayList<>(upperBounds) : new ArrayList<>();
        this.lowerBound = lowerBound;
    }
    
    public static WildcardTypeImpl unbounded() {
        return new WildcardTypeImpl(WildcardKind.UNBOUNDED, Collections.emptyList(), null);
    }
    
    public static WildcardTypeImpl extendsBound(GenericType bound) {
        return new WildcardTypeImpl(WildcardKind.EXTENDS, Collections.singletonList(bound), null);
    }
    
    public static WildcardTypeImpl superBound(GenericType bound) {
        return new WildcardTypeImpl(WildcardKind.SUPER, Collections.emptyList(), bound);
    }
    
    public WildcardKind getKind() {
        return kind;
    }
    
    public List<GenericType> getUpperBounds() {
        return Collections.unmodifiableList(upperBounds);
    }
    
    public GenericType getLowerBound() {
        return lowerBound;
    }
    
    @Override
    public String getTypeName() {
        switch (kind) {
            case UNBOUNDED:
                return "?";
            case EXTENDS:
                return "? extends " + upperBounds.get(0).getTypeName();
            case SUPER:
                return "? super " + lowerBound.getTypeName();
            default:
                return "?";
        }
    }
    
    @Override
    public Class<?> getRawType() {
        switch (kind) {
            case UNBOUNDED:
                return Object.class;
            case EXTENDS:
                return upperBounds.get(0).getRawType();
            case SUPER:
                return Object.class;
            default:
                return Object.class;
        }
    }
    
    @Override
    public GenericType getErasedType() {
        switch (kind) {
            case UNBOUNDED:
                return new ClassTypeImpl(Object.class);
            case EXTENDS:
                return upperBounds.get(0).getErasedType();
            case SUPER:
                return new ClassTypeImpl(Object.class);
            default:
                return new ClassTypeImpl(Object.class);
        }
    }
    
    @Override
    public boolean isAssignableFrom(GenericType other) {
        if (other == null) return false;
        
        switch (kind) {
            case UNBOUNDED:
                return true;
                
            case EXTENDS:
                for (GenericType upperBound : upperBounds) {
                    if (upperBound.isAssignableFrom(other)) {
                        return true;
                    }
                }
                return false;
                
            case SUPER:
                if (lowerBound == null) return true;
                if (other instanceof WildcardTypeImpl) {
                    WildcardTypeImpl otherWildcard = (WildcardTypeImpl) other;
                    if (otherWildcard.kind == WildcardKind.SUPER && otherWildcard.lowerBound != null) {
                        return lowerBound.isAssignableFrom(otherWildcard.lowerBound);
                    }
                }
                return lowerBound.isAssignableFrom(other);
                
            default:
                return false;
        }
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
        return true;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WildcardTypeImpl)) return false;
        WildcardTypeImpl other = (WildcardTypeImpl) obj;
        return kind == other.kind &&
               upperBounds.equals(other.upperBounds) &&
               Objects.equals(lowerBound, other.lowerBound);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(kind, upperBounds, lowerBound);
    }
    
    @Override
    public String toString() {
        return getTypeName();
    }
}
