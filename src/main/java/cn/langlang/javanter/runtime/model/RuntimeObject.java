package cn.langlang.javanter.runtime.model;

import cn.langlang.javanter.interpreter.Interpreter;
import java.util.*;

public class RuntimeObject {
    private static final ThreadLocal<Interpreter> currentInterpreter = new ThreadLocal<>();
    
    private final ScriptClass scriptClass;
    private final Map<String, Object> fields;
    private Map<String, Object> capturedVariables;
    
    public static void setCurrentInterpreter(Interpreter interpreter) {
        currentInterpreter.set(interpreter);
    }
    
    public static Interpreter getCurrentInterpreter() {
        return currentInterpreter.get();
    }
    
    public RuntimeObject(ScriptClass scriptClass) {
        this.scriptClass = scriptClass;
        this.fields = new HashMap<>();
        this.capturedVariables = new HashMap<>();
    }
    
    public ScriptClass getScriptClass() { return scriptClass; }
    
    public Object getField(String name) {
        return fields.get(name);
    }
    
    public void setField(String name, Object value) {
        fields.put(name, value);
    }
    
    public boolean hasField(String name) {
        return fields.containsKey(name);
    }
    
    public void setCapturedVariable(String name, Object value) {
        capturedVariables.put(name, value);
    }
    
    public Object getCapturedVariable(String name) {
        return capturedVariables.get(name);
    }
    
    public boolean hasCapturedVariable(String name) {
        return capturedVariables.containsKey(name);
    }
    
    public Map<String, Object> getCapturedVariables() {
        return capturedVariables;
    }
    
    @Override
    public String toString() {
        if (scriptClass instanceof ScriptEnum) {
            Object name = fields.get("name");
            if (name != null) {
                return name.toString();
            }
        }
        
        Interpreter interpreter = currentInterpreter.get();
        if (interpreter != null) {
            ScriptMethod toStringMethod = scriptClass.getMethod("toString", Collections.emptyList());
            if (toStringMethod != null) {
                try {
                    Object result = interpreter.invokeMethod(this, toStringMethod, Collections.emptyList());
                    if (result != null) {
                        return result.toString();
                    }
                    return "null";
                } catch (Exception e) {
                    return getClass().getName() + "@" + Integer.toHexString(hashCode());
                }
            }
        }
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }
}
