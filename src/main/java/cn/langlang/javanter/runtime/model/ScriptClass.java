package cn.langlang.javanter.runtime.model;

import cn.langlang.javanter.ast.declaration.InitializerBlock;
import cn.langlang.javanter.ast.declaration.ParameterDeclaration;
import cn.langlang.javanter.ast.declaration.TypeDeclaration;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.type.TypeParameter;
import cn.langlang.javanter.parser.Modifier;
import cn.langlang.javanter.runtime.TypeConstants;
import cn.langlang.javanter.runtime.environment.Environment;
import cn.langlang.javanter.runtime.generics.*;
import cn.langlang.javanter.runtime.nativesupport.NativeMethod;
import java.util.*;
import java.util.function.Function;

/**
 * Represents a class definition during interpretation.
 *
 * <p>ScriptClass is the runtime representation of a Java class. It stores:</p>
 * <ul>
 *   <li><b>name / qualifiedName</b> - Simple and fully qualified class names</li>
 *   <li><b>modifiers</b> - Access modifiers and other flags (public, final, abstract, etc.)</li>
 *   <li><b>superClass / interfaces</b> - Inheritance hierarchy</li>
 *   <li><b>fields / methods / constructors</b> - Class members</li>
 *   <li><b>initializers</b> - Static and instance initializer blocks</li>
 *   <li><b>genericInfo / typeBindings</b> - Generic type parameters and their bindings</li>
 * </ul>
 *
 * <p>Method resolution:</p>
 * <p>Methods are resolved using a scoring system that considers:</p>
 * <ul>
 *   <li>Exact type matches (highest score)</li>
 *   <li>Assignment compatibility through inheritance</li>
 *   <li>Boxing/unboxing compatibility for primitives and wrappers</li>
 *   <li>Widening conversions for numeric types</li>
 * </ul>
 *
 * <p>Type parameter binding:</p>
 * <p>When a generic class is instantiated with type arguments,
 * the type parameters are bound to concrete types for type checking.</p>
 *
 * @see RuntimeObject
 * @see ScriptMethod
 * @see ScriptField
 * @author Javanter Development Team
 */
public class ScriptClass {
    private static final int MATCH_SCORE_EXACT = 3;
    private static final int MATCH_SCORE_WIDENING = 2;
    private static final int MATCH_SCORE_COMPATIBLE = 1;
    private static final int MATCH_SCORE_NONE = -1;
    
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
    
    private List<TypeParameter> typeParameters;
    private GenericClassInfo genericInfo;
    private Map<String, GenericType> typeBindings;
    private List<Type> permittedSubtypes;
    private List<ScriptClass> nestedTypes;
    private Map<String, Object> staticFieldValues;
    private Environment environment;
    
    public ScriptClass(String name, String qualifiedName, int modifiers, 
                      ScriptClass superClass, List<ScriptClass> interfaces,
                      TypeDeclaration astNode) {
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.modifiers = modifiers;
        this.superClass = superClass;
        this.interfaces = interfaces != null ? interfaces : new ArrayList<>();
        this.fields = new LinkedHashMap<>();
        this.methods = new LinkedHashMap<>();
        this.constructors = new ArrayList<>();
        this.staticInitializers = new ArrayList<>();
        this.instanceInitializers = new ArrayList<>();
        this.astNode = astNode;
        this.initialized = false;
        this.typeParameters = new ArrayList<>();
        this.typeBindings = new LinkedHashMap<>();
        this.permittedSubtypes = new ArrayList<>();
        this.nestedTypes = new ArrayList<>();
        this.staticFieldValues = new LinkedHashMap<>();
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
    
    public List<TypeParameter> getTypeParameters() { return typeParameters; }
    public GenericClassInfo getGenericInfo() { return genericInfo; }
    public Map<String, GenericType> getTypeBindings() { return typeBindings; }
    public List<Type> getPermittedSubtypes() { return permittedSubtypes; }
    public void setPermittedSubtypes(List<Type> permittedSubtypes) { this.permittedSubtypes = permittedSubtypes != null ? permittedSubtypes : new ArrayList<>(); }
    public List<ScriptClass> getNestedTypes() { return Collections.unmodifiableList(nestedTypes); }
    public Map<String, Object> getStaticFieldValues() { return staticFieldValues; }
    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }
    
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
    
    public void setEnclosingClass(ScriptClass enclosingClass) {
        this.enclosingClass = enclosingClass;
    }
    
    public void setTypeParameters(List<TypeParameter> typeParameters) {
        this.typeParameters = typeParameters != null ? new ArrayList<>(typeParameters) : new ArrayList<>();
    }
    
