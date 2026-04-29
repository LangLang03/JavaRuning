package cn.langlang.javainterpreter.runtime.model;

import cn.langlang.javainterpreter.ast.declaration.InitializerBlock;
import cn.langlang.javainterpreter.ast.declaration.ParameterDeclaration;
import cn.langlang.javainterpreter.ast.declaration.TypeDeclaration;
import cn.langlang.javainterpreter.ast.type.Type;
import java.util.*;

public class ScriptClass {
    private final String name;
    private final String qualifiedName;
    private final int modifiers;
    private final ScriptClass superClass;
    private final List<ScriptClass> interfaces;
    private final Map<String, ScriptField> fields;
    private final Map<String, List<ScriptMethod>> methods;
    private final List<ScriptMethod> constructors;
    private final List<InitializerBlock> staticInitializers;
    private final List<InitializerBlock> instanceInitializers;
    private final TypeDeclaration astNode;
    private boolean initialized;
    private ScriptClass enclosingClass;
    
    public ScriptClass(String name, String qualifiedName, int modifiers, 
                      ScriptClass superClass, List<ScriptClass> interfaces,
                      TypeDeclaration astNode) {
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.modifiers = modifiers;
        this.superClass = superClass;
        this.interfaces = interfaces != null ? interfaces : new ArrayList<>();
        this.fields = new HashMap<>();
        this.methods = new HashMap<>();
        this.constructors = new ArrayList<>();
        this.staticInitializers = new ArrayList<>();
        this.instanceInitializers = new ArrayList<>();
        this.astNode = astNode;
        this.initialized = false;
    }
    
    public String getName() { return name; }
    public String getQualifiedName() { return qualifiedName; }
    public int getModifiers() { return modifiers; }
    public ScriptClass getSuperClass() { return superClass; }
    public List<ScriptClass> getInterfaces() { return interfaces; }
    public Map<String, ScriptField> getFields() { return fields; }
    public Map<String, List<ScriptMethod>> getMethods() { return methods; }
    public List<ScriptMethod> getConstructors() { return constructors; }
    public List<InitializerBlock> getStaticInitializers() { return staticInitializers; }
    public List<InitializerBlock> getInstanceInitializers() { return instanceInitializers; }
    public TypeDeclaration getAstNode() { return astNode; }
    public boolean isInitialized() { return initialized; }
    public ScriptClass getEnclosingClass() { return enclosingClass; }
    
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
    
    public void setEnclosingClass(ScriptClass enclosingClass) {
        this.enclosingClass = enclosingClass;
    }
    
    public void addField(ScriptField field) {
        fields.put(field.getName(), field);
    }
    
    public void addMethod(ScriptMethod method) {
        methods.computeIfAbsent(method.getName(), k -> new ArrayList<>()).add(method);
    }
    
    public void addConstructor(ScriptMethod constructor) {
        constructors.add(constructor);
    }
    
    public void addStaticInitializer(InitializerBlock block) {
        staticInitializers.add(block);
    }
    
    public void addInstanceInitializer(InitializerBlock block) {
        instanceInitializers.add(block);
    }
    
    public ScriptField getField(String name) {
        ScriptField field = fields.get(name);
        if (field == null && superClass != null) {
            return superClass.getField(name);
        }
        return field;
    }
    
    public List<ScriptMethod> getMethods(String name) {
        List<ScriptMethod> result = new ArrayList<>();
        List<ScriptMethod> localMethods = methods.get(name);
        if (localMethods != null) {
            result.addAll(localMethods);
        }
        if (superClass != null) {
            List<ScriptMethod> superMethods = superClass.getMethods(name);
            for (ScriptMethod superMethod : superMethods) {
                if (!isMethodOverridden(superMethod, localMethods)) {
                    result.add(superMethod);
                }
            }
        }
        return result;
    }
    
    private boolean isMethodOverridden(ScriptMethod superMethod, List<ScriptMethod> localMethods) {
        if (localMethods == null) {
            return false;
        }
        for (ScriptMethod localMethod : localMethods) {
            if (localMethod.getName().equals(superMethod.getName()) &&
                localMethod.getParameters().size() == superMethod.getParameters().size() &&
                localMethod.isVarArgs() == superMethod.isVarArgs()) {
                return true;
            }
        }
        return false;
    }
    
