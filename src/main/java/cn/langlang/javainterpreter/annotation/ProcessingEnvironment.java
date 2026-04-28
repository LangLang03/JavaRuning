package cn.langlang.javainterpreter.annotation;

import cn.langlang.javainterpreter.interpreter.*;
import cn.langlang.javainterpreter.runtime.*;
import cn.langlang.javainterpreter.ast.*;
import java.util.*;

public class ProcessingEnvironment {
    private final Interpreter interpreter;
    private final Environment globalEnv;
    private final Map<String, ScriptClass> classes;
    private final Map<String, AbstractAnnotationProcessor> processorRegistry;
    private final List<AbstractAnnotationProcessor> orderedProcessors;
    
    public ProcessingEnvironment(Interpreter interpreter, Environment globalEnv) {
        this.interpreter = interpreter;
        this.globalEnv = globalEnv;
        this.classes = new HashMap<>();
        this.processorRegistry = new HashMap<>();
        this.orderedProcessors = new ArrayList<>();
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
    
    public void registerProcessor(AbstractAnnotationProcessor processor) {
        String fullName = processor.getFullName();
        processorRegistry.put(fullName, processor);
        orderedProcessors.add(processor);
        orderedProcessors.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }
    
    public void unregisterProcessor(String name) {
        AbstractAnnotationProcessor processor = processorRegistry.remove(name);
        if (processor != null) {
            orderedProcessors.remove(processor);
        }
    }
    
    public void unregisterProcessor(AbstractAnnotationProcessor processor) {
        unregisterProcessor(processor.getFullName());
    }
    
    public AbstractAnnotationProcessor getProcessor(String name) {
        return processorRegistry.get(name);
    }
    
    public List<AbstractAnnotationProcessor> getProcessors() {
        return new ArrayList<>(orderedProcessors);
    }
    
    public List<AbstractAnnotationProcessor> getProcessorsForAnnotation(String annotationName) {
        List<AbstractAnnotationProcessor> result = new ArrayList<>();
        for (AbstractAnnotationProcessor processor : orderedProcessors) {
            if (processor.supportsAnnotation(annotationName)) {
                result.add(processor);
            }
        }
        return result;
    }
    
    public List<AbstractAnnotationProcessor> getProcessorsForElement(int elementKind) {
        List<AbstractAnnotationProcessor> result = new ArrayList<>();
        for (AbstractAnnotationProcessor processor : orderedProcessors) {
            if (processor.supportsElement(elementKind)) {
                result.add(processor);
            }
        }
        return result;
    }
    
    public List<AbstractAnnotationProcessor> getProcessorsForAnnotationAndElement(
            String annotationName, int elementKind) {
        List<AbstractAnnotationProcessor> result = new ArrayList<>();
        for (AbstractAnnotationProcessor processor : orderedProcessors) {
            if (processor.supportsAnnotation(annotationName) &&
                processor.supportsElement(elementKind)) {
                result.add(processor);
            }
        }
        return result;
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
    
    public void invokeProcessorsForClass(ClassDeclaration classDecl, ScriptClass scriptClass) {
        ProcessingContext context = new ProcessingContext(
            ProcessingContext.TYPE, classDecl, null, null, scriptClass, this);
        
        for (Annotation ann : classDecl.getAnnotations()) {
            List<AbstractAnnotationProcessor> processors =
                getProcessorsForAnnotationAndElement(ann.getTypeName(), ProcessingContext.TYPE);
            for (AbstractAnnotationProcessor processor : processors) {
                ProcessingContext annContext = new ProcessingContext(
                    ProcessingContext.TYPE, classDecl, null, ann, scriptClass, this);
                processor.onStart(annContext);
                processor.process(annContext);
                processor.onEnd(annContext);
            }
        }
        
        for (Annotation ann : classDecl.getAnnotations()) {
            List<AbstractAnnotationProcessor> processors =
                getProcessorsForAnnotationAndElement(ann.getTypeName(), ProcessingContext.TYPE);
            for (AbstractAnnotationProcessor processor : processors) {
                ProcessingContext annContext = new ProcessingContext(
                    ProcessingContext.TYPE, classDecl, null, ann, scriptClass, this);
                processor.onStart(annContext);
                processor.process(annContext);
                processor.onEnd(annContext);
            }
        }
    }
    
    public void invokeProcessorsForMethod(MethodDeclaration method, ScriptClass scriptClass) {
        ProcessingContext context = new ProcessingContext(
            ProcessingContext.METHOD, method, null, null, scriptClass, this);
        
        for (Annotation ann : method.getAnnotations()) {
            List<AbstractAnnotationProcessor> processors =
                getProcessorsForAnnotationAndElement(ann.getTypeName(), ProcessingContext.METHOD);
            for (AbstractAnnotationProcessor processor : processors) {
                ProcessingContext annContext = new ProcessingContext(
                    ProcessingContext.METHOD, method, null, ann, scriptClass, this);
                processor.onStart(annContext);
                processor.process(annContext);
                processor.onEnd(annContext);
            }
        }
    }
    
    public void invokeProcessorsForField(FieldDeclaration field, ScriptClass scriptClass) {
        ProcessingContext context = new ProcessingContext(
            ProcessingContext.FIELD, field, null, null, scriptClass, this);
        
        for (Annotation ann : field.getAnnotations()) {
            List<AbstractAnnotationProcessor> processors =
                getProcessorsForAnnotationAndElement(ann.getTypeName(), ProcessingContext.FIELD);
            for (AbstractAnnotationProcessor processor : processors) {
                ProcessingContext annContext = new ProcessingContext(
                    ProcessingContext.FIELD, field, null, ann, scriptClass, this);
                processor.onStart(annContext);
                processor.process(annContext);
                processor.onEnd(annContext);
            }
        }
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