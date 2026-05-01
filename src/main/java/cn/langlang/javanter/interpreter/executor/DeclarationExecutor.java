package cn.langlang.javanter.interpreter.executor;

import cn.langlang.javanter.ast.base.AbstractASTVisitor;
import cn.langlang.javanter.ast.base.ASTVisitor;
import cn.langlang.javanter.ast.declaration.*;
import cn.langlang.javanter.ast.misc.Annotation;
import cn.langlang.javanter.ast.misc.EnumConstant;
import cn.langlang.javanter.ast.statement.BlockStatement;
import cn.langlang.javanter.ast.statement.LocalVariableDeclaration;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.type.TypeParameter;
import cn.langlang.javanter.interpreter.Interpreter;
import cn.langlang.javanter.parser.Modifier;
import cn.langlang.javanter.runtime.environment.Environment;
import cn.langlang.javanter.runtime.model.*;
import cn.langlang.javanter.runtime.generics.*;
import java.util.*;

public class DeclarationExecutor extends AbstractASTVisitor<Object> {
    private final Interpreter interpreter;
    
    public DeclarationExecutor(Interpreter interpreter) {
        this.interpreter = interpreter;
    }
    
    @Override
    public Object visitClassDeclaration(ClassDeclaration node) {
        String name = node.getName();
        ScriptClass superClass = null;
        
        if (node.getSuperClass() != null) {
            superClass = interpreter.resolveClass(node.getSuperClass());
            if (superClass != null && (superClass.getModifiers() & Modifier.FINAL) != 0) {
                throw new RuntimeException("Cannot inherit from final class '" + superClass.getName() + "'");
            }
            if (superClass != null && (superClass.getModifiers() & Modifier.SEALED) != 0) {
                validateSealedSubclass(name, node.getModifiers(), superClass);
            }
        }
        
        List<ScriptClass> interfaces = new ArrayList<>();
        for (Type iface : node.getInterfaces()) {
            ScriptClass ifaceClass = interpreter.resolveClass(iface);
            if (ifaceClass != null && (ifaceClass.getModifiers() & Modifier.SEALED) != 0) {
                validateSealedSubclass(name, node.getModifiers(), ifaceClass);
            }
            interfaces.add(ifaceClass);
        }
        
        ScriptClass scriptClass = new ScriptClass(name, name, node.getModifiers(),
                                                  superClass, interfaces, node);
        
        if (node.getTypeParameters() != null && !node.getTypeParameters().isEmpty()) {
            scriptClass.setTypeParameters(node.getTypeParameters());
            GenericClassInfo genericInfo = createGenericClassInfo(node, scriptClass);
            scriptClass.setGenericInfo(genericInfo);
        }
        
        interpreter.getGlobalEnv().defineClass(name, scriptClass);
        interpreter.getLoadedClasses().put(name, scriptClass);
        
        if (node.getPermittedSubtypes() != null && !node.getPermittedSubtypes().isEmpty()) {
            scriptClass.setPermittedSubtypes(node.getPermittedSubtypes());
        }
        
        for (FieldDeclaration field : node.getFields()) {
            ScriptField scriptField = new ScriptField(
                field.getName(), field.getModifiers(), field.getType(),
                field.getInitializer(), scriptClass, field.getAnnotations());
            scriptClass.addField(scriptField);
            
            if (scriptClass.getGenericInfo() != null) {
                GenericType genericFieldType = createGenericType(field.getType(), scriptClass.getGenericInfo());
                scriptClass.getGenericInfo().registerGenericField(field.getName(), genericFieldType);
            }
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            if ((method.getModifiers() & Modifier.NATIVE) != 0) {
                throw new RuntimeException("Native methods are not supported: " + 
                    node.getName() + "." + method.getName() + "()");
            }
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), method.getModifiers(), method.getReturnType(),
                method.getParameters(), method.isVarArgs(), method.getBody(),
                scriptClass, false, method.isDefault(), method.getAnnotations());
            
            if (method.getTypeParameters() != null && !method.getTypeParameters().isEmpty()) {
                scriptMethod.setTypeParameters(method.getTypeParameters());
                GenericMethodInfo genericMethodInfo = createGenericMethodInfo(method, scriptMethod, scriptClass);
                scriptMethod.setGenericInfo(genericMethodInfo);
            }
            
            scriptClass.addMethod(scriptMethod);
            
