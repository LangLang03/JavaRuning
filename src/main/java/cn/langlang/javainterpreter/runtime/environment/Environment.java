package cn.langlang.javainterpreter.runtime.environment;

import cn.langlang.javainterpreter.runtime.model.RuntimeObject;
import cn.langlang.javainterpreter.runtime.model.ScriptClass;
import java.util.*;

public class Environment {
    private final Environment parent;
    private final Map<String, Object> variables;
    private final Map<String, ScriptClass> classes;
    private RuntimeObject thisObject;
    private ScriptClass currentClass;
    private boolean thisObjectSet;
    
    public Environment() {
        this(null);
    }
    
    public Environment(Environment parent) {
        this.parent = parent;
        this.variables = new HashMap<>();
        this.classes = new HashMap<>();
        this.thisObjectSet = false;
    }
    
    public Object getVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.getVariable(name);
        }
        return null;
    }
    
    public void setVariable(String name, Object value) {
        if (variables.containsKey(name) || parent == null || !parent.hasVariable(name)) {
            variables.put(name, value);
        } else {
            parent.setVariable(name, value);
        }
    }
    
    public void defineVariable(String name, Object value) {
        variables.put(name, value);
    }
    
    public boolean hasVariable(String name) {
        if (variables.containsKey(name)) {
            return true;
        }
        if (parent != null) {
            return parent.hasVariable(name);
        }
        return false;
    }
    
    public ScriptClass getClass(String name) {
        if (classes.containsKey(name)) {
            return classes.get(name);
        }
        if (parent != null) {
            return parent.getClass(name);
        }
        return null;
    }
    
    public void defineClass(String name, ScriptClass scriptClass) {
        classes.put(name, scriptClass);
    }
    
    public boolean hasClass(String name) {
        if (classes.containsKey(name)) {
            return true;
        }
        if (parent != null) {
            return parent.hasClass(name);
        }
        return false;
    }
    
    public RuntimeObject getThisObject() {
        if (thisObjectSet) {
            return thisObject;
        }
        if (parent != null) {
            return parent.getThisObject();
        }
        return null;
    }
    
    public void setThisObject(RuntimeObject thisObject) {
        this.thisObject = thisObject;
        this.thisObjectSet = true;
    }
    
    public ScriptClass getCurrentClass() {
        if (currentClass != null) {
            return currentClass;
        }
        if (parent != null) {
            return parent.getCurrentClass();
        }
        return null;
    }
    
    public void setCurrentClass(ScriptClass currentClass) {
        this.currentClass = currentClass;
    }
    
    public Environment getParent() {
        return parent;
    }
    
    public Environment push() {
        return new Environment(this);
    }
    
    public Environment pop() {
        return parent;
    }
}
