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

        return null;
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
