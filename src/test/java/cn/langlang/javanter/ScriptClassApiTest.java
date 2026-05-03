package cn.langlang.javanter;

import cn.langlang.javanter.api.JavaInterpreter;
import cn.langlang.javanter.interpreter.Interpreter;
import cn.langlang.javanter.parser.Modifier;
import cn.langlang.javanter.runtime.environment.Environment;
import cn.langlang.javanter.runtime.model.RuntimeObject;
import cn.langlang.javanter.runtime.model.ScriptClass;
import cn.langlang.javanter.runtime.model.ScriptField;
import cn.langlang.javanter.runtime.model.ScriptMethod;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScriptClassApiTest {
    
    private JavaInterpreter interpreter;
    private Environment env;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
        env = interpreter.getGlobalEnvironment();
    }
    
    @Nested
    @DisplayName("Environment Registration Tests")
    class EnvironmentRegistrationTests {
        
        @Test
        @DisplayName("Register simple class")
        public void testRegisterSimpleClass() {
            ScriptClass myClass = env.registerClass("MyClass");
            
            assertNotNull(myClass);
            assertEquals("MyClass", myClass.getName());
            assertEquals("MyClass", myClass.getQualifiedName());
            assertTrue((myClass.getModifiers() & Modifier.PUBLIC) != 0);
            assertSame(env, myClass.getEnvironment());
            assertSame(myClass, env.getClass("MyClass"));
        }
        
        @Test
        @DisplayName("Register class with modifiers")
        public void testRegisterClassWithModifiers() {
            ScriptClass myClass = env.registerClass("FinalClass", Modifier.PUBLIC | Modifier.FINAL);
            
            assertTrue((myClass.getModifiers() & Modifier.FINAL) != 0);
            assertTrue((myClass.getModifiers() & Modifier.PUBLIC) != 0);
        }
        
        @Test
        @DisplayName("Register class with superclass")
        public void testRegisterClassWithSuperClass() {
            ScriptClass parentClass = env.registerClass("Parent");
            ScriptClass childClass = env.registerClass("Child", Modifier.PUBLIC, parentClass);
            
            assertSame(parentClass, childClass.getSuperClass());
        }
        
        @Test
        @DisplayName("Register class with interfaces")
        public void testRegisterClassWithInterfaces() {
            ScriptClass iface = env.registerClass("MyInterface", Modifier.INTERFACE);
            ScriptClass impl = env.registerClass("MyImpl", Modifier.PUBLIC, null, Arrays.asList(iface));
            
            assertEquals(1, impl.getInterfaces().size());
            assertSame(iface, impl.getInterfaces().get(0));
        }
        
        @Test
        @DisplayName("Register record")
        public void testRegisterRecord() {
            ScriptClass record = env.registerRecord("Point");
            
            assertNotNull(record);
            assertTrue(record.isRecord());
            assertTrue((record.getModifiers() & Modifier.FINAL) != 0);
        }
        
        @Test
        @DisplayName("Register sealed class")
        public void testRegisterSealedClass() {
            ScriptClass sealed = env.registerSealedClass("Shape", "Circle", "Rectangle");
            
            assertNotNull(sealed);
            assertTrue(sealed.isSealed());
            assertEquals(2, sealed.getPermittedSubtypes().size());
        }
        
        @Test
        @DisplayName("Register sealed class with modifiers")
        public void testRegisterSealedClassWithModifiers() {
            ScriptClass sealed = env.registerSealedClass("AbstractShape", 
                Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.SEALED, "Circle", "Square");
            
            assertTrue(sealed.isSealed());
            assertTrue((sealed.getModifiers() & Modifier.ABSTRACT) != 0);
        }
    }
    
    @Nested
    @DisplayName("ScriptClass Method Registration Tests")
    class MethodRegistrationTests {
        
        private ScriptClass testClass;
        
        @BeforeEach
        public void setUpClass() {
            testClass = env.registerClass("TestClass");
        }
        
        @Test
        @DisplayName("Register instance method")
        public void testRegisterInstanceMethod() {
            ScriptClass result = testClass.registerMethod("greet", args -> "Hello");
            
            assertSame(testClass, result);
            assertNotNull(testClass.getMethods("greet"));
            assertEquals(1, testClass.getMethods("greet").size());
        }
        
        @Test
        @DisplayName("Register instance method with modifiers")
        public void testRegisterInstanceMethodWithModifiers() {
            testClass.registerMethod("privateMethod", Modifier.PRIVATE, args -> "private");
            
            ScriptMethod method = testClass.getMethods("privateMethod").get(0);
            assertTrue((method.getModifiers() & Modifier.PRIVATE) != 0);
            assertFalse(method.isStatic());
        }
        
        @Test
        @DisplayName("Register static method")
        public void testRegisterStaticMethod() {
            testClass.registerStaticMethod("staticGreet", args -> "Static Hello");
            
            ScriptMethod method = testClass.getMethods("staticGreet").get(0);
            assertTrue(method.isStatic());
        }
        
        @Test
        @DisplayName("Register static method with modifiers")
        public void testRegisterStaticMethodWithModifiers() {
            testClass.registerStaticMethod("privateStatic", Modifier.PRIVATE, args -> "private static");
            
            ScriptMethod method = testClass.getMethods("privateStatic").get(0);
            assertTrue(method.isStatic());
            assertTrue((method.getModifiers() & Modifier.PRIVATE) != 0);
        }
        
        @Test
        @DisplayName("Register method with full signature")
        public void testRegisterMethodWithSignature() {
            testClass.registerMethod("add", Modifier.PUBLIC | Modifier.STATIC,
                "int", new String[]{"int", "int"}, new String[]{"a", "b"},
                args -> (int)args[0] + (int)args[1]);
            
            ScriptMethod method = testClass.getMethods("add").get(0);
            assertEquals(2, method.getParameters().size());
            assertEquals("add", method.getName());
        }
        
        @Test
        @DisplayName("Register constructor")
        public void testRegisterConstructor() {
            testClass.registerConstructor(args -> {
                RuntimeObject instance = (RuntimeObject) args[0];
                instance.setField("initialized", true);
                return null;
            });
            
            assertEquals(1, testClass.getConstructors().size());
            assertTrue(testClass.getConstructors().get(0).isConstructor());
        }
        
        @Test
        @DisplayName("Register constructor with modifiers")
        public void testRegisterConstructorWithModifiers() {
            testClass.registerConstructor(Modifier.PRIVATE, args -> null);
            
            ScriptMethod constructor = testClass.getConstructors().get(0);
            assertTrue((constructor.getModifiers() & Modifier.PRIVATE) != 0);
        }
        
        @Test
        @DisplayName("Method chaining")
        public void testMethodChaining() {
            ScriptClass result = testClass
                .registerMethod("method1", args -> "1")
                .registerMethod("method2", args -> "2")
                .registerStaticMethod("staticMethod", args -> "static");
            
            assertSame(testClass, result);
            assertEquals(3, testClass.getMethods().size());
        }
    }
    
    @Nested
    @DisplayName("ScriptClass Field Registration Tests")
    class FieldRegistrationTests {
        
        private ScriptClass testClass;
        
        @BeforeEach
        public void setUpClass() {
            testClass = env.registerClass("TestClass");
        }
        
        @Test
        @DisplayName("Register instance field")
        public void testRegisterInstanceField() {
            ScriptClass result = testClass.registerField("name", "default");
            
            assertSame(testClass, result);
            assertNotNull(testClass.getField("name"));
            assertEquals("name", testClass.getField("name").getName());
        }
        
        @Test
        @DisplayName("Register field with modifiers")
        public void testRegisterFieldWithModifiers() {
            testClass.registerField("privateField", Modifier.PRIVATE, "value");
            
            ScriptField field = testClass.getField("privateField");
            assertTrue((field.getModifiers() & Modifier.PRIVATE) != 0);
            assertFalse(field.isStatic());
        }
        
        @Test
        @DisplayName("Register static field")
        public void testRegisterStaticField() {
            testClass.registerStaticField("VERSION", "1.0.0");
            
            ScriptField field = testClass.getField("VERSION");
            assertTrue(field.isStatic());
            assertEquals("1.0.0", testClass.getStaticFieldValues().get("VERSION"));
        }
        
        @Test
        @DisplayName("Register static field with modifiers")
        public void testRegisterStaticFieldWithModifiers() {
            testClass.registerStaticField("CONSTANT", Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC, 42);
            
            ScriptField field = testClass.getField("CONSTANT");
            assertTrue(field.isStatic());
            assertTrue(field.isFinal());
        }
        
        @Test
        @DisplayName("Field with null value")
        public void testRegisterFieldWithNullValue() {
            testClass.registerField("nullableField", null);
            
            ScriptField field = testClass.getField("nullableField");
            assertNotNull(field);
        }
    }
    
    @Nested
    @DisplayName("Inner Class Registration Tests")
    class InnerClassTests {
        
        @Test
        @DisplayName("Register inner class")
        public void testRegisterInnerClass() {
            ScriptClass outer = env.registerClass("Outer");
            ScriptClass inner = outer.registerInnerClass("Inner", Modifier.PUBLIC);
            
            assertNotNull(inner);
            assertEquals("Inner", inner.getName());
            assertEquals("Outer$Inner", inner.getQualifiedName());
            assertSame(outer, inner.getEnclosingClass());
            assertSame(env, inner.getEnvironment());
        }
        
        @Test
        @DisplayName("Register inner class with superclass")
        public void testRegisterInnerClassWithSuperClass() {
            ScriptClass parent = env.registerClass("Parent");
            ScriptClass outer = env.registerClass("Outer");
            ScriptClass inner = outer.registerInnerClass("Inner", Modifier.PUBLIC, parent);
            
            assertSame(parent, inner.getSuperClass());
        }
        
        @Test
        @DisplayName("Register inner class with interfaces")
        public void testRegisterInnerClassWithInterfaces() {
            ScriptClass iface = env.registerClass("Runnable", Modifier.INTERFACE);
            ScriptClass outer = env.registerClass("Outer");
            ScriptClass inner = outer.registerInnerClass("Inner", Modifier.PUBLIC, null, Arrays.asList(iface));
            
            assertEquals(1, inner.getInterfaces().size());
            assertSame(iface, inner.getInterfaces().get(0));
        }
        
        @Test
        @DisplayName("Register static inner class")
        public void testRegisterStaticInnerClass() {
            ScriptClass outer = env.registerClass("Outer");
            ScriptClass inner = outer.registerInnerClass("StaticInner", Modifier.PUBLIC | Modifier.STATIC);
            
            assertTrue((inner.getModifiers() & Modifier.STATIC) != 0);
        }
        
        @Test
        @DisplayName("Inner class registered in environment")
        public void testInnerClassRegisteredInEnvironment() {
            ScriptClass outer = env.registerClass("Outer");
            ScriptClass inner = outer.registerInnerClass("Inner", Modifier.PUBLIC);
            
            assertSame(inner, env.getClass("Inner"));
            assertSame(inner, env.getClass("Outer$Inner"));
        }
        
        @Test
        @DisplayName("Register nested inner class")
        public void testRegisterNestedInnerClass() {
            ScriptClass level1 = env.registerClass("Level1");
            ScriptClass level2 = level1.registerInnerClass("Level2", Modifier.PUBLIC);
            ScriptClass level3 = level2.registerInnerClass("Level3", Modifier.PUBLIC);
            
            assertEquals("Level1$Level2$Level3", level3.getQualifiedName());
            assertSame(level2, level3.getEnclosingClass());
        }
        
        @Test
        @DisplayName("Throw exception when environment not set")
        public void testThrowWhenEnvironmentNotSet() {
            ScriptClass orphanClass = new ScriptClass("Orphan", "Orphan", Modifier.PUBLIC, null, new java.util.ArrayList<>(), null);
            
            assertThrows(IllegalStateException.class, () -> {
                orphanClass.registerInnerClass("Inner", Modifier.PUBLIC);
            });
        }
    }
    
    @Nested
    @DisplayName("ScriptClass Static Method Invocation Tests")
    class StaticMethodInvocationTests {
        
        private ScriptClass mathClass;
        
        @BeforeEach
        public void setUpClass() {
            mathClass = env.registerClass("MathUtils")
                .registerStaticMethod("add", args -> (int)args[0] + (int)args[1])
                .registerStaticMethod("multiply", args -> (int)args[0] * (int)args[1])
                .registerStaticMethod("concat", args -> args[0].toString() + args[1].toString())
                .registerMethod("instanceMethod", args -> "instance");
        }
        
        private void setupInterpreter() {
            RuntimeObject.setCurrentInterpreter(interpreter.getInterpreter());
        }
        
        @Test
        @DisplayName("Invoke static method with varargs")
        public void testInvokeStaticMethodVarargs() {
            setupInterpreter();
            
            Object result = mathClass.invokeStaticMethod("add", 10, 20);
            
            assertEquals(30, result);
        }
        
        @Test
        @DisplayName("Invoke static method with list")
        public void testInvokeStaticMethodWithList() {
            setupInterpreter();
            
            List<Object> args = Arrays.asList(5, 7);
            Object result = mathClass.invokeStaticMethod("multiply", args);
            
            assertEquals(35, result);
        }
        
        @Test
        @DisplayName("Invoke static method with string args")
        public void testInvokeStaticMethodWithStrings() {
            setupInterpreter();
            
            Object result = mathClass.invokeStaticMethod("concat", "Hello", "World");
            
            assertEquals("HelloWorld", result);
        }
        
        @Test
        @DisplayName("Throw when method not found")
        public void testThrowWhenMethodNotFound() {
            setupInterpreter();
            
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                mathClass.invokeStaticMethod("nonExistent", 1, 2);
            });
            
            assertTrue(exception.getMessage().contains("not found"));
        }
        
        @Test
        @DisplayName("Throw when method is not static")
        public void testThrowWhenMethodNotStatic() {
            setupInterpreter();
            
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                mathClass.invokeStaticMethod("instanceMethod");
            });
            
            assertTrue(exception.getMessage().contains("not static"));
        }
        
        @Test
        @DisplayName("Throw when no interpreter available")
        public void testThrowWhenNoInterpreter() {
            RuntimeObject.setCurrentInterpreter(null);
            
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                mathClass.invokeStaticMethod("add", 1, 2);
            });
            
            assertTrue(exception.getMessage().contains("No interpreter"));
        }
    }
    
    @Nested
    @DisplayName("RuntimeObject Instance Method Invocation Tests")
    class RuntimeObjectMethodInvocationTests {
        
        private ScriptClass personClass;
        private RuntimeObject person;
        
        @BeforeEach
        public void setUpClass() {
            personClass = env.registerClass("Person")
                .registerField("name", "Unknown")
                .registerField("age", 0)
                .registerMethod("getName", args -> {
                    RuntimeObject self = (RuntimeObject) args[0];
                    return self.getField("name");
                })
                .registerMethod("setName", args -> {
                    RuntimeObject self = (RuntimeObject) args[0];
                    self.setField("name", args[1]);
                    return null;
                })
                .registerMethod("getAge", args -> {
                    RuntimeObject self = (RuntimeObject) args[0];
                    return self.getField("age");
                })
                .registerMethod("isAdult", args -> {
                    RuntimeObject self = (RuntimeObject) args[0];
                    int age = (int) self.getField("age");
                    return age >= 18;
                });
            
            person = new RuntimeObject(personClass);
            person.setField("name", "Alice");
            person.setField("age", 25);
            
            RuntimeObject.setCurrentInterpreter(interpreter.getInterpreter());
        }
        
        @Test
        @DisplayName("Invoke instance method with varargs")
        public void testInvokeMethodVarargs() {
            Object result = person.invokeMethod("getName");
            
            assertEquals("Alice", result);
        }
        
        @Test
        @DisplayName("Invoke instance method that modifies state")
        public void testInvokeMethodModifiesState() {
            person.invokeMethod("setName", "Bob");
            
            assertEquals("Bob", person.getField("name"));
        }
        
        @Test
        @DisplayName("Invoke method returning boolean")
        public void testInvokeMethodReturningBoolean() {
            Object result = person.invokeMethod("isAdult");
            
            assertTrue((Boolean) result);
        }
        
        @Test
        @DisplayName("Invoke method with list args")
        public void testInvokeMethodWithList() {
            List<Object> args = Collections.emptyList();
            Object result = person.invokeMethod("getAge", args);
            
            assertEquals(25, result);
        }
        
        @Test
        @DisplayName("Throw when method not found")
        public void testThrowWhenMethodNotFound() {
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                person.invokeMethod("nonExistent");
            });
            
            assertTrue(exception.getMessage().contains("not found"));
        }
        
        @Test
        @DisplayName("Throw when no interpreter available")
        public void testThrowWhenNoInterpreter() {
            RuntimeObject.setCurrentInterpreter(null);
            
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                person.invokeMethod("getName");
            });
            
            assertTrue(exception.getMessage().contains("No interpreter"));
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Full workflow: register class, methods, create instance, invoke")
        public void testFullWorkflow() {
            RuntimeObject.setCurrentInterpreter(interpreter.getInterpreter());
            
            ScriptClass calculator = env.registerClass("Calculator")
                .registerStaticField("PI", 3.14159)
                .registerStaticMethod("add", args -> (double)args[0] + (double)args[1])
                .registerStaticMethod("subtract", args -> (double)args[0] - (double)args[1])
                .registerField("value", 0.0)
                .registerMethod("getValue", args -> {
                    RuntimeObject self = (RuntimeObject) args[0];
                    return self.getField("value");
                })
                .registerMethod("setValue", args -> {
                    RuntimeObject self = (RuntimeObject) args[0];
                    self.setField("value", args[1]);
                    return null;
                });
            
            Object sum = calculator.invokeStaticMethod("add", 10.5, 5.5);
            assertEquals(16.0, sum);
            
            Object diff = calculator.invokeStaticMethod("subtract", 20.0, 8.0);
            assertEquals(12.0, diff);
            
            RuntimeObject instance = new RuntimeObject(calculator);
            instance.invokeMethod("setValue", 42.0);
            Object value = instance.invokeMethod("getValue");
            assertEquals(42.0, value);
        }
        
        @Test
        @DisplayName("Inner class with methods")
        public void testInnerClassWithMethods() {
            RuntimeObject.setCurrentInterpreter(interpreter.getInterpreter());
            
            ScriptClass outer = env.registerClass("Outer")
                .registerStaticMethod("outerMethod", args -> "from outer");
            
            ScriptClass inner = outer.registerInnerClass("Inner", Modifier.PUBLIC | Modifier.STATIC)
                .registerStaticMethod("innerMethod", args -> "from inner");
            
            assertEquals("from outer", outer.invokeStaticMethod("outerMethod"));
            assertEquals("from inner", inner.invokeStaticMethod("innerMethod"));
        }
        
        @Test
        @DisplayName("Execute script with registered class")
        public void testExecuteScriptWithRegisteredClass() {
            env.registerClass("MyMath")
                .registerStaticMethod("square", args -> {
                    int x = ((Number)args[0]).intValue();
                    return x * x;
                });
            
            Object result = interpreter.execute("System.out.println(MyMath.square(5));");
            
            assertNull(result);
        }
    }
}