    public void setGenericInfo(GenericClassInfo genericInfo) {
        this.genericInfo = genericInfo;
    }
    
    public void bindTypeParameter(String name, GenericType type) {
        typeBindings.put(name, type);
        if (genericInfo != null) {
            genericInfo.bindTypeVariable(name, type);
        }
    }
    
    public GenericType getTypeBinding(String name) {
        return typeBindings.get(name);
    }
    
    public boolean isGenericClass() {
        return typeParameters != null && !typeParameters.isEmpty();
    }
    
    public ScriptClass withTypeBindings(Map<String, GenericType> bindings) {
        ScriptClass copy = new ScriptClass(name, qualifiedName, modifiers, superClass, interfaces, astNode);
        copy.fields.putAll(this.fields);
        copy.methods.putAll(this.methods);
        copy.constructors.addAll(this.constructors);
        copy.staticInitializers.addAll(this.staticInitializers);
        copy.instanceInitializers.addAll(this.instanceInitializers);
        copy.initialized = this.initialized;
        copy.enclosingClass = this.enclosingClass;
        copy.typeParameters = this.typeParameters;
        copy.genericInfo = this.genericInfo;
        copy.typeBindings.putAll(this.typeBindings);
        copy.typeBindings.putAll(bindings);
        copy.staticFieldValues.putAll(this.staticFieldValues);
        copy.environment = this.environment;
        return copy;
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
    
    public ScriptClass createInnerClass(String name, int modifiers) {
        String qualifiedName = this.qualifiedName + "$" + name;
        ScriptClass innerClass = new ScriptClass(name, qualifiedName, modifiers, null, new ArrayList<>(), null);
        innerClass.setEnclosingClass(this);
        nestedTypes.add(innerClass);
        return innerClass;
    }
    
    public ScriptClass createInnerClass(String name, int modifiers, ScriptClass superClass) {
        String qualifiedName = this.qualifiedName + "$" + name;
        ScriptClass innerClass = new ScriptClass(name, qualifiedName, modifiers, superClass, new ArrayList<>(), null);
        innerClass.setEnclosingClass(this);
        nestedTypes.add(innerClass);
        return innerClass;
    }
    
    public ScriptClass createInnerClass(String name, int modifiers, ScriptClass superClass, List<ScriptClass> interfaces) {
        String qualifiedName = this.qualifiedName + "$" + name;
        ScriptClass innerClass = new ScriptClass(name, qualifiedName, modifiers, superClass, interfaces, null);
        innerClass.setEnclosingClass(this);
        nestedTypes.add(innerClass);
        return innerClass;
    }
    
    public ScriptClass registerInnerClass(String name, int modifiers) {
        if (environment == null) {
            throw new IllegalStateException("Environment not set. Use registerClass() to create classes with environment.");
        }
        ScriptClass innerClass = this.createInnerClass(name, modifiers);
        innerClass.setEnvironment(environment);
        environment.defineClass(name, innerClass);
        environment.defineClass(innerClass.getQualifiedName(), innerClass);
        return innerClass;
    }
    
    public ScriptClass registerInnerClass(String name, int modifiers, ScriptClass superClass) {
        if (environment == null) {
            throw new IllegalStateException("Environment not set. Use registerClass() to create classes with environment.");
        }
        ScriptClass innerClass = this.createInnerClass(name, modifiers, superClass);
        innerClass.setEnvironment(environment);
        environment.defineClass(name, innerClass);
        environment.defineClass(innerClass.getQualifiedName(), innerClass);
        return innerClass;
    }
    
    public ScriptClass registerInnerClass(String name, int modifiers, ScriptClass superClass, List<ScriptClass> interfaces) {
        if (environment == null) {
            throw new IllegalStateException("Environment not set. Use registerClass() to create classes with environment.");
        }
        ScriptClass innerClass = this.createInnerClass(name, modifiers, superClass, interfaces);
        innerClass.setEnvironment(environment);
        environment.defineClass(name, innerClass);
        environment.defineClass(innerClass.getQualifiedName(), innerClass);
        return innerClass;
    }
    
    public ScriptClass getNestedType(String name) {
        for (ScriptClass nested : nestedTypes) {
            if (nested.getName().equals(name)) {
                return nested;
            }
        }
        return null;
    }
    
    public Collection<ScriptField> getAllFields() { return fields.values(); }
    
    public Collection<List<ScriptMethod>> getAllMethodGroups() { return methods.values(); }
    
    public boolean isSealed() { return (modifiers & cn.langlang.javanter.parser.Modifier.SEALED) != 0; }
    
    public boolean isNonSealed() { return (modifiers & cn.langlang.javanter.parser.Modifier.NON_SEALED) != 0; }
    
    public boolean isRecord() { return (modifiers & cn.langlang.javanter.parser.Modifier.RECORD) != 0; }
    
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
            
            int typeScore = computeTypeMatchScore(arg, paramType);
            if (typeScore < 0) {
                return -1;
            }
            score += typeScore;
        }
        
