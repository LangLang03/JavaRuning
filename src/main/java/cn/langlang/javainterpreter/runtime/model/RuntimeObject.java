package cn.langlang.javainterpreter.runtime.model;

import java.util.*;

public class RuntimeObject {
    private final ScriptClass scriptClass;
    private final Map<String, Object> fields;
    private Map<String, Object> capturedVariables;
    
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
}
