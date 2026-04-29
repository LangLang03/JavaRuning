package cn.langlang.javanter.interpreter.exception;

public class BreakException extends RuntimeException {
    private final String label;
    
    public BreakException(String label) {
        this.label = label;
    }
    
    public String getLabel() { return label; }
}
