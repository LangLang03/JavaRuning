package cn.langlang.javainterpreter.runtime.model;

import cn.langlang.javainterpreter.ast.declaration.TypeDeclaration;
import cn.langlang.javainterpreter.parser.Modifier;
import java.util.*;

public class ScriptEnum extends ScriptClass {
    private final List<EnumConstantInfo> constants;
    private final Map<String, RuntimeObject> constantMap;
    
    public ScriptEnum(String name, String qualifiedName, int modifiers,
                     List<ScriptClass> interfaces, TypeDeclaration astNode) {
        super(name, qualifiedName, modifiers | Modifier.ENUM, null, interfaces, astNode);
        this.constants = new ArrayList<>();
        this.constantMap = new LinkedHashMap<>();
    }
    
    public void addConstant(String name, RuntimeObject instance, int ordinal) {
        EnumConstantInfo info = new EnumConstantInfo(name, instance, ordinal);
        constants.add(info);
        constantMap.put(name, instance);
    }
    
    public List<EnumConstantInfo> getConstants() {
        return Collections.unmodifiableList(constants);
    }
    
    public List<String> getConstantNames() {
        List<String> names = new ArrayList<>();
        for (EnumConstantInfo info : constants) {
            names.add(info.getName());
        }
        return names;
    }
    
    public RuntimeObject getConstant(String name) {
        return constantMap.get(name);
    }
    
    public RuntimeObject[] values() {
        RuntimeObject[] result = new RuntimeObject[constants.size()];
        for (int i = 0; i < constants.size(); i++) {
            result[i] = constants.get(i).getInstance();
        }
        return result;
    }
    
    public RuntimeObject valueOf(String name) {
        RuntimeObject result = constantMap.get(name);
        if (result == null) {
            throw new IllegalArgumentException("No enum constant " + getName() + "." + name);
        }
        return result;
    }
    
    public boolean isEnum() {
        return true;
    }
    
    public static class EnumConstantInfo {
        private final String name;
        private final RuntimeObject instance;
        private final int ordinal;
        
        public EnumConstantInfo(String name, RuntimeObject instance, int ordinal) {
            this.name = name;
            this.instance = instance;
            this.ordinal = ordinal;
        }
        
        public String getName() { return name; }
        public RuntimeObject getInstance() { return instance; }
        public int getOrdinal() { return ordinal; }
    }
}
