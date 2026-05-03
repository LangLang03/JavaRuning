package cn.langlang.javanter.runtime.environment;

import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.parser.Modifier;
import cn.langlang.javanter.runtime.model.RuntimeObject;
import cn.langlang.javanter.runtime.model.ScriptClass;
import java.util.*;

/**
 * Represents a scoped environment for variables, classes, and execution context.
 *
 * <p>The Environment class implements a chained scope mechanism similar to
 * nested scopes in programming languages. Each environment can have a parent
 * environment, creating a scope chain that is traversed when looking up
 * variables or classes.</p>
 *
 * <p>Each Environment maintains:</p>
 * <ul>
 *   <li><b>variables</b> - Map of local variable names to their values</li>
 *   <li><b>classes</b> - Map of class names to ScriptClass definitions</li>
 *   <li><b>thisObject</b> - Reference to the current instance (for instance method execution)</li>
 *   <li><b>currentClass</b> - Reference to the class whose code is currently executing</li>
 * </ul>
 *
 * <p>Variable resolution order:</p>
 * <ol>
 *   <li>Check local variables in current scope</li>
 *   <li>Traverse up the parent chain to find the variable</li>
 *   <li>Return null if not found anywhere in the chain</li>
 * </ol>
 *
 * <p>Creating new scopes:</p>
 * <ul>
 *   <li>Use {@link #push()} to create a child environment for method calls</li>
 *   <li>Use {@link #pop()} to return to the parent environment</li>
 * </ul>
 *
 * @see Interpreter
 * @author Javanter Development Team
 */
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

    /**
     * Creates a new Environment with the given parent.
     * Used internally when pushing a new scope.
     *
     * @param parent The parent environment in the scope chain
     */
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

    /**
     * Sets an existing variable's value.
     * If the variable exists in a parent scope, updates it there (shadowing the local).
     * Otherwise, sets it in the current scope.
     *
     * @param name The variable name
     * @param value The new value
     */
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

    /**
     * Creates a new child environment for a new scope (e.g., method call).
     * The child inherits all parent bindings but can shadow them locally.
     *
     * @return A new Environment with this as its parent
     */
    public Environment push() {
        return new Environment(this);
    }

    /**
     * Returns the parent environment.
     * Should be called after push() to restore the previous scope.
     *
     * @return The parent environment, or null if this is the global environment
     */
    public Environment pop() {
        return parent;
    }
    
    public ScriptClass registerClass(String name) {
        return registerClass(name, Modifier.PUBLIC);
    }
    
    public ScriptClass registerClass(String name, int modifiers) {
        ScriptClass scriptClass = new ScriptClass(name, name, modifiers, null, new ArrayList<>(), null);
        scriptClass.setEnvironment(this);
        classes.put(name, scriptClass);
        return scriptClass;
    }
    
    public ScriptClass registerClass(String name, int modifiers, ScriptClass superClass) {
        ScriptClass scriptClass = new ScriptClass(name, name, modifiers, superClass, new ArrayList<>(), null);
        scriptClass.setEnvironment(this);
        classes.put(name, scriptClass);
        return scriptClass;
    }
    
    public ScriptClass registerClass(String name, int modifiers, ScriptClass superClass, List<ScriptClass> interfaces) {
        ScriptClass scriptClass = new ScriptClass(name, name, modifiers, superClass, interfaces, null);
        scriptClass.setEnvironment(this);
        classes.put(name, scriptClass);
        return scriptClass;
    }
    
    public ScriptClass registerRecord(String name) {
        return registerRecord(name, Modifier.PUBLIC);
    }
    
    public ScriptClass registerRecord(String name, int modifiers) {
        int recordModifiers = modifiers | Modifier.FINAL | Modifier.RECORD;
        ScriptClass recordClass = new ScriptClass(name, name, recordModifiers, null, new ArrayList<>(), null);
        recordClass.setEnvironment(this);
        classes.put(name, recordClass);
        return recordClass;
    }
    
    public ScriptClass registerSealedClass(String name, String... permittedSubtypes) {
        return registerSealedClass(name, Modifier.PUBLIC | Modifier.SEALED, permittedSubtypes);
    }
    
    public ScriptClass registerSealedClass(String name, int modifiers, String... permittedSubtypes) {
        int sealedModifiers = modifiers | Modifier.SEALED;
        ScriptClass sealedClass = new ScriptClass(name, name, sealedModifiers, null, new ArrayList<>(), null);
        sealedClass.setEnvironment(this);
        
        if (permittedSubtypes != null && permittedSubtypes.length > 0) {
            List<Type> permits = new ArrayList<>();
            for (String subtype : permittedSubtypes) {
                permits.add(new Type(null, subtype, null, 0, null));
            }
            sealedClass.setPermittedSubtypes(permits);
        }
        
        classes.put(name, sealedClass);
        return sealedClass;
    }
}
