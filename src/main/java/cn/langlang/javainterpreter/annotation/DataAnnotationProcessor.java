package cn.langlang.javainterpreter.annotation;

import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.parser.Modifier;
import cn.langlang.javainterpreter.runtime.*;
import java.util.*;

public class DataAnnotationProcessor extends AbstractAnnotationProcessor {
    
    private static final Set<String> LOMBOK_ANNOTATIONS = new HashSet<>(Arrays.asList(
        "Data", "Getter", "Setter", "ToString",
        "EqualsAndHashCode", "NoArgsConstructor", "AllArgsConstructor"
    ));
    
    private static final Set<Integer> TYPE_ELEMENT = new HashSet<>(Collections.singleton(
        ProcessingContext.TYPE
    ));
    
    public DataAnnotationProcessor() {
        super("DataAnnotationProcessor", "lombok", LOMBOK_ANNOTATIONS, TYPE_ELEMENT);
    }
    
    @Override
    public void process(ProcessingContext context) {
        ClassDeclaration classDecl = context.asClassDeclaration();
        if (classDecl == null) {
            return;
        }
        
        ScriptClass scriptClass = context.getScriptClass();
        String annotationName = context.getAnnotationName();
        
        if (annotationName == null) {
            return;
        }
        
        String simpleName = annotationName;
        int lastDot = annotationName.lastIndexOf('.');
        if (lastDot >= 0) {
            simpleName = annotationName.substring(lastDot + 1);
        }
        
        boolean isData = simpleName.equals("Data");
        boolean isGetter = simpleName.equals("Getter");
        boolean isSetter = simpleName.equals("Setter");
        boolean isToString = simpleName.equals("ToString");
        boolean isEqualsAndHashCode = simpleName.equals("EqualsAndHashCode");
        boolean isNoArgsConstructor = simpleName.equals("NoArgsConstructor");
        boolean isAllArgsConstructor = simpleName.equals("AllArgsConstructor");
        
        if (isData) {
            isGetter = true;
            isSetter = true;
            isToString = true;
            isEqualsAndHashCode = true;
        }
        
        String className = classDecl.getName();
        
        for (FieldDeclaration field : classDecl.getFields()) {
            if ((field.getModifiers() & Modifier.STATIC) != 0) {
                continue;
            }
            
            String fieldName = field.getName();
            Type fieldType = field.getType();
            
            if (isGetter) {
                ScriptMethod getter = createGetterMethod(fieldName, fieldType, scriptClass);
                scriptClass.addMethod(getter);
            }
            
            if (isSetter) {
                ScriptMethod setter = createSetterMethod(fieldName, fieldType, scriptClass);
                scriptClass.addMethod(setter);
            }
        }
        
        if (isToString) {
            ScriptMethod toStringMethod = createToStringMethod(classDecl, scriptClass);
            scriptClass.addMethod(toStringMethod);
        }
        
        if (isEqualsAndHashCode) {
            ScriptMethod equalsMethod = createEqualsMethod(classDecl, scriptClass);
            scriptClass.addMethod(equalsMethod);
            
            ScriptMethod hashCodeMethod = createHashCodeMethod(classDecl, scriptClass);
            scriptClass.addMethod(hashCodeMethod);
        }
        
        if (isNoArgsConstructor) {
            ScriptMethod noArgsConstructor = createNoArgsConstructor(className, scriptClass);
            scriptClass.addConstructor(noArgsConstructor);
        }
        
        if (isAllArgsConstructor) {
            ScriptMethod allArgsConstructor = createAllArgsConstructor(classDecl, className, scriptClass);
            scriptClass.addConstructor(allArgsConstructor);
        }
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    private ScriptMethod createGetterMethod(String fieldName, Type fieldType, ScriptClass declaringClass) {
        String methodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        if (fieldType.getName().equals("boolean")) {
            methodName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        }
        
        ScriptMethod method = new ScriptMethod(methodName, Modifier.PUBLIC, fieldType,
            new ArrayList<>(), false, null, declaringClass, false, false);
        method.setNativeImplementation(args -> {
            RuntimeObject obj = (RuntimeObject) args[0];
            return obj.getField(fieldName);
        });
        return method;
    }
    
    private ScriptMethod createSetterMethod(String fieldName, Type fieldType, ScriptClass declaringClass) {
        String methodName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        
        List<ParameterDeclaration> params = new ArrayList<>();
        params.add(new ParameterDeclaration(null, 0, fieldType, fieldName, false, new ArrayList<>()));
        
        ScriptMethod method = new ScriptMethod(methodName, Modifier.PUBLIC, 
            new Type(null, "void", new ArrayList<>(), 0, new ArrayList<>()),
            params, false, null, declaringClass, false, false);
        method.setNativeImplementation(args -> {
            RuntimeObject obj = (RuntimeObject) args[0];
            Object value = args[1];
            obj.setField(fieldName, value);
            return null;
        });
        return method;
    }
    
    private ScriptMethod createToStringMethod(ClassDeclaration classDecl, ScriptClass declaringClass) {
        ScriptMethod method = new ScriptMethod("toString", Modifier.PUBLIC,
            new Type(null, "String", new ArrayList<>(), 0, new ArrayList<>()),
            new ArrayList<>(), false, null, declaringClass, false, false);
        method.setNativeImplementation(args -> {
            RuntimeObject obj = (RuntimeObject) args[0];
            StringBuilder sb = new StringBuilder(classDecl.getName() + "(");
            boolean first = true;
            for (FieldDeclaration field : classDecl.getFields()) {
                if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
                if (!first) sb.append(", ");
                first = false;
                sb.append(field.getName()).append("=").append(obj.getField(field.getName()));
            }
            sb.append(")");
            return sb.toString();
        });
        return method;
    }
    
    private ScriptMethod createEqualsMethod(ClassDeclaration classDecl, ScriptClass declaringClass) {
        List<ParameterDeclaration> params = new ArrayList<>();
        params.add(new ParameterDeclaration(null, 0, 
            new Type(null, "Object", new ArrayList<>(), 0, new ArrayList<>()),
            "obj", false, new ArrayList<>()));
        
        ScriptMethod method = new ScriptMethod("equals", Modifier.PUBLIC,
            new Type(null, "boolean", new ArrayList<>(), 0, new ArrayList<>()),
            params, false, null, declaringClass, false, false);
        method.setNativeImplementation(args -> {
            RuntimeObject self = (RuntimeObject) args[0];
            Object other = args[1];
            if (!(other instanceof RuntimeObject)) return false;
            RuntimeObject otherObj = (RuntimeObject) other;
            if (self.getScriptClass() != otherObj.getScriptClass()) return false;
            for (FieldDeclaration field : classDecl.getFields()) {
                if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
                Object thisVal = self.getField(field.getName());
                Object otherVal = otherObj.getField(field.getName());
                if (thisVal == null && otherVal != null) return false;
                if (thisVal != null && !thisVal.equals(otherVal)) return false;
            }
            return true;
        });
        return method;
    }
    
    private ScriptMethod createHashCodeMethod(ClassDeclaration classDecl, ScriptClass declaringClass) {
        ScriptMethod method = new ScriptMethod("hashCode", Modifier.PUBLIC,
            new Type(null, "int", new ArrayList<>(), 0, new ArrayList<>()),
            new ArrayList<>(), false, null, declaringClass, false, false);
        method.setNativeImplementation(args -> {
            RuntimeObject obj = (RuntimeObject) args[0];
            int hash = 1;
            for (FieldDeclaration field : classDecl.getFields()) {
                if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
                Object val = obj.getField(field.getName());
                hash = 31 * hash + (val != null ? val.hashCode() : 0);
            }
            return hash;
        });
        return method;
    }
    
    private ScriptMethod createNoArgsConstructor(String className, ScriptClass declaringClass) {
        ScriptMethod method = new ScriptMethod(className, Modifier.PUBLIC,
            new Type(null, className, new ArrayList<>(), 0, new ArrayList<>()),
            new ArrayList<>(), false, null, declaringClass, true, false);
        method.setNativeImplementation(args -> null);
        return method;
    }
    
    private ScriptMethod createAllArgsConstructor(ClassDeclaration classDecl, String className, ScriptClass declaringClass) {
        List<ParameterDeclaration> params = new ArrayList<>();
        for (FieldDeclaration field : classDecl.getFields()) {
            if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
            params.add(new ParameterDeclaration(null, 0, field.getType(), 
                field.getName(), false, new ArrayList<>()));
        }
        
        ScriptMethod method = new ScriptMethod(className, Modifier.PUBLIC,
            new Type(null, className, new ArrayList<>(), 0, new ArrayList<>()),
            params, false, null, declaringClass, true, false);
        method.setNativeImplementation(args -> {
            RuntimeObject obj = (RuntimeObject) args[0];
            int paramIndex = 0;
            for (int i = 1; i < args.length && paramIndex < params.size(); i++) {
                FieldDeclaration field = classDecl.getFields().get(paramIndex);
                while ((field.getModifiers() & Modifier.STATIC) != 0 && paramIndex < classDecl.getFields().size() - 1) {
                    paramIndex++;
                    field = classDecl.getFields().get(paramIndex);
                }
                obj.setField(field.getName(), args[i]);
                paramIndex++;
            }
            return null;
        });
        return method;
    }
}