            checkFinalMethodOverride(scriptMethod, superClass);
        }
        
        for (ConstructorDeclaration constructor : node.getConstructors()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                constructor.getName(), constructor.getModifiers(), null,
                constructor.getParameters(), false, constructor.getBody(),
                scriptClass, true, false, constructor.getAnnotations());
            scriptClass.addConstructor(scriptMethod);
        }
        
        for (InitializerBlock init : node.getInitializers()) {
            if (init.isStatic()) {
                scriptClass.addStaticInitializer(init);
            } else {
                scriptClass.addInstanceInitializer(init);
            }
        }
        
        for (TypeDeclaration nested : node.getNestedTypes()) {
            String nestedFullName = name + "." + nested.getName();
            registerNestedType(nested, nestedFullName);
        }
        
        processAnnotations(node, scriptClass);
        
        if ((node.getModifiers() & Modifier.ABSTRACT) == 0) {
            checkAbstractMethodsImplemented(scriptClass);
        }
        
        if (scriptClass.isGenericClass()) {
            generateBridgeMethods(scriptClass);
        }

        return null;
    }
    
    private GenericClassInfo createGenericClassInfo(ClassDeclaration node, ScriptClass scriptClass) {
        List<TypeParameter> typeParams = node.getTypeParameters();
        
        GenericType genericSuperClass = null;
        if (node.getSuperClass() != null) {
            genericSuperClass = createGenericType(node.getSuperClass(), null);
        }
        
        List<GenericType> genericInterfaces = new ArrayList<>();
        for (Type iface : node.getInterfaces()) {
            genericInterfaces.add(createGenericType(iface, null));
        }
        
        return new GenericClassInfo(scriptClass, typeParams, genericSuperClass, genericInterfaces);
    }
    
    private GenericMethodInfo createGenericMethodInfo(MethodDeclaration method, ScriptMethod scriptMethod, 
                                                       ScriptClass scriptClass) {
        List<TypeParameter> typeParams = method.getTypeParameters();
        
        GenericType genericReturnType = null;
        if (method.getReturnType() != null) {
            genericReturnType = createGenericType(method.getReturnType(), scriptClass.getGenericInfo());
        }
        
        List<GenericType> genericParamTypes = new ArrayList<>();
        for (ParameterDeclaration param : method.getParameters()) {
            genericParamTypes.add(createGenericType(param.getType(), scriptClass.getGenericInfo()));
        }
        
        GenericMethodInfo methodInfo = new GenericMethodInfo(scriptMethod, typeParams, 
            genericReturnType, genericParamTypes);
        
        if (scriptClass.getGenericInfo() != null) {
            scriptClass.getGenericInfo().registerGenericMethod(method.getName(), methodInfo);
        }
        
        return methodInfo;
    }
    
    private GenericType createGenericType(Type type, GenericClassInfo classInfo) {
        if (type == null) return null;
        
        String typeName = type.getName();
        
        if (classInfo != null) {
            TypeVariableImpl typeVar = classInfo.getTypeVariable(typeName);
            if (typeVar != null) {
                return typeVar;
            }
        }
        
        if (!type.getTypeArguments().isEmpty()) {
            List<GenericType> typeArgs = new ArrayList<>();
            for (var typeArg : type.getTypeArguments()) {
                typeArgs.add(createGenericTypeFromArgument(typeArg, classInfo));
            }
            
            Class<?> rawType = tryLoadClass(typeName);
            if (rawType != null) {
                return new ParameterizedTypeImpl(rawType, typeArgs);
            }
            
            return new ParameterizedTypeImpl(typeName, typeArgs);
        }
        
        Class<?> rawType = tryLoadClass(typeName);
        if (rawType != null) {
            return new ClassTypeImpl(rawType);
        }
        
        return new ClassTypeImpl(typeName);
    }
    
    private GenericType createGenericTypeFromArgument(cn.langlang.javanter.ast.type.TypeArgument typeArg, 
                                                       GenericClassInfo classInfo) {
        if (typeArg == null) return new ClassTypeImpl(Object.class);
        
        var wildcardKind = typeArg.getWildcardKind();
        
        switch (wildcardKind) {
            case UNBOUNDED:
                return WildcardTypeImpl.unbounded();
            case EXTENDS:
                GenericType extendsBound = createGenericType(typeArg.getBoundType(), classInfo);
                return WildcardTypeImpl.extendsBound(extendsBound);
            case SUPER:
                GenericType superBound = createGenericType(typeArg.getBoundType(), classInfo);
                return WildcardTypeImpl.superBound(superBound);
            default:
                return createGenericType(typeArg.getType(), classInfo);
        }
    }
    
    private Class<?> tryLoadClass(String typeName) {
        try {
            if (typeName.equals("int")) return int.class;
            if (typeName.equals("long")) return long.class;
            if (typeName.equals("short")) return short.class;
            if (typeName.equals("byte")) return byte.class;
            if (typeName.equals("char")) return char.class;
            if (typeName.equals("boolean")) return boolean.class;
            if (typeName.equals("float")) return float.class;
            if (typeName.equals("double")) return double.class;
            if (typeName.equals("void")) return void.class;
            
            if (typeName.equals("String")) return String.class;
            if (typeName.equals("Integer")) return Integer.class;
            if (typeName.equals("Long")) return Long.class;
            if (typeName.equals("Double")) return Double.class;
            if (typeName.equals("Boolean")) return Boolean.class;
            if (typeName.equals("Object")) return Object.class;
            if (typeName.equals("Class")) return Class.class;
            
            return Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    private void generateBridgeMethods(ScriptClass scriptClass) {
        if (scriptClass.getGenericInfo() == null) return;
        
        List<ScriptMethod> bridges = TypeErasure.generateBridgeMethods(scriptClass, scriptClass.getGenericInfo());
        for (ScriptMethod bridge : bridges) {
            scriptClass.addMethod(bridge);
            scriptClass.getGenericInfo().addBridgeMethod(bridge);
        }
    }
    
    private void checkAbstractMethodsImplemented(ScriptClass scriptClass) {
        List<ScriptMethod> unimplementedMethods = new ArrayList<>();
        collectAbstractMethods(scriptClass, unimplementedMethods, new HashSet<>());
        
        for (ScriptMethod abstractMethod : unimplementedMethods) {
            boolean implemented = false;
            for (ScriptMethod method : scriptClass.getMethods(abstractMethod.getName())) {
                if (!method.isAbstract() && methodSignaturesMatch(method, abstractMethod)) {
                    implemented = true;
                    break;
                }
            }
            if (!implemented) {
                String declaringClassName = abstractMethod.getDeclaringClass() != null ? 
                    abstractMethod.getDeclaringClass().getName() : "<unknown>";
                throw new RuntimeException("Class '" + scriptClass.getName() + 
                    "' is not abstract and does not override abstract method '" + 
                    abstractMethod.getName() + "(" + getParameterTypesString(abstractMethod) + ")' in '" + 
                    declaringClassName + "'");
            }
        }
    }
    
    private void collectAbstractMethods(ScriptClass scriptClass, List<ScriptMethod> methods, Set<ScriptClass> visited) {
        if (scriptClass == null || visited.contains(scriptClass)) {
            return;
        }
        visited.add(scriptClass);
        
        for (ScriptMethod method : scriptClass.getMethods().values().stream().flatMap(List::stream).toList()) {
            if (method.isAbstract() && !method.isDefault()) {
                methods.add(method);
            }
        }
        
        if (scriptClass.getSuperClass() != null) {
            collectAbstractMethods(scriptClass.getSuperClass(), methods, visited);
        }
        
        for (ScriptClass iface : scriptClass.getInterfaces()) {
            collectAbstractMethods(iface, methods, visited);
        }
    }
    
    private boolean methodSignaturesMatch(ScriptMethod m1, ScriptMethod m2) {
        if (!m1.getName().equals(m2.getName())) {
            return false;
        }
        List<ParameterDeclaration> params1 = m1.getParameters();
        List<ParameterDeclaration> params2 = m2.getParameters();
        if (params1.size() != params2.size()) {
            return false;
        }
        if (m1.isVarArgs() != m2.isVarArgs()) {
            return false;
        }
        return true;
    }
    
    private String getParameterTypesString(ScriptMethod method) {
        StringBuilder sb = new StringBuilder();
        List<ParameterDeclaration> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            Type type = params.get(i).getType();
            sb.append(type.getName());
            if (i == params.size() - 1 && method.isVarArgs()) {
                sb.append("...");
            }
        }
        return sb.toString();
    }
    
    private void checkFinalMethodOverride(ScriptMethod method, ScriptClass superClass) {
        if (superClass == null) {
            return;
        }
        
        for (ScriptMethod superMethod : superClass.getMethods(method.getName())) {
            if (methodSignaturesMatch(method, superMethod)) {
                if ((superMethod.getModifiers() & Modifier.FINAL) != 0) {
                    throw new RuntimeException("Cannot override final method '" + 
                        method.getName() + "(" + getParameterTypesString(superMethod) + ")' in class '" + 
                        superClass.getName() + "'");
                }
                
                if (method.isStatic() && !superMethod.isStatic()) {
                    throw new RuntimeException("Cannot override instance method '" + 
                        method.getName() + "(" + getParameterTypesString(superMethod) + ")' with static method in class '" + 
                        superClass.getName() + "'");
                }
                
                if (!method.isStatic() && superMethod.isStatic()) {
                    throw new RuntimeException("Cannot override static method '" + 
                        method.getName() + "(" + getParameterTypesString(superMethod) + ")' with instance method in class '" + 
                        superClass.getName() + "'");
                }
                
                if (!method.isStatic()) {
                    checkOverrideAccessModifier(method, superMethod, superClass);
                    checkOverrideReturnType(method, superMethod, superClass);
                }
                
                break;
            }
        }
    }
    
    private void checkOverrideAccessModifier(ScriptMethod method, ScriptMethod superMethod, ScriptClass superClass) {
        int methodMods = method.getModifiers();
        int superMods = superMethod.getModifiers();
        
        boolean methodIsPublic = (methodMods & Modifier.PUBLIC) != 0;
        boolean methodIsProtected = (methodMods & Modifier.PROTECTED) != 0;
        boolean methodIsPrivate = (methodMods & Modifier.PRIVATE) != 0;
        boolean methodIsPackage = !methodIsPublic && !methodIsProtected && !methodIsPrivate;
        
        boolean superIsPublic = (superMods & Modifier.PUBLIC) != 0;
        boolean superIsProtected = (superMods & Modifier.PROTECTED) != 0;
        boolean superIsPrivate = (superMods & Modifier.PRIVATE) != 0;
        boolean superIsPackage = !superIsPublic && !superIsProtected && !superIsPrivate;
        
        if (superIsPublic && !methodIsPublic) {
            throw new RuntimeException("Cannot reduce visibility of public method '" + 
                method.getName() + "(" + getParameterTypesString(superMethod) + ")' in class '" + 
                superClass.getName() + "'");
        }
        
        if (superIsProtected && !methodIsProtected && !methodIsPublic) {
            throw new RuntimeException("Cannot reduce visibility of protected method '" + 
                method.getName() + "(" + getParameterTypesString(superMethod) + ")' in class '" + 
                superClass.getName() + "'");
        }
        
        if (superIsPackage && methodIsPrivate) {
            throw new RuntimeException("Cannot reduce visibility of package-private method '" + 
                method.getName() + "(" + getParameterTypesString(superMethod) + ")' in class '" + 
                superClass.getName() + "'");
        }
    }
    
    private void checkOverrideReturnType(ScriptMethod method, ScriptMethod superMethod, ScriptClass superClass) {
        Type methodReturn = method.getReturnType();
        Type superReturn = superMethod.getReturnType();
        
        if (methodReturn == null && superReturn == null) {
            return;
        }
        
        if (methodReturn == null || superReturn == null) {
            return;
        }
        
        String methodReturnName = methodReturn.getName();
        String superReturnName = superReturn.getName();
        
        if (methodReturnName.equals(superReturnName)) {
            return;
        }
        
        if (methodReturnName.equals("void") || superReturnName.equals("void")) {
            if (!methodReturnName.equals(superReturnName)) {
                throw new RuntimeException("Return type '" + methodReturnName + "' is not compatible with '" + 
                    superReturnName + "' in overridden method '" + method.getName() + "(" + 
                    getParameterTypesString(superMethod) + ")' in class '" + superClass.getName() + "'");
            }
        }
        
        if (methodReturnName.equals("Object") || superReturnName.equals("Object")) {
            return;
        }
    }

    private void processAnnotations(ClassDeclaration classDecl, ScriptClass scriptClass) {
        cn.langlang.javanter.annotation.ProcessingEnvironment env =
            new cn.langlang.javanter.annotation.ProcessingEnvironment(interpreter, interpreter.getGlobalEnv());
        
        for (cn.langlang.javanter.annotation.AnnotationProcessor processor : interpreter.getAnnotationProcessors()) {
            if (processor instanceof cn.langlang.javanter.annotation.AbstractAnnotationProcessor) {
                env.registerProcessor((cn.langlang.javanter.annotation.AbstractAnnotationProcessor) processor);
            }
        }
        
        env.invokeProcessorsForClass(classDecl, scriptClass);
    }
    
    private void validateSealedSubclass(String subclassName, int subclassModifiers, ScriptClass sealedParent) {
        boolean isFinal = (subclassModifiers & Modifier.FINAL) != 0;
        boolean isSealed = (subclassModifiers & Modifier.SEALED) != 0;
        boolean isNonSealed = (subclassModifiers & Modifier.NON_SEALED) != 0;
        
        if (!isFinal && !isSealed && !isNonSealed) {
            throw new RuntimeException("Class '" + subclassName + "' must be declared final, sealed, or non-sealed " +
                "because it extends/implements sealed class/interface '" + sealedParent.getName() + "'");
        }
        
        List<Type> permittedTypes = sealedParent.getPermittedSubtypes();
        if (permittedTypes != null && !permittedTypes.isEmpty()) {
            boolean isPermitted = false;
            for (Type permitted : permittedTypes) {
                if (permitted.getName().equals(subclassName)) {
                    isPermitted = true;
                    break;
                }
            }
            if (!isPermitted) {
                throw new RuntimeException("Class '" + subclassName + "' is not in the permits list of sealed class/interface '" +
                    sealedParent.getName() + "'");
            }
        }
    }
    
    private void registerNestedType(TypeDeclaration nested, String fullName) {
        if (nested instanceof ClassDeclaration) {
            ClassDeclaration classDecl = (ClassDeclaration) nested;
            ScriptClass superClass = null;
            if (classDecl.getSuperClass() != null) {
                superClass = interpreter.resolveClass(classDecl.getSuperClass());
            }
            
            List<ScriptClass> interfaces = new ArrayList<>();
            for (Type iface : classDecl.getInterfaces()) {
                interfaces.add(interpreter.resolveClass(iface));
            }
            
            ScriptClass scriptClass = new ScriptClass(fullName, fullName, classDecl.getModifiers(),
                                                      superClass, interfaces, classDecl);
            
            interpreter.getGlobalEnv().defineClass(fullName, scriptClass);
            interpreter.getLoadedClasses().put(fullName, scriptClass);
            
            for (FieldDeclaration field : classDecl.getFields()) {
                ScriptField scriptField = new ScriptField(
                    field.getName(), field.getModifiers(), field.getType(),
                    field.getInitializer(), scriptClass, field.getAnnotations());
                scriptClass.addField(scriptField);
            }
            
            for (MethodDeclaration method : classDecl.getMethods()) {
                ScriptMethod scriptMethod = new ScriptMethod(
                    method.getName(), method.getModifiers(), method.getReturnType(),
                    method.getParameters(), method.isVarArgs(), method.getBody(),
                    scriptClass, false, method.isDefault(), method.getAnnotations());
                scriptClass.addMethod(scriptMethod);
            }
            
            for (ConstructorDeclaration constructor : classDecl.getConstructors()) {
                ScriptMethod scriptMethod = new ScriptMethod(
                    constructor.getName(), constructor.getModifiers(), null,
                    constructor.getParameters(), false, constructor.getBody(),
                    scriptClass, true, false, constructor.getAnnotations());
                scriptClass.addConstructor(scriptMethod);
            }
            
            for (TypeDeclaration nestedNested : classDecl.getNestedTypes()) {
                registerNestedType(nestedNested, fullName + "." + nestedNested.getName());
            }
        }
    }
    
    @Override
    public Object visitInterfaceDeclaration(InterfaceDeclaration node) {
        String name = node.getName();
        
        List<ScriptClass> extendsInterfaces = new ArrayList<>();
        for (Type iface : node.getExtendsInterfaces()) {
            extendsInterfaces.add(interpreter.resolveClass(iface));
        }
        
        ScriptClass scriptClass = new ScriptClass(name, name, node.getModifiers(),
                                                  null, extendsInterfaces, node);
        
        interpreter.getGlobalEnv().defineClass(name, scriptClass);
        interpreter.getLoadedClasses().put(name, scriptClass);
        
        if (node.getPermittedSubtypes() != null && !node.getPermittedSubtypes().isEmpty()) {
            scriptClass.setPermittedSubtypes(node.getPermittedSubtypes());
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), method.getModifiers(), method.getReturnType(),
                method.getParameters(), method.isVarArgs(), method.getBody(),
                scriptClass, false, method.isDefault(), method.getAnnotations());
            scriptClass.addMethod(scriptMethod);
        }
        
        for (FieldDeclaration constant : node.getConstants()) {
            ScriptField scriptField = new ScriptField(
                constant.getName(), constant.getModifiers(), constant.getType(),
                constant.getInitializer(), scriptClass, constant.getAnnotations());
            scriptClass.addField(scriptField);
        }
        
        boolean hasFunctionalInterface = false;
        for (Annotation ann : node.getAnnotations()) {
            if (ann.getTypeName().equals("FunctionalInterface") || 
                ann.getTypeName().endsWith(".FunctionalInterface")) {
                hasFunctionalInterface = true;
                break;
            }
        }
        
        if (hasFunctionalInterface) {
            int abstractMethodCount = 0;
            for (MethodDeclaration method : node.getMethods()) {
                if ((method.getModifiers() & Modifier.DEFAULT) == 0 && 
                    (method.getModifiers() & Modifier.STATIC) == 0) {
                    abstractMethodCount++;
                }
            }
            if (abstractMethodCount != 1) {
                throw new RuntimeException("@FunctionalInterface annotation requires exactly one abstract method, but " + 
                    name + " has " + abstractMethodCount + " abstract methods");
            }
        }
        
        return null;
    }
    
    @Override
    public Object visitEnumDeclaration(EnumDeclaration node) {
        String name = node.getName();
        
        List<ScriptClass> interfaces = new ArrayList<>();
        for (Type iface : node.getInterfaces()) {
            interfaces.add(interpreter.resolveClass(iface));
        }
        
        ScriptEnum scriptEnum = new ScriptEnum(name, name, node.getModifiers(),
                                                  interfaces, node);
        
        interpreter.getGlobalEnv().defineClass(name, scriptEnum);
        interpreter.getLoadedClasses().put(name, scriptEnum);
        
        for (FieldDeclaration field : node.getFields()) {
            ScriptField scriptField = new ScriptField(
                field.getName(), field.getModifiers(), field.getType(),
                field.getInitializer(), scriptEnum, field.getAnnotations());
            scriptEnum.addField(scriptField);
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), method.getModifiers(), method.getReturnType(),
                method.getParameters(), method.isVarArgs(), method.getBody(),
                scriptEnum, false, method.isDefault(), method.getAnnotations());
            scriptEnum.addMethod(scriptMethod);
        }
        
        for (ConstructorDeclaration constructor : node.getConstructors()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                constructor.getName(), constructor.getModifiers(), null,
                constructor.getParameters(), false, constructor.getBody(),
                scriptEnum, true, false, constructor.getAnnotations());
            scriptEnum.addConstructor(scriptMethod);
        }
        
        int ordinal = 0;
        for (EnumConstant constant : node.getConstants()) {
            ScriptClass constantClass = scriptEnum;
            
            if (constant.getAnonymousClass() != null) {
                ClassDeclaration anonClass = constant.getAnonymousClass();
                constantClass = new ScriptClass(name + "$" + constant.getName(), name + "$" + constant.getName(),
                    0, scriptEnum, new ArrayList<>(), anonClass);
                
                for (FieldDeclaration field : anonClass.getFields()) {
                    ScriptField scriptField = new ScriptField(
                        field.getName(), field.getModifiers(), field.getType(),
                        field.getInitializer(), constantClass, field.getAnnotations());
                    constantClass.addField(scriptField);
                }
                
                for (MethodDeclaration method : anonClass.getMethods()) {
                    ScriptMethod scriptMethod = new ScriptMethod(
                        method.getName(), method.getModifiers(), method.getReturnType(),
                        method.getParameters(), method.isVarArgs(), method.getBody(),
                        constantClass, false, method.isDefault(), method.getAnnotations());
                    constantClass.addMethod(scriptMethod);
                }
            }
            
            RuntimeObject enumObj = new RuntimeObject(constantClass);
            enumObj.setField("name", constant.getName());
            enumObj.setField("ordinal", ordinal);
            
            for (ScriptField field : scriptEnum.getFields().values()) {
                if (!field.isStatic()) {
                    Object value = null;
                    if (field.getInitializer() != null) {
                        value = field.getInitializer().accept(interpreter.getExpressionEvaluator());
                    }
                    enumObj.setField(field.getName(), value);
                }
            }
            
            if (!constant.getArguments().isEmpty()) {
                List<Object> args = new ArrayList<>();
                for (cn.langlang.javanter.ast.expression.Expression arg : constant.getArguments()) {
                    args.add(arg.accept(interpreter.getExpressionEvaluator()));
                }
                ScriptMethod constructor = interpreter.findConstructor(scriptEnum, args);
                if (constructor != null) {
                    interpreter.invokeMethod(enumObj, constructor, args);
                }
            } else if (!scriptEnum.getConstructors().isEmpty()) {
                ScriptMethod defaultConstructor = interpreter.findConstructor(scriptEnum, new ArrayList<>());
                if (defaultConstructor != null) {
                    interpreter.invokeMethod(enumObj, defaultConstructor, new ArrayList<>());
                }
            }
            
            scriptEnum.addConstant(constant.getName(), enumObj, ordinal);
            interpreter.getGlobalEnv().defineVariable(constant.getName(), enumObj);
            ordinal++;
        }
        
        generateEnumMethods(scriptEnum);
        
        return null;
    }
    
    private void generateEnumMethods(ScriptEnum scriptEnum) {
        ScriptMethod valuesMethod = new ScriptMethod(
            "values",
            Modifier.PUBLIC | Modifier.STATIC,
            new cn.langlang.javanter.ast.type.Type(null, scriptEnum.getName() + "[]", null, 0, null),
            new ArrayList<>(),
            false,
            null,
            scriptEnum,
            false,
            false,
            new ArrayList<>()
        );
        valuesMethod.setNativeImplementation(args -> scriptEnum.values());
        scriptEnum.addMethod(valuesMethod);
        
        List<ParameterDeclaration> valueOfParams = new ArrayList<>();
        valueOfParams.add(new ParameterDeclaration(
            null, 0, new cn.langlang.javanter.ast.type.Type(null, "String", null, 0, null), "name", false, new ArrayList<>()
        ));
        
        ScriptMethod valueOfMethod = new ScriptMethod(
            "valueOf",
            Modifier.PUBLIC | Modifier.STATIC,
            new cn.langlang.javanter.ast.type.Type(null, scriptEnum.getName(), null, 0, null),
            valueOfParams,
            false,
            null,
            scriptEnum,
            false,
            false,
            new ArrayList<>()
        );
        valueOfMethod.setNativeImplementation(args -> {
            String enumName = (String) args[0];
            return scriptEnum.valueOf(enumName);
        });
        scriptEnum.addMethod(valueOfMethod);
    }
    
    @Override
    public Object visitAnnotationDeclaration(AnnotationDeclaration node) {
        return null;
    }
    
    @Override
    public Object visitRecordDeclaration(RecordDeclaration node) {
        String name = node.getName();
        
        List<ScriptClass> interfaces = new ArrayList<>();
        for (Type iface : node.getImplementsInterfaces()) {
            interfaces.add(interpreter.resolveClass(iface));
        }
        
        ScriptClass recordClass = new ScriptClass(name, name, Modifier.FINAL | node.getModifiers(),
                                                  null, interfaces, null);
        
        interpreter.getGlobalEnv().defineClass(name, recordClass);
        interpreter.getLoadedClasses().put(name, recordClass);
        
        for (RecordDeclaration.RecordComponent component : node.getComponents()) {
            ScriptField scriptField = new ScriptField(
                component.getName(), Modifier.PRIVATE | Modifier.FINAL, component.getType(),
                null, recordClass, component.getAnnotations());
            recordClass.addField(scriptField);
            
            Type returnType = component.getType();
            List<ParameterDeclaration> accessorParams = new ArrayList<>();
            ScriptMethod accessor = new ScriptMethod(
                component.getName(), Modifier.PUBLIC, returnType,
                accessorParams, false, null, recordClass, false, false,
                component.getAnnotations()
            );
            final String fieldName = component.getName();
            accessor.setNativeImplementation(args -> {
                if (args[0] instanceof RuntimeObject) {
                    RuntimeObject robj = (RuntimeObject) args[0];
                    return robj.getField(fieldName);
                }
                return null;
            });
            recordClass.addMethod(accessor);
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                throw new RuntimeException("Record methods must be static: " + method.getName());
            }
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), method.getModifiers(), method.getReturnType(),
                method.getParameters(), method.isVarArgs(), method.getBody(),
                recordClass, false, method.isDefault(), method.getAnnotations());
            recordClass.addMethod(scriptMethod);
        }
        
        for (FieldDeclaration field : node.getStaticFields()) {
            ScriptField scriptField = new ScriptField(
                field.getName(), field.getModifiers(), field.getType(),
                field.getInitializer(), recordClass, field.getAnnotations());
            recordClass.addField(scriptField);
        }
        
        generateRecordMethods(recordClass, node);
        
        return null;
    }
    
    private void generateRecordMethods(ScriptClass recordClass, RecordDeclaration node) {
        Type stringType = new Type(null, "String", null, 0, null);
        List<ParameterDeclaration> toStringParams = new ArrayList<>();
        ScriptMethod toStringMethod = new ScriptMethod(
            "toString", Modifier.PUBLIC, stringType,
            toStringParams, false, null, recordClass, false, false, new ArrayList<>()
        );
        toStringMethod.setNativeImplementation(args -> {
            StringBuilder sb = new StringBuilder();
            sb.append(recordClass.getName()).append("[");
            boolean first = true;
            for (RecordDeclaration.RecordComponent comp : node.getComponents()) {
                if (!first) sb.append(", ");
                sb.append(comp.getName()).append("=");
                first = false;
            }
            sb.append("]");
            return sb.toString();
        });
        recordClass.addMethod(toStringMethod);
        
        Type intType = new Type(null, "int", null, 0, null);
        List<ParameterDeclaration> hashCodeParams = new ArrayList<>();
        ScriptMethod hashCodeMethod = new ScriptMethod(
            "hashCode", Modifier.PUBLIC, intType,
            hashCodeParams, false, null, recordClass, false, false, new ArrayList<>()
        );
        hashCodeMethod.setNativeImplementation(args -> {
            int result = 1;
            for (RecordDeclaration.RecordComponent comp : node.getComponents()) {
                result = 31 * result + (comp.getName() != null ? comp.getName().hashCode() : 0);
            }
            return result;
        });
        recordClass.addMethod(hashCodeMethod);
        
        Type objectType = new Type(null, "Object", null, 0, null);
        List<ParameterDeclaration> equalsParams = new ArrayList<>();
        equalsParams.add(new ParameterDeclaration(null, 0, objectType, "obj", false, new ArrayList<>()));
        ScriptMethod equalsMethod = new ScriptMethod(
            "equals", Modifier.PUBLIC, new Type(null, "boolean", null, 0, null),
            equalsParams, false, null, recordClass, false, false, new ArrayList<>()
        );
        equalsMethod.setNativeImplementation(args -> {
            if (args[0] == recordClass) return true;
            if (!(args[0] instanceof RuntimeObject)) return false;
            RuntimeObject other = (RuntimeObject) args[0];
            return other.getScriptClass() == recordClass;
        });
        recordClass.addMethod(equalsMethod);
        
        List<ParameterDeclaration> constructorParams = new ArrayList<>();
        for (RecordDeclaration.RecordComponent comp : node.getComponents()) {
            constructorParams.add(new ParameterDeclaration(null, 0, comp.getType(), comp.getName(), 
                                                       false, comp.getAnnotations()));
        }
        ScriptMethod canonicalConstructor = new ScriptMethod(
            node.getName(), Modifier.PUBLIC, null,
            constructorParams, false, null, recordClass, true, false, new ArrayList<>()
        );
        final List<RecordDeclaration.RecordComponent> components = node.getComponents();
        canonicalConstructor.setNativeImplementation(args -> {
            if (args[0] instanceof RuntimeObject) {
                RuntimeObject robj = (RuntimeObject) args[0];
                for (int i = 0; i < components.size(); i++) {
                    String fieldName = components.get(i).getName();
                    Object value = args[i + 1];
                    robj.setField(fieldName, value);
                }
            }
            return args[0];
        });
        recordClass.addConstructor(canonicalConstructor);
    }
    
    @Override
    public Object visitFieldDeclaration(FieldDeclaration node) {
        return null;
    }
    
    @Override
    public Object visitMethodDeclaration(MethodDeclaration node) {
        return null;
    }
    
    @Override
    public Object visitConstructorDeclaration(ConstructorDeclaration node) {
        return null;
    }
    
    @Override
    public Object visitInitializerBlock(InitializerBlock node) {
        return node.getBody().accept(interpreter.getStatementExecutor());
    }
    
    @Override
    public Object visitParameterDeclaration(ParameterDeclaration node) {
        return null;
    }
    
    @Override
    public Object visitLocalVariableDeclaration(LocalVariableDeclaration node) {
        for (LocalVariableDeclaration.VariableDeclarator declarator : node.getDeclarators()) {
            Object value = null;
            if (declarator.getInitializer() != null) {
                value = declarator.getInitializer().accept(interpreter.getExpressionEvaluator());
            }
            interpreter.getCurrentEnv().defineVariable(declarator.getName(), value);
        }
        return null;
    }
    
    @Override
    public Object visitEnumConstant(EnumConstant node) {
        return null;
    }
}
