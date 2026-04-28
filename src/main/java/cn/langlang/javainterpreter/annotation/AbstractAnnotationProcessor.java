package cn.langlang.javainterpreter.annotation;

import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.runtime.*;
import java.util.*;

public abstract class AbstractAnnotationProcessor implements AnnotationProcessor {
    
    private final String name;
    private final String packageName;
    private final Set<String> supportedAnnotationTypes;
    private final Set<Integer> supportedElementTypes;
    
    protected AbstractAnnotationProcessor(String name, String packageName,
                                         Set<String> annotationTypes, Set<Integer> elementTypes) {
        this.name = name;
        this.packageName = packageName;
        this.supportedAnnotationTypes = new HashSet<>(annotationTypes);
        this.supportedElementTypes = new HashSet<>(elementTypes);
    }
    
    public String getName() {
        return name;
    }
    
    public String getPackage() {
        return packageName;
    }
    
    public String getFullName() {
        if (packageName == null || packageName.isEmpty()) {
            return name;
        }
        return packageName + "." + name;
    }
    
    public Set<String> getSupportedAnnotationTypes() {
        return supportedAnnotationTypes;
    }
    
    public Set<Integer> getSupportedElementTypes() {
        return supportedElementTypes;
    }
    
    public boolean supportsAnnotation(String annotationName) {
        if (annotationName == null) return false;
        for (String supported : supportedAnnotationTypes) {
            if (supported.equals(annotationName)) {
                return true;
            }
            if (packageName != null && !packageName.isEmpty()) {
                if ((packageName + "." + supported).equals(annotationName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean supportsElement(int elementKind) {
        if (supportedElementTypes.isEmpty()) {
            return true;
        }
        return supportedElementTypes.contains(elementKind);
    }
    
    public void onStart(ProcessingContext context) {
    }
    
    public void process(ProcessingContext context) {
    }
    
    public void onEnd(ProcessingContext context) {
    }
    
    public int getPriority() {
        return 0;
    }
    
    public ProcessingEnvironment getProcessingEnvironment(ProcessingContext context) {
        return context.getProcessingEnvironment();
    }

    @Override
    @Deprecated
    public void process(Set<? extends TypeDeclaration> annotations,
                        ProcessingEnvironment processingEnv) {
    }
}