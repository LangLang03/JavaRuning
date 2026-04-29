package cn.langlang.javanter.annotation;

import cn.langlang.javanter.ast.base.ASTNode;
import cn.langlang.javanter.ast.declaration.*;
import cn.langlang.javanter.ast.misc.Annotation;
import cn.langlang.javanter.ast.statement.LocalVariableDeclaration;
import cn.langlang.javanter.runtime.model.ScriptClass;
import java.util.*;

public class ProcessingContext {
    
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
    
    private final int elementKind;
    private final ASTNode currentNode;
    private final ASTNode parentNode;
    private final Annotation annotation;
    private final ScriptClass scriptClass;
    private final ProcessingEnvironment processingEnv;
    private final Map<String, Object> attributes;
    
    public ProcessingContext(int elementKind, ASTNode currentNode, ASTNode parentNode,
                            Annotation annotation, ScriptClass scriptClass,
                            ProcessingEnvironment processingEnv) {
        this.elementKind = elementKind;
        this.currentNode = currentNode;
        this.parentNode = parentNode;
        this.annotation = annotation;
        this.scriptClass = scriptClass;
        this.processingEnv = processingEnv;
        this.attributes = new HashMap<>();
    }
    
    public int getElementKind() {
        return elementKind;
    }
    
    public String getElementKindName() {
        switch (elementKind) {
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
    
    public ASTNode getCurrentNode() {
        return currentNode;
    }
    
    public ASTNode getParentNode() {
        return parentNode;
    }
    
    public Annotation getAnnotation() {
        return annotation;
    }
    
    public String getAnnotationName() {
        return annotation != null ? annotation.getTypeName() : null;
    }
    
    public ScriptClass getScriptClass() {
        return scriptClass;
    }
    
    public ProcessingEnvironment getProcessingEnvironment() {
        return processingEnv;
    }
    
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    public boolean isKind(int kind) {
        return elementKind == kind;
    }
    
    public boolean isTypeDeclaration() {
        return currentNode instanceof TypeDeclaration;
    }
    
    public boolean isClassDeclaration() {
        return currentNode instanceof ClassDeclaration;
    }
    
    public boolean isFieldDeclaration() {
        return currentNode instanceof FieldDeclaration;
    }
    
    public boolean isMethodDeclaration() {
        return currentNode instanceof MethodDeclaration;
    }
    
    public boolean isConstructorDeclaration() {
        return currentNode instanceof ConstructorDeclaration;
    }
    
    public boolean isVariableDeclarator() {
        return currentNode instanceof LocalVariableDeclaration;
    }
    
    public ClassDeclaration asClassDeclaration() {
        return currentNode instanceof ClassDeclaration ? (ClassDeclaration) currentNode : null;
    }
    
    public FieldDeclaration asFieldDeclaration() {
        return currentNode instanceof FieldDeclaration ? (FieldDeclaration) currentNode : null;
    }
    
    public MethodDeclaration asMethodDeclaration() {
        return currentNode instanceof MethodDeclaration ? (MethodDeclaration) currentNode : null;
    }
    
    public ConstructorDeclaration asConstructorDeclaration() {
        return currentNode instanceof ConstructorDeclaration ? (ConstructorDeclaration) currentNode : null;
    }
    
    public LocalVariableDeclaration asLocalVariableDeclaration() {
        return currentNode instanceof LocalVariableDeclaration ? (LocalVariableDeclaration) currentNode : null;
    }
    
    public List<FieldDeclaration> getFields() {
        if (currentNode instanceof ClassDeclaration) {
            return ((ClassDeclaration) currentNode).getFields();
        }
        return new ArrayList<>();
    }
    
    public List<MethodDeclaration> getMethods() {
        if (currentNode instanceof ClassDeclaration) {
            return ((ClassDeclaration) currentNode).getMethods();
        }
        return new ArrayList<>();
    }
    
    public List<ConstructorDeclaration> getConstructors() {
        if (currentNode instanceof ClassDeclaration) {
            return ((ClassDeclaration) currentNode).getConstructors();
        }
        return new ArrayList<>();
    }
    
    public List<Annotation> getAnnotations() {
        if (currentNode instanceof ClassDeclaration) {
            return ((ClassDeclaration) currentNode).getAnnotations();
        } else if (currentNode instanceof FieldDeclaration) {
            return ((FieldDeclaration) currentNode).getAnnotations();
        } else if (currentNode instanceof MethodDeclaration) {
            return ((MethodDeclaration) currentNode).getAnnotations();
        } else if (currentNode instanceof ConstructorDeclaration) {
            return ((ConstructorDeclaration) currentNode).getAnnotations();
        } else if (currentNode instanceof ParameterDeclaration) {
            return ((ParameterDeclaration) currentNode).getAnnotations();
        } else if (currentNode instanceof LocalVariableDeclaration) {
            return ((LocalVariableDeclaration) currentNode).getAnnotations();
        }
        return new ArrayList<>();
    }
}