package cn.langlang.javanter.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScriptResult {
    private final Object result;
    private final Map<String, Object> variables;
    private final boolean success;
    private final Exception error;

    public ScriptResult(Object result, Map<String, Object> variables, boolean success, Exception error) {
        this.result = result;
        this.variables = variables != null ? new HashMap<>(variables) : new HashMap<>();
        this.success = success;
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public Object get(String name) {
        return variables.get(name);
    }

    public boolean isSuccess() {
        return success;
    }

    public Exception getError() {
        return error;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAs(String name, Class<T> type) {
        Object value = variables.get(name);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException("Cannot cast " + value.getClass().getName() + " to " + type.getName());
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    public static ScriptResult success(Object result, Map<String, Object> variables) {
        return new ScriptResult(result, variables, true, null);
    }

    public static ScriptResult failure(Exception error, Map<String, Object> variables) {
        return new ScriptResult(null, variables, false, error);
    }

    public static ScriptResult of(Object result) {
        return new ScriptResult(result, new HashMap<>(), true, null);
    }
}
