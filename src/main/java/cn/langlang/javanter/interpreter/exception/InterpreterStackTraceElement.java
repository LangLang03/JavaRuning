package cn.langlang.javanter.interpreter.exception;

public class InterpreterStackTraceElement {
    private final String className;
    private final String methodName;
    private final String fileName;
    private final int lineNumber;
    
    public InterpreterStackTraceElement(String className, String methodName, String fileName, int lineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\tat ");
        if (className != null && !className.isEmpty()) {
            sb.append(className);
            if (methodName != null && !methodName.isEmpty()) {
                sb.append(".").append(methodName);
            }
        }
        if (fileName != null && !fileName.isEmpty()) {
            sb.append("(").append(fileName);
            if (lineNumber > 0) {
                sb.append(":").append(lineNumber);
            }
            sb.append(")");
        } else {
            sb.append("(Unknown Source)");
        }
        return sb.toString();
    }
}
