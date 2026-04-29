package cn.langlang.javanter.annotation;

import cn.langlang.javanter.ast.declaration.TypeDeclaration;
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