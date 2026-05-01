package cn.langlang.javanter.interpreter;

import cn.langlang.javanter.parser.Modifier;
import cn.langlang.javanter.runtime.environment.Environment;
import cn.langlang.javanter.runtime.model.ScriptClass;
import cn.langlang.javanter.runtime.model.ScriptField;
import cn.langlang.javanter.runtime.model.ScriptMethod;
import cn.langlang.javanter.runtime.model.RuntimeObject;

public final class AccessController {
    private AccessController() {}
    
    public static void checkMethodAccess(ScriptMethod method, ScriptClass targetClass, 
                                          Environment currentEnv) {
        int modifiers = method.getModifiers();
        ScriptClass currentClass = currentEnv.getCurrentClass();
        
        if (isPublic(modifiers)) {
            return;
        }
        
        if (isPrivate(modifiers)) {
            if (currentClass == targetClass) {
                return;
            }
            throw new RuntimeException("Cannot access private method '" + method.getName() + 
                "' from class '" + (currentClass != null ? currentClass.getName() : "<unknown>") + "'");
        }
        
        if (isProtected(modifiers)) {
            if (currentClass == targetClass) {
                return;
            }
            if (currentClass != null && isSubclassOf(currentClass, targetClass)) {
                return;
            }
            if (isSamePackage(currentClass, targetClass)) {
                return;
            }
            throw new RuntimeException("Cannot access protected method '" + method.getName() + 
                "' from class '" + (currentClass != null ? currentClass.getName() : "<unknown>") + "'");
        }
        
        if (isPackagePrivate(modifiers)) {
            if (isSamePackage(currentClass, targetClass)) {
                return;
            }
            throw new RuntimeException("Cannot access package-private method '" + method.getName() + 
                "' from class '" + (currentClass != null ? currentClass.getName() : "<unknown>") + "'");
        }
    }
    
    public static void checkFieldAccess(ScriptField field, ScriptClass targetClass,
                                         Environment currentEnv) {
        int modifiers = field.getModifiers();
        ScriptClass currentClass = currentEnv.getCurrentClass();
        
        if (isPublic(modifiers)) {
            return;
        }
        
        if (isPrivate(modifiers)) {
            if (currentClass == targetClass) {
                return;
            }
            throw new RuntimeException("Cannot access private field '" + field.getName() + 
                "' from class '" + (currentClass != null ? currentClass.getName() : "<unknown>") + "'");
        }
        
        if (isProtected(modifiers)) {
            if (currentClass == targetClass) {
                return;
            }
            if (currentClass != null && isSubclassOf(currentClass, targetClass)) {
                return;
            }
            if (isSamePackage(currentClass, targetClass)) {
                return;
            }
            throw new RuntimeException("Cannot access protected field '" + field.getName() + 
                "' from class '" + (currentClass != null ? currentClass.getName() : "<unknown>") + "'");
        }
        
        if (isPackagePrivate(modifiers)) {
            if (isSamePackage(currentClass, targetClass)) {
                return;
            }
            throw new RuntimeException("Cannot access package-private field '" + field.getName() + 
                "' from class '" + (currentClass != null ? currentClass.getName() : "<unknown>") + "'");
        }
    }
    
    public static void checkClassAccess(ScriptClass targetClass, Environment currentEnv) {
        int modifiers = targetClass.getModifiers();
        ScriptClass currentClass = currentEnv.getCurrentClass();
        
        if (isPublic(modifiers)) {
            return;
        }
        
        if (isPackagePrivate(modifiers)) {
            if (isSamePackage(currentClass, targetClass)) {
                return;
            }
            throw new RuntimeException("Cannot access package-private class '" + targetClass.getName() + 
                "' from class '" + (currentClass != null ? currentClass.getName() : "<unknown>") + "'");
        }
    }
    
    public static void checkConstructorAccess(ScriptMethod constructor, ScriptClass targetClass,
                                               Environment currentEnv) {
        checkMethodAccess(constructor, targetClass, currentEnv);
    }
    
    private static boolean isPublic(int modifiers) {
        return (modifiers & Modifier.PUBLIC) != 0;
    }
    
    private static boolean isPrivate(int modifiers) {
        return (modifiers & Modifier.PRIVATE) != 0;
    }
    
    private static boolean isProtected(int modifiers) {
        return (modifiers & Modifier.PROTECTED) != 0;
    }
    
    private static boolean isPackagePrivate(int modifiers) {
        return !isPublic(modifiers) && !isPrivate(modifiers) && !isProtected(modifiers);
    }
    
    private static boolean isSubclassOf(ScriptClass subclass, ScriptClass superclass) {
        ScriptClass current = subclass;
        while (current != null) {
            if (current == superclass) {
                return true;
            }
            current = current.getSuperClass();
        }
        return false;
    }
    
    private static boolean isSamePackage(ScriptClass class1, ScriptClass class2) {
        if (class1 == null || class2 == null) {
            return false;
        }
        
        String package1 = getPackageName(class1.getQualifiedName());
        String package2 = getPackageName(class2.getQualifiedName());
        
        return package1 != null && package1.equals(package2);
    }
    
    private static String getPackageName(String qualifiedName) {
        if (qualifiedName == null) return null;
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot > 0) {
            return qualifiedName.substring(0, lastDot);
        }
        return "";
    }
    
    public static boolean isAccessibleFrom(ScriptMethod method, ScriptClass targetClass, 
                                            ScriptClass fromClass) {
        int modifiers = method.getModifiers();
        
        if (isPublic(modifiers)) return true;
        
        if (isPrivate(modifiers)) {
            return fromClass == targetClass;
        }
        
        if (isProtected(modifiers)) {
            if (fromClass == targetClass) return true;
            if (isSubclassOf(fromClass, targetClass)) return true;
            return isSamePackage(fromClass, targetClass);
        }
        
        return isSamePackage(fromClass, targetClass);
    }
    
    public static boolean isAccessibleFrom(ScriptField field, ScriptClass targetClass,
                                            ScriptClass fromClass) {
        int modifiers = field.getModifiers();
        
        if (isPublic(modifiers)) return true;
        
        if (isPrivate(modifiers)) {
            return fromClass == targetClass;
        }
        
        if (isProtected(modifiers)) {
            if (fromClass == targetClass) return true;
            if (isSubclassOf(fromClass, targetClass)) return true;
            return isSamePackage(fromClass, targetClass);
        }
        
        return isSamePackage(fromClass, targetClass);
    }
}