        return score;
    }
    
    private int computeTypeMatchScore(Object value, Type type) {
        if (value == null) {
            return isPrimitiveType(type.getName()) ? MATCH_SCORE_NONE : MATCH_SCORE_COMPATIBLE;
        }
        
        String typeName = type.getName();
        
        if (isPrimitiveType(typeName)) {
            return computePrimitiveMatchScore(value, typeName);
        }
        
        if (isWrapperType(typeName)) {
            Class<?> wrapperClass = getWrapperClass(typeName);
            if (wrapperClass != null && wrapperClass.isInstance(value)) {
                return MATCH_SCORE_WIDENING;
            }
            if (isBoxingCompatible(value, typeName)) {
                return MATCH_SCORE_COMPATIBLE;
            }
        }
        
        if (value instanceof RuntimeObject) {
            RuntimeObject runtimeObj = (RuntimeObject) value;
            ScriptClass valueClass = runtimeObj.getScriptClass();
            if (valueClass != null) {
                String valueClassName = valueClass.getName();
                if (valueClassName.equals(typeName) || valueClass.getQualifiedName().equals(typeName)) {
                    return MATCH_SCORE_EXACT;
                }
                if (isInterfaceImplementedBy(valueClass, typeName)) {
                    return MATCH_SCORE_WIDENING;
                }
                if (isSubclassOf(valueClass, typeName)) {
                    return MATCH_SCORE_WIDENING;
                }
            }
            return MATCH_SCORE_COMPATIBLE;
        }
        
        if (value instanceof ScriptClass) {
            ScriptClass scriptClass = (ScriptClass) value;
            if (scriptClass.getName().equals(typeName) || scriptClass.getQualifiedName().equals(typeName)) {
                return MATCH_SCORE_EXACT;
            }
        }
        
        if (value instanceof Class) {
            Class<?> clazz = (Class<?>) value;
            if (clazz.getSimpleName().equals(typeName) || clazz.getName().equals(typeName)) {
                return MATCH_SCORE_EXACT;
            }
        }
        
        Class<?> valueClass = value.getClass();
        if (valueClass.getSimpleName().equals(typeName) || valueClass.getName().equals(typeName)) {
            return MATCH_SCORE_EXACT;
        }
        
        if (isAssignableFrom(typeName, valueClass)) {
            return MATCH_SCORE_WIDENING;
        }
        
        return MATCH_SCORE_COMPATIBLE;
    }
    
    private int computePrimitiveMatchScore(Object value, String typeName) {
        if (value instanceof Number) {
            Number num = (Number) value;
            switch (typeName) {
                case "int":
                    if (value instanceof Integer) return MATCH_SCORE_EXACT;
                    if (isWideningConvertible(num, typeName)) return MATCH_SCORE_WIDENING;
                    return MATCH_SCORE_NONE;
                case "long":
                    if (value instanceof Long) return MATCH_SCORE_EXACT;
                    if (value instanceof Integer) return MATCH_SCORE_WIDENING;
                    if (isWideningConvertible(num, typeName)) return MATCH_SCORE_COMPATIBLE;
                    return MATCH_SCORE_NONE;
                case "float":
                    if (value instanceof Float) return MATCH_SCORE_EXACT;
                    if (value instanceof Long || value instanceof Integer) return MATCH_SCORE_WIDENING;
                    if (value instanceof Double) return MATCH_SCORE_COMPATIBLE;
                    return MATCH_SCORE_NONE;
                case "double":
                    if (value instanceof Double) return MATCH_SCORE_EXACT;
                    if (value instanceof Float || value instanceof Long || value instanceof Integer) return MATCH_SCORE_WIDENING;
                    return MATCH_SCORE_NONE;
                case "byte":
                    if (value instanceof Byte) return MATCH_SCORE_EXACT;
                    return MATCH_SCORE_NONE;
                case "short":
                    if (value instanceof Short) return MATCH_SCORE_EXACT;
                    if (value instanceof Byte) return MATCH_SCORE_WIDENING;
                    return MATCH_SCORE_NONE;
                case "char":
                    if (value instanceof Character) return MATCH_SCORE_EXACT;
                    return MATCH_SCORE_NONE;
                case "boolean":
                    if (value instanceof Boolean) return MATCH_SCORE_EXACT;
                    return MATCH_SCORE_NONE;
            }
        }
        
        if (typeName.equals("boolean") && value instanceof Boolean) return MATCH_SCORE_EXACT;
        if (typeName.equals("char") && value instanceof Character) return MATCH_SCORE_EXACT;
        
        return MATCH_SCORE_NONE;
    }
    
    private boolean isWideningConvertible(Number num, String targetType) {
        if (num instanceof Integer) {
            return targetType.equals("long") || targetType.equals("float") || targetType.equals("double");
        }
        if (num instanceof Long) {
            return targetType.equals("float") || targetType.equals("double");
        }
        if (num instanceof Float) {
            return targetType.equals("double");
        }
        if (num instanceof Double) {
            return false;
        }
        if (num instanceof Byte) {
            return targetType.equals("short") || targetType.equals("int") || targetType.equals("long") || 
                   targetType.equals("float") || targetType.equals("double");
        }
        if (num instanceof Short) {
            return targetType.equals("int") || targetType.equals("long") || 
                   targetType.equals("float") || targetType.equals("double");
        }
        return false;
    }
    
    private boolean isWrapperType(String typeName) {
        return TypeConstants.isWrapperType(typeName);
    }
    
    private Class<?> getWrapperClass(String typeName) {
        return TypeConstants.getWrapperClass(typeName);
    }
    
    private boolean isBoxingCompatible(Object value, String typeName) {
        if (value instanceof Integer && (typeName.equals("Integer") || typeName.equals("java.lang.Integer"))) return true;
        if (value instanceof Long && (typeName.equals("Long") || typeName.equals("java.lang.Long"))) return true;
        if (value instanceof Double && (typeName.equals("Double") || typeName.equals("java.lang.Double"))) return true;
        if (value instanceof Float && (typeName.equals("Float") || typeName.equals("java.lang.Float"))) return true;
        if (value instanceof Boolean && (typeName.equals("Boolean") || typeName.equals("java.lang.Boolean"))) return true;
        if (value instanceof Byte && (typeName.equals("Byte") || typeName.equals("java.lang.Byte"))) return true;
        if (value instanceof Short && (typeName.equals("Short") || typeName.equals("java.lang.Short"))) return true;
        if (value instanceof Character && (typeName.equals("Character") || typeName.equals("java.lang.Character"))) return true;
        return false;
    }
    
    private boolean isInterfaceImplementedBy(ScriptClass scriptClass, String interfaceName) {
        for (ScriptClass iface : scriptClass.getInterfaces()) {
            if (iface.getName().equals(interfaceName) || iface.getQualifiedName().equals(interfaceName)) {
                return true;
            }
            if (isInterfaceImplementedBy(iface, interfaceName)) {
                return true;
            }
        }
        if (scriptClass.getSuperClass() != null) {
            return isInterfaceImplementedBy(scriptClass.getSuperClass(), interfaceName);
        }
        return false;
    }
    
    private boolean isSubclassOf(ScriptClass scriptClass, String superClassName) {
        ScriptClass current = scriptClass.getSuperClass();
        while (current != null) {
            if (current.getName().equals(superClassName) || current.getQualifiedName().equals(superClassName)) {
                return true;
            }
            current = current.getSuperClass();
        }
        return false;
    }
    
    private boolean isAssignableFrom(String typeName, Class<?> valueClass) {
        try {
            String fullName = typeName.contains(".") ? typeName : "java.lang." + typeName;
            Class<?> targetClass = Class.forName(fullName);
            return targetClass.isAssignableFrom(valueClass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private boolean isPrimitiveType(String typeName) {
        return TypeConstants.isPrimitiveType(typeName);
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
    
    public static ScriptClass registerClass(Environment env, String name) {
        return registerClass(env, name, Modifier.PUBLIC);
    }
    
    public static ScriptClass registerClass(Environment env, String name, int modifiers) {
        ScriptClass scriptClass = new ScriptClass(name, name, modifiers, null, new ArrayList<>(), null);
        scriptClass.setEnvironment(env);
        env.defineClass(name, scriptClass);
        return scriptClass;
    }
    
    public static ScriptClass registerClass(Environment env, String name, int modifiers, ScriptClass superClass) {
        ScriptClass scriptClass = new ScriptClass(name, name, modifiers, superClass, new ArrayList<>(), null);
        scriptClass.setEnvironment(env);
        env.defineClass(name, scriptClass);
        return scriptClass;
    }
    
    public static ScriptClass registerClass(Environment env, String name, int modifiers, ScriptClass superClass, List<ScriptClass> interfaces) {
        ScriptClass scriptClass = new ScriptClass(name, name, modifiers, superClass, interfaces, null);
        scriptClass.setEnvironment(env);
        env.defineClass(name, scriptClass);
        return scriptClass;
    }
    
    public static ScriptClass registerRecord(Environment env, String name) {
        return registerRecord(env, name, Modifier.PUBLIC | Modifier.FINAL);
    }
    
    public static ScriptClass registerRecord(Environment env, String name, int modifiers) {
        int recordModifiers = modifiers | Modifier.FINAL;
        ScriptClass recordClass = new ScriptClass(name, name, recordModifiers, null, new ArrayList<>(), null);
        recordClass.setEnvironment(env);
        env.defineClass(name, recordClass);
        return recordClass;
    }
    
    public static ScriptClass registerSealedClass(Environment env, String name, String... permittedSubtypes) {
        return registerSealedClass(env, name, Modifier.PUBLIC | Modifier.SEALED, permittedSubtypes);
    }
    
    public static ScriptClass registerSealedClass(Environment env, String name, int modifiers, String... permittedSubtypes) {
        int sealedModifiers = modifiers | Modifier.SEALED;
        ScriptClass sealedClass = new ScriptClass(name, name, sealedModifiers, null, new ArrayList<>(), null);
        sealedClass.setEnvironment(env);
        
        if (permittedSubtypes != null && permittedSubtypes.length > 0) {
            List<Type> permits = new ArrayList<>();
            for (String subtype : permittedSubtypes) {
                permits.add(new Type(null, subtype, null, 0, null));
            }
            sealedClass.setPermittedSubtypes(permits);
        }
        
        env.defineClass(name, sealedClass);
        return sealedClass;
    }
    
    public ScriptClass registerMethod(String methodName, Function<Object[], Object> implementation) {
        return registerMethod(methodName, Modifier.PUBLIC, implementation);
    }
    
    public ScriptClass registerMethod(String methodName, int modifiers, Function<Object[], Object> implementation) {
        NativeMethod method = NativeMethod.createVarArgs(methodName, modifiers, "Object", this, implementation);
        this.addMethod(method);
        return this;
    }
    
    public ScriptClass registerMethod(String methodName, int modifiers, String returnType, 
                                      String[] paramTypes, String[] paramNames,
                                      Function<Object[], Object> implementation) {
        NativeMethod method = NativeMethod.create(methodName, modifiers, returnType, paramTypes, paramNames, this, implementation);
        this.addMethod(method);
        return this;
    }
    
    public ScriptClass registerStaticMethod(String methodName, Function<Object[], Object> implementation) {
        return registerMethod(methodName, Modifier.PUBLIC | Modifier.STATIC, implementation);
    }
    
    public ScriptClass registerStaticMethod(String methodName, int modifiers, Function<Object[], Object> implementation) {
        return registerMethod(methodName, modifiers | Modifier.STATIC, implementation);
    }
    
    public ScriptClass registerConstructor(Function<Object[], Object> implementation) {
        return registerConstructor(Modifier.PUBLIC, implementation);
    }
    
    public ScriptClass registerConstructor(int modifiers, Function<Object[], Object> implementation) {
        NativeMethod constructor = new NativeMethod(this.name, modifiers, 
            new Type(null, this.name, new ArrayList<>(), 0, new ArrayList<>()), 
            new ArrayList<>(), false, this, true, implementation);
        this.addConstructor(constructor);
        return this;
    }
    
    public ScriptClass registerField(String fieldName, Object value) {
        return registerField(fieldName, Modifier.PUBLIC, value);
    }
    
    public ScriptClass registerField(String fieldName, int modifiers, Object value) {
        Type fieldType = new Type(null, value != null ? value.getClass().getSimpleName() : "Object", 
                                 new ArrayList<>(), 0, new ArrayList<>());
        ScriptField field = new ScriptField(fieldName, modifiers, fieldType, null, this);
        this.addField(field);
        
        if ((modifiers & Modifier.STATIC) != 0) {
            this.staticFieldValues.put(fieldName, value);
        }
        return this;
    }
    
    public ScriptClass registerStaticField(String fieldName, Object value) {
        return registerField(fieldName, Modifier.PUBLIC | Modifier.STATIC, value);
    }
    
    public ScriptClass registerStaticField(String fieldName, int modifiers, Object value) {
        return registerField(fieldName, modifiers | Modifier.STATIC, value);
    }
}
