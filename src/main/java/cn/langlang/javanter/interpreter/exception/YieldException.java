package cn.langlang.javanter.interpreter.exception;

public class YieldException extends RuntimeException {
    private final Object value;
    
    public YieldException(Object value) {
        super("Yield with value: " + value);
        this.value = value;
    }
    
    public Object getValue() {
        return value;
    }
}
