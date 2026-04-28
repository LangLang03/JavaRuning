package cn.langlang.javainterpreter.annotation;

import cn.langlang.javainterpreter.interpreter.*;
import cn.langlang.javainterpreter.runtime.*;
import cn.langlang.javainterpreter.ast.*;
import java.util.*;

public class ProcessingEnvironment {
    private final Interpreter interpreter;
    private final Environment globalEnv;
    private final Map<String, ScriptClass> classes;
    private final List<AnnotationProcessor> processors;
    
    public ProcessingEnvironment(Interpreter interpreter, Environment globalEnv) {
        this.interpreter = interpreter;
        this.globalEnv = globalEnv;
        this.classes = new HashMap<>();
        this.processors = new ArrayList<>();
    }
    
    public Interpreter getInterpreter() {
        return interpreter;
    }
    
    public Environment getGlobalEnv() {
        return globalEnv;
    }
    
    public ScriptClass getClass(String name) {
        return globalEnv.getClass(name);
    }
    
    public void registerClass(String name, ScriptClass scriptClass) {
        globalEnv.defineClass(name, scriptClass);
        classes.put(name, scriptClass);
    }
    
    public void addProcessor(AnnotationProcessor processor) {
        processors.add(processor);
    }
    
    public List<AnnotationProcessor> getProcessors() {
        return processors;
    }
    
    public void addMethodToClass(String className, ScriptMethod method) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass != null) {
            scriptClass.addMethod(method);
        }
    }
    
    public void addFieldToClass(String className, ScriptField field) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass != null) {
            scriptClass.addField(field);
        }
    }
    
    public List<ScriptField> getFields(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass != null) {
            return new ArrayList<>(scriptClass.getFields().values());
        }
        return new ArrayList<>();
    }
    
    public List<ScriptMethod> getMethods(String className) {
        ScriptClass scriptClass = globalEnv.getClass(className);
        if (scriptClass != null) {
            List<ScriptMethod> allMethods = new ArrayList<>();
            for (List<ScriptMethod> methodList : scriptClass.getMethods().values()) {
                allMethods.addAll(methodList);
            }
            return allMethods;
        }
        return new ArrayList<>();
    }
    
    public static class Element {
        public static final int TYPE = 1;
        public static final int FIELD = 2;
        public static final int METHOD = 3;
        public static final int PARAMETER = 4;
        public static final int CONSTRUCTOR = 5;
        public static final int LOCAL_VARIABLE = 6;
        public static final int ANNOTATION_TYPE = 7;
        public static final int PACKAGE = 8;
        public static final int TYPE_PARAMETER = 9;
        public static final int TYPE_USE = 10;
        
        private final int kind;
        private final Object element;
        
        public Element(int kind, Object element) {
            this.kind = kind;
            this.element = element;
        }
        
        public int getKind() {
            return kind;
        }
        
        public Object getElement() {
            return element;
        }
        
        public String getKindName() {
            switch (kind) {
                case TYPE: return "TYPE";
                case FIELD: return "FIELD";
                case METHOD: return "METHOD";
                case PARAMETER: return "PARAMETER";
                case CONSTRUCTOR: return "CONSTRUCTOR";
                case LOCAL_VARIABLE: return "LOCAL_VARIABLE";
                case ANNOTATION_TYPE: return "ANNOTATION_TYPE";
                case PACKAGE: return "PACKAGE";
                case TYPE_PARAMETER: return "TYPE_PARAMETER";
                case TYPE_USE: return "TYPE_USE";
                default: return "UNKNOWN";
            }
        }
    }
}
