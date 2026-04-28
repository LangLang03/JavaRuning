package cn.langlang.javainterpreter.annotation;

import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.runtime.*;
import java.util.*;

public interface AnnotationProcessor {
    
    Set<String> getSupportedAnnotationTypes();
    
    void process(Set<? extends TypeDeclaration> annotations, 
                 ProcessingEnvironment processingEnv);
    
    default int getPriority() {
        return 0;
    }
}