    public ScriptMethod getMethod(String name, List<Object> args) {
        List<ScriptMethod> candidates = getMethods(name);
        ScriptMethod bestMatch = null;
        int bestScore = -1;
        
        for (ScriptMethod method : candidates) {
            int score = computeMatchScore(method, args);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = method;
            }
        }
        
        return bestMatch;
    }
    
    private int computeMatchScore(ScriptMethod method, List<Object> args) {
        List<ParameterDeclaration> params = method.getParameters();
        if (params.size() != args.size() && !method.isVarArgs()) {
            return -1;
        }
        
        int score = 0;
        for (int i = 0; i < args.size(); i++) {
            Object arg = args.get(i);
            Type paramType = i < params.size() ? params.get(i).getType() : params.get(params.size() - 1).getType();
            
            if (isTypeCompatible(arg, paramType)) {
                score++;
            } else {
                return -1;
            }
        }
        
        return score;
    }
    
    private boolean isTypeCompatible(Object value, Type type) {
        if (value == null) {
            return !isPrimitiveType(type.getName());
        }
        
        String typeName = type.getName();
        
        if (isPrimitiveType(typeName)) {
            return isPrimitiveCompatible(value, typeName);
        }
        
        return true;
    }
    
    private boolean isPrimitiveType(String typeName) {
        return typeName.equals("int") || typeName.equals("long") || 
               typeName.equals("short") || typeName.equals("byte") ||
               typeName.equals("char") || typeName.equals("boolean") ||
               typeName.equals("float") || typeName.equals("double");
    }
    
    private boolean isPrimitiveCompatible(Object value, String typeName) {
        if (typeName.equals("int")) return value instanceof Integer;
        if (typeName.equals("long")) return value instanceof Long || value instanceof Integer;
        if (typeName.equals("short")) return value instanceof Short || value instanceof Integer;
        if (typeName.equals("byte")) return value instanceof Byte || value instanceof Integer;
        if (typeName.equals("char")) return value instanceof Character;
        if (typeName.equals("boolean")) return value instanceof Boolean;
        if (typeName.equals("float")) return value instanceof Float || value instanceof Double || value instanceof Integer || value instanceof Long;
        if (typeName.equals("double")) return value instanceof Double || value instanceof Float || value instanceof Integer || value instanceof Long;
        return false;
    }
    
    public boolean isAssignableFrom(ScriptClass other) {
        if (this == other) return true;
        if (other == null) return false;
        
        Set<ScriptClass> visited = new HashSet<>();
        return isAssignableFromHelper(other, visited);
    }
    
    private boolean isAssignableFromHelper(ScriptClass other, Set<ScriptClass> visited) {
        if (this == other) return true;
        if (other == null || visited.contains(other)) return false;
        
        visited.add(other);
        
        if (other.superClass != null) {
            if (isAssignableFromHelper(other.superClass, visited)) {
                return true;
            }
        }
        
        for (ScriptClass iface : other.interfaces) {
            if (isAssignableFromHelper(iface, visited)) {
                return true;
            }
            
            if (iface.superClass != null) {
                if (isAssignableFromHelper(iface.superClass, visited)) {
                    return true;
                }
            }
            
            for (ScriptClass extendedIface : iface.interfaces) {
                if (isAssignableFromHelper(extendedIface, visited)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public boolean isInstance(Object obj) {
        if (obj == null) return false;
        
        if (obj instanceof RuntimeObject) {
            RuntimeObject runtimeObj = (RuntimeObject) obj;
            return isAssignableFrom(runtimeObj.getScriptClass());
        }
        
        String typeName = this.name;
        if (typeName.equals("Object") || typeName.equals("java.lang.Object")) {
            return true;
        }
        
        Class<?> objClass = obj.getClass();
        if (typeName.equals("String") || typeName.equals("java.lang.String")) {
            return objClass == String.class;
        }
        if (typeName.equals("Integer") || typeName.equals("java.lang.Integer")) {
            return objClass == Integer.class;
        }
        if (typeName.equals("Long") || typeName.equals("java.lang.Long")) {
            return objClass == Long.class;
        }
        if (typeName.equals("Double") || typeName.equals("java.lang.Double")) {
            return objClass == Double.class;
        }
        if (typeName.equals("Boolean") || typeName.equals("java.lang.Boolean")) {
            return objClass == Boolean.class;
        }
        
        return false;
    }
}
