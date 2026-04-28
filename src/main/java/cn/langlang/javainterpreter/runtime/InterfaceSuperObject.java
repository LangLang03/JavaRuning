package cn.langlang.javainterpreter.runtime;

public class InterfaceSuperObject {
    private final RuntimeObject thisObject;
    private final ScriptClass interfaceClass;
    
    public InterfaceSuperObject(RuntimeObject thisObject, ScriptClass interfaceClass) {
        this.thisObject = thisObject;
        this.interfaceClass = interfaceClass;
    }
    
    public RuntimeObject getThisObject() { return thisObject; }
    public ScriptClass getInterfaceClass() { return interfaceClass; }
}
