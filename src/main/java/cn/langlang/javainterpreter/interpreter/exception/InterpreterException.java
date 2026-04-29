package cn.langlang.javainterpreter.interpreter.exception;

import cn.langlang.javainterpreter.runtime.model.ScriptClass;
import java.util.ArrayList;
import java.util.List;

public class InterpreterException extends RuntimeException {
    private final List<InterpreterStackTraceElement> interpreterStackTrace;
    private Throwable cause;
    private ScriptClass exceptionClass;
    
    public InterpreterException(String message) {
        super(message);
        this.interpreterStackTrace = new ArrayList<>();
    }
    
    public InterpreterException(String message, Throwable cause) {
        super(message, cause);
        this.interpreterStackTrace = new ArrayList<>();
        this.cause = cause;
    }
    
    public InterpreterException(String message, ScriptClass exceptionClass) {
        super(message);
        this.interpreterStackTrace = new ArrayList<>();
        this.exceptionClass = exceptionClass;
    }
    
    public InterpreterException(String message, Throwable cause, ScriptClass exceptionClass) {
        super(message, cause);
        this.interpreterStackTrace = new ArrayList<>();
        this.cause = cause;
        this.exceptionClass = exceptionClass;
    }
    
    public ScriptClass getExceptionClass() {
        return exceptionClass;
    }
    
    public void setExceptionClass(ScriptClass exceptionClass) {
        this.exceptionClass = exceptionClass;
    }
    
    public void addStackTraceElement(InterpreterStackTraceElement element) {
        interpreterStackTrace.add(0, element);
    }
    
    public void addStackTraceElement(String className, String methodName, String fileName, int lineNumber) {
        interpreterStackTrace.add(0, new InterpreterStackTraceElement(className, methodName, fileName, lineNumber));
    }
    
    public List<InterpreterStackTraceElement> getInterpreterStackTrace() {
        return new ArrayList<>(interpreterStackTrace);
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getMessage());
        return sb.toString();
    }
    
    public String getFullStackTrace() {
        StringBuilder sb = new StringBuilder();
        sb.append("Exception in thread \"main\" ").append(this.getClass().getName());
        sb.append(": ").append(getMessage()).append("\n");
        
        for (InterpreterStackTraceElement element : interpreterStackTrace) {
            sb.append(element.toString()).append("\n");
        }
        
        if (cause != null) {
            sb.append("Caused by: ").append(cause.getClass().getName());
            sb.append(": ").append(cause.getMessage()).append("\n");
            for (StackTraceElement element : cause.getStackTrace()) {
                sb.append("\tat ").append(element.toString()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    @Override
    public void printStackTrace() {
        System.err.println(getFullStackTrace());
    }
    
    @Override
    public void printStackTrace(java.io.PrintStream s) {
        s.println(getFullStackTrace());
    }
    
    @Override
    public void printStackTrace(java.io.PrintWriter s) {
        s.println(getFullStackTrace());
    }
}
