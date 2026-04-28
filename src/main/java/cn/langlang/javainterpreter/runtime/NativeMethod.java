package cn.langlang.javainterpreter.runtime;

import cn.langlang.javainterpreter.ast.*;
import cn.langlang.javainterpreter.parser.Modifier;
import java.util.*;
import java.util.function.Function;

public class NativeMethod extends ScriptMethod {
    private final Function<Object[], Object> nativeImplementation;
    
    public NativeMethod(String name, int modifiers, Type returnType,
                       List<ParameterDeclaration> parameters, boolean isVarArgs,
                       ScriptClass declaringClass, boolean isConstructor,
                       Function<Object[], Object> nativeImplementation) {
        super(name, modifiers, returnType, parameters, isVarArgs, null, declaringClass, isConstructor, false);
        this.nativeImplementation = nativeImplementation;
    }
    
    public Function<Object[], Object> getNativeImplementation() {
        return nativeImplementation;
    }
    
    public static NativeMethod create(String name, int modifiers, String returnType,
                                     String[] paramTypes, String[] paramNames,
                                     ScriptClass declaringClass,
                                     Function<Object[], Object> implementation) {
        List<ParameterDeclaration> params = new ArrayList<>();
        if (paramTypes != null && paramNames != null) {
            for (int i = 0; i < paramTypes.length; i++) {
                Type paramType = new Type(null, paramTypes[i], new ArrayList<>(), 0, new ArrayList<>());
                params.add(new ParameterDeclaration(null, 0, paramType, paramNames[i], false, new ArrayList<>()));
            }
        }
        
        Type retType = new Type(null, returnType, new ArrayList<>(), 0, new ArrayList<>());
        
        return new NativeMethod(name, modifiers, retType, params, false, declaringClass, false, implementation);
    }
    
    public static NativeMethod createVarArgs(String name, int modifiers, String returnType,
                                            ScriptClass declaringClass,
                                            Function<Object[], Object> implementation) {
        List<ParameterDeclaration> params = new ArrayList<>();
        Type varArgType = new Type(null, "Object", new ArrayList<>(), 1, new ArrayList<>());
        params.add(new ParameterDeclaration(null, 0, varArgType, "args", true, new ArrayList<>()));
        
        Type retType = new Type(null, returnType, new ArrayList<>(), 0, new ArrayList<>());
        
        return new NativeMethod(name, modifiers, retType, params, true, declaringClass, false, implementation);
    }
    
    public static NativeMethod createStatic(String name, int modifiers, String returnType,
                                           String[] paramTypes, String[] paramNames,
                                           ScriptClass declaringClass,
                                           Function<Object[], Object> implementation) {
        return create(name, modifiers | Modifier.STATIC, returnType, paramTypes, paramNames, declaringClass, implementation);
    }
}
