package cn.langlang.javainterpreter.interpreter.exception;

public class BreakException extends RuntimeException {
    private final String label;
    
    public BreakException(String label) {
        this.label = label;
    }
    
    public String getLabel() { return label; }
}
