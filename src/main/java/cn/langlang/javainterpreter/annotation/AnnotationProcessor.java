package cn.langlang.javainterpreter.annotation;

import cn.langlang.javainterpreter.ast.declaration.TypeDeclaration;
import java.util.*;

@Deprecated
public interface AnnotationProcessor {
    
    Set<String> getSupportedAnnotationTypes();
    
    void process(Set<? extends TypeDeclaration> annotations, 
                 ProcessingEnvironment processingEnv);
    
    default int getPriority() {
        return 0;
    }
}