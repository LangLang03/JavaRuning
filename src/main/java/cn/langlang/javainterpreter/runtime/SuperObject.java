package cn.langlang.javainterpreter.runtime;

public class SuperObject {
    private final RuntimeObject target;
    private final ScriptClass superClass;
    
    public SuperObject(RuntimeObject target, ScriptClass superClass) {
        this.target = target;
        this.superClass = superClass;
    }
    
    public RuntimeObject getTarget() { return target; }
    public ScriptClass getSuperClass() { return superClass; }
}
