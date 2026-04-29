package cn.langlang.javainterpreter.interpreter.executor;

import cn.langlang.javainterpreter.ast.base.AbstractASTVisitor;
import cn.langlang.javainterpreter.ast.base.ASTVisitor;
import cn.langlang.javainterpreter.ast.declaration.*;
import cn.langlang.javainterpreter.ast.misc.Annotation;
import cn.langlang.javainterpreter.ast.misc.EnumConstant;
import cn.langlang.javainterpreter.ast.statement.BlockStatement;
import cn.langlang.javainterpreter.ast.statement.LocalVariableDeclaration;
import cn.langlang.javainterpreter.ast.type.Type;
import cn.langlang.javainterpreter.interpreter.Interpreter;
import cn.langlang.javainterpreter.parser.Modifier;
import cn.langlang.javainterpreter.runtime.environment.Environment;
import cn.langlang.javainterpreter.runtime.model.*;
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
        }
        
        List<ScriptClass> interfaces = new ArrayList<>();
        for (Type iface : node.getInterfaces()) {
            interfaces.add(interpreter.resolveClass(iface));
        }
        
        ScriptClass scriptClass = new ScriptClass(name, name, node.getModifiers(),
                                                  superClass, interfaces, node);
        
        interpreter.getGlobalEnv().defineClass(name, scriptClass);
        interpreter.getLoadedClasses().put(name, scriptClass);
        
        for (FieldDeclaration field : node.getFields()) {
            ScriptField scriptField = new ScriptField(
                field.getName(), field.getModifiers(), field.getType(),
                field.getInitializer(), scriptClass, field.getAnnotations());
            scriptClass.addField(scriptField);
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

        return null;
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
        cn.langlang.javainterpreter.annotation.ProcessingEnvironment env =
            new cn.langlang.javainterpreter.annotation.ProcessingEnvironment(interpreter, interpreter.getGlobalEnv());
        
        for (cn.langlang.javainterpreter.annotation.AnnotationProcessor processor : interpreter.getAnnotationProcessors()) {
            if (processor instanceof cn.langlang.javainterpreter.annotation.AbstractAnnotationProcessor) {
                env.registerProcessor((cn.langlang.javainterpreter.annotation.AbstractAnnotationProcessor) processor);
            }
        }
        
        env.invokeProcessorsForClass(classDecl, scriptClass);
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
        
        ScriptClass scriptClass = new ScriptClass(name, name, node.getModifiers(),
                                                  null, interfaces, node);
        
        interpreter.getGlobalEnv().defineClass(name, scriptClass);
        interpreter.getLoadedClasses().put(name, scriptClass);
        
        for (FieldDeclaration field : node.getFields()) {
            ScriptField scriptField = new ScriptField(
                field.getName(), field.getModifiers(), field.getType(),
                field.getInitializer(), scriptClass, field.getAnnotations());
            scriptClass.addField(scriptField);
        }
        
        for (MethodDeclaration method : node.getMethods()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                method.getName(), method.getModifiers(), method.getReturnType(),
                method.getParameters(), method.isVarArgs(), method.getBody(),
                scriptClass, false, method.isDefault(), method.getAnnotations());
            scriptClass.addMethod(scriptMethod);
        }
        
        for (ConstructorDeclaration constructor : node.getConstructors()) {
            ScriptMethod scriptMethod = new ScriptMethod(
                constructor.getName(), constructor.getModifiers(), null,
                constructor.getParameters(), false, constructor.getBody(),
                scriptClass, true, false, constructor.getAnnotations());
            scriptClass.addConstructor(scriptMethod);
        }
        
        int ordinal = 0;
        for (EnumConstant constant : node.getConstants()) {
            ScriptClass constantClass = scriptClass;
            
            if (constant.getAnonymousClass() != null) {
                ClassDeclaration anonClass = constant.getAnonymousClass();
                constantClass = new ScriptClass(name + "$" + constant.getName(), name + "$" + constant.getName(),
                    0, scriptClass, new ArrayList<>(), anonClass);
                
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
            
            for (ScriptField field : scriptClass.getFields().values()) {
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
                for (cn.langlang.javainterpreter.ast.expression.Expression arg : constant.getArguments()) {
                    args.add(arg.accept(interpreter.getExpressionEvaluator()));
                }
                ScriptMethod constructor = interpreter.findConstructor(scriptClass, args);
                if (constructor != null) {
                    interpreter.invokeMethod(enumObj, constructor, args);
                }
            }
            
            interpreter.getGlobalEnv().defineVariable(constant.getName(), enumObj);
            ordinal++;
        }
        
        return null;
    }
    
    @Override
    public Object visitAnnotationDeclaration(AnnotationDeclaration node) {
        return null;
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
