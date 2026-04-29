package cn.langlang.javanter.runtime.generics;

import cn.langlang.javanter.ast.declaration.*;
import cn.langlang.javanter.ast.expression.*;
import cn.langlang.javanter.ast.statement.*;
import cn.langlang.javanter.ast.type.Type;
import cn.langlang.javanter.ast.type.TypeArgument;
import cn.langlang.javanter.ast.type.TypeParameter;
import cn.langlang.javanter.parser.Modifier;
import cn.langlang.javanter.runtime.model.*;
import java.util.*;

public class TypeErasure {
    
    public static GenericType erase(GenericType type) {
        if (type == null) return null;
        return type.getErasedType();
    }
    
    public static Class<?> eraseToClass(GenericType type) {
        if (type == null) return Object.class;
        
        GenericType erased = type.getErasedType();
        if (erased instanceof ClassTypeImpl) {
            return ((ClassTypeImpl) erased).getJavaClass();
        }
        
        return Object.class;
    }
    
    public static String eraseToTypeName(GenericType type) {
        if (type == null) return "Object";
        
        if (type.isTypeVariable()) {
            TypeVariableImpl typeVar = (TypeVariableImpl) type;
            if (typeVar.getBounds().isEmpty()) {
                return "Object";
            }
            return typeVar.getBounds().get(0).getTypeName();
        }
        
        if (type.isParameterized()) {
            ParameterizedTypeImpl paramType = (ParameterizedTypeImpl) type;
            if (paramType.getRawType() != null) {
                return paramType.getRawType().getSimpleName();
            }
            if (paramType.getRawScriptClass() != null) {
                return paramType.getRawScriptClass().getName();
            }
            if (paramType.getRawTypeName() != null) {
                return paramType.getRawTypeName();
            }
        }
        
        return type.getTypeName();
    }
    
    public static GenericType getErasedReturnType(GenericMethodInfo method) {
        if (method == null) return null;
        GenericType returnType = method.getGenericReturnType();
        if (returnType == null) return null;
        return erase(returnType);
    }
    
    public static List<GenericType> getErasedParameterTypes(GenericMethodInfo method) {
        if (method == null) return Collections.emptyList();
        
        List<GenericType> erased = new ArrayList<>();
        for (GenericType paramType : method.getGenericParameterTypes()) {
            erased.add(erase(paramType));
        }
        return erased;
    }
    
    public static boolean needsBridgeMethod(ScriptMethod method, ScriptMethod superMethod,
                                            GenericClassInfo classInfo) {
        if (method == null || superMethod == null) return false;
        if (method.isStatic() || superMethod.isStatic()) return false;
        if (method.isPrivate() || superMethod.isPrivate()) return false;
        if (superMethod.isFinal()) return false;
        
        if (!method.getName().equals(superMethod.getName())) return false;
        
        if (classInfo == null || !classInfo.isGenericClass()) return false;
        
        GenericMethodInfo methodInfo = classInfo.getGenericMethod(method.getName());
        if (methodInfo == null || !methodInfo.isGenericMethod()) return false;
        
        GenericType returnType = methodInfo.getGenericReturnType();
        if (returnType == null || !returnType.isTypeVariable()) return false;
        
        GenericType superReturn = getMethodReturnType(superMethod);
        if (superReturn == null) return false;
        
        GenericType erasedReturn = erase(returnType);
        return !erasedReturn.equals(superReturn);
    }
    
    private static GenericType getMethodReturnType(ScriptMethod method) {
        if (method == null) return null;
        Type returnType = method.getReturnType();
        if (returnType == null) return null;
        return new ClassTypeImpl(returnType.getName());
    }
    
    public static ScriptMethod generateBridgeMethod(ScriptMethod method, ScriptMethod superMethod,
                                                    GenericClassInfo classInfo) {
        if (!needsBridgeMethod(method, superMethod, classInfo)) {
            return null;
        }
        
        int modifiers = method.getModifiers() & ~(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);
        modifiers |= Modifier.BRIDGE | Modifier.SYNTHETIC;
        
        GenericType erasedReturn = erase(getMethodReturnType(method));
        String returnTypeName = erasedReturn != null ? erasedReturn.getTypeName() : "void";
        
        Type erasedReturnType = method.getReturnType();
        
        List<ParameterDeclaration> erasedParams = new ArrayList<>();
        for (ParameterDeclaration param : method.getParameters()) {
            erasedParams.add(new ParameterDeclaration(
                null, param.getModifiers(), param.getType(), param.getName(), 
                param.isVarArgs(), param.getAnnotations()
            ));
        }
        
        BlockStatement body = generateBridgeBody(method, superMethod, classInfo);
        
        ScriptMethod bridgeMethod = new ScriptMethod(
            method.getName(),
            modifiers,
            erasedReturnType,
            erasedParams,
            method.isVarArgs(),
            body,
            method.getDeclaringClass(),
            method.isConstructor(),
            false,
            method.getAnnotations()
        );
        
        return bridgeMethod;
    }
    
