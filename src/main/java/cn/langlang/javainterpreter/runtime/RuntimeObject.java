package cn.langlang.javainterpreter.runtime;

import cn.langlang.javainterpreter.ast.*;
import java.util.*;

public class RuntimeObject {
    private final ScriptClass scriptClass;
    private final Map<String, Object> fields;
    
    public RuntimeObject(ScriptClass scriptClass) {
        this.scriptClass = scriptClass;
        this.fields = new HashMap<>();
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
}
