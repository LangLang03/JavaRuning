package cn.langlang.javanter.runtime.model;

import cn.langlang.javanter.interpreter.Interpreter;
import java.util.*;

/**
 * Represents an instance of a class during interpretation.
 *
 * <p>RuntimeObject is the runtime representation of a Java object. It holds:</p>
 * <ul>
 *   <li><b>scriptClass</b> - The ScriptClass definition describing this object's structure</li>
 *   <li><b>fields</b> - A map of field names to their current values for this instance</li>
 *   <li><b>capturedVariables</b> - Variables captured from enclosing scopes for lambda/closure support</li>
 * </ul>
 *
 * <p>Thread-local interpreter reference:</p>
 * <p>The current interpreter is stored in a ThreadLocal to support
 * proper interpreter state management in multi-threaded scenarios.</p>
 *
 * <p>toString() behavior:</p>
 * <p>Returns the enum name for enum instances, or attempts to call
 * the class's toString() method. Falls back to the default
 * Object.toString() representation if no custom toString exists.</p>
 *
 * @see ScriptClass
 * @see Interpreter
 * @author Javanter Development Team
 */
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