    private static BlockStatement generateBridgeBody(ScriptMethod method, ScriptMethod superMethod,
                                                     GenericClassInfo classInfo) {
        List<Statement> statements = new ArrayList<>();
        
        List<Expression> args = new ArrayList<>();
        for (ParameterDeclaration param : method.getParameters()) {
            args.add(new IdentifierExpression(null, param.getName()));
        }
        
        Expression methodCall = new MethodInvocationExpression(
            null,
            new ThisExpression(null, null),
            Collections.emptyList(),
            method.getName(),
            args
        );
        
        if (method.getReturnType() != null && !method.getReturnType().getName().equals("void")) {
            Expression castExpr = new CastExpression(
                null,
                method.getReturnType(),
                methodCall
            );
            statements.add(new ReturnStatement(null, castExpr));
        } else {
            statements.add(new ExpressionStatement(null, methodCall));
            statements.add(new ReturnStatement(null, null));
        }
        
        return new BlockStatement(null, statements);
    }
    
    public static List<ScriptMethod> generateBridgeMethods(ScriptClass scriptClass, 
                                                           GenericClassInfo classInfo) {
        List<ScriptMethod> bridges = new ArrayList<>();
        
        if (scriptClass == null || classInfo == null) return bridges;
        if (!classInfo.isGenericClass()) return bridges;
        
        for (ScriptMethod method : scriptClass.getMethods().values().stream()
             .flatMap(List::stream).toList()) {
            
            if (method.isStatic() || method.isPrivate() || method.isConstructor()) {
                continue;
            }
            
            ScriptClass superClass = scriptClass.getSuperClass();
            while (superClass != null) {
                for (ScriptMethod superMethod : superClass.getMethods(method.getName())) {
                    ScriptMethod bridge = generateBridgeMethod(method, superMethod, classInfo);
                    if (bridge != null) {
                        bridges.add(bridge);
                    }
                }
                superClass = superClass.getSuperClass();
            }
            
            for (ScriptClass iface : scriptClass.getInterfaces()) {
                if (iface == null) continue;
                for (ScriptMethod ifaceMethod : iface.getMethods(method.getName())) {
                    ScriptMethod bridge = generateBridgeMethod(method, ifaceMethod, classInfo);
                    if (bridge != null) {
                        bridges.add(bridge);
                    }
                }
            }
        }
        
        return bridges;
    }
    
    public static Map<String, GenericType> inferTypeArguments(GenericMethodInfo methodInfo,
                                                              List<Object> arguments) {
        Map<String, GenericType> inferred = new HashMap<>();
        
        if (methodInfo == null || arguments == null) return inferred;
        
        List<GenericType> paramTypes = methodInfo.getGenericParameterTypes();
        
        for (int i = 0; i < Math.min(paramTypes.size(), arguments.size()); i++) {
            GenericType paramType = paramTypes.get(i);
            Object argValue = arguments.get(i);
            
            inferFromArgument(paramType, argValue, inferred);
        }
        
        return inferred;
    }
    
    private static void inferFromArgument(GenericType paramType, Object argValue,
                                          Map<String, GenericType> inferred) {
        if (paramType == null || argValue == null) return;
        
        if (paramType.isTypeVariable()) {
            TypeVariableImpl typeVar = (TypeVariableImpl) paramType;
            GenericType argType = inferTypeFromValue(argValue);
            if (argType != null) {
                inferred.putIfAbsent(typeVar.getName(), argType);
            }
        }
        
        if (paramType.isParameterized()) {
            ParameterizedTypeImpl paramParamType = (ParameterizedTypeImpl) paramType;
            List<GenericType> typeArgs = paramParamType.getTypeArguments();
            
            if (argValue instanceof RuntimeObject) {
                RuntimeObject runtimeObj = (RuntimeObject) argValue;
                GenericClassInfo objClassInfo = runtimeObj.getScriptClass().getGenericInfo();
                if (objClassInfo != null) {
                    for (GenericType typeArg : typeArgs) {
                        if (typeArg.isTypeVariable()) {
                            TypeVariableImpl typeVar = (TypeVariableImpl) typeArg;
                            GenericType binding = objClassInfo.getBinding(typeVar.getName());
                            if (binding != null) {
                                inferred.putIfAbsent(typeVar.getName(), binding);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static GenericType inferTypeFromValue(Object value) {
        if (value == null) return null;
        
        if (value instanceof RuntimeObject) {
            RuntimeObject runtimeObj = (RuntimeObject) value;
            return new ClassTypeImpl(runtimeObj.getScriptClass());
        }
        
        return new ClassTypeImpl(value.getClass());
    }
}
