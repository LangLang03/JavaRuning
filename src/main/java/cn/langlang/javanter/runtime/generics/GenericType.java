package cn.langlang.javanter.runtime.generics;

import java.util.*;

public interface GenericType {
    String getTypeName();
    Class<?> getRawType();
    GenericType getErasedType();
    boolean isAssignableFrom(GenericType other);
    boolean isParameterized();
    boolean isTypeVariable();
    boolean isWildcard();
}
