package cn.langlang.javainterpreter.interpreter.exception;

public class ContinueException extends RuntimeException {
    private final String label;
    
    public ContinueException(String label) {
        this.label = label;
    }
    
    public String getLabel() { return label; }
}
