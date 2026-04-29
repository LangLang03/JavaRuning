package cn.langlang.javanter.runtime.model;

import cn.langlang.javanter.interpreter.ExecutionContext;
import java.util.List;

public interface Callable {
    Object call(ExecutionContext context, Object target, List<Object> arguments);
    
    default boolean isStatic() {
        return false;
    }
    
    default String getName() {
        return null;
    }
}
