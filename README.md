# Javanter

[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

A Java interpreter written in pure Java, supporting Java 17 syntax including records, sealed classes, pattern matching, and more.

## Features

- **Java 17 Syntax Support**: Full support for modern Java features including records, sealed classes, pattern matching, switch expressions, text blocks, and var keyword
- **Java 8+ Features**: Lambda expressions (`x -> x * 2`) and method references (`String::compareTo`)
- **Complete Generics System**: Type erasure, bridge method generation, type inference
- **Annotation Processing**: Built-in Lombok-style annotation processor (`@Data` auto-generates getters/setters)
- **Static Analysis**: Pre-execution checks for undefined variables, static context errors, etc.
- **Android Support**: Can run on Android runtime with DEX file scanning
- **Thread Safety**: ThreadLocal-based execution environment isolation

## Quick Start

```java
// Create interpreter
JavaInterpreter interpreter = new JavaInterpreter();

// Execute code
interpreter.execute("int sum = 0; for(int i = 1; i <= 100; i++) { sum += i; } System.out.println(sum);");

// Lambda support
interpreter.execute("List<Integer> nums = Arrays.asList(1, 2, 3); nums.forEach(x -> System.out.println(x));");
```

## Comparison with BeanShell

| Feature | Javanter | BeanShell |
|---------|----------|-----------|
| Java Version | 17 | 7 |
| Lambda `->` | Supported | Not supported |
| Method reference `::` | Supported | Not supported |
| `var` keyword | Supported | Not supported |
| Records | Supported | Not supported |
| Sealed classes | Supported | Not supported |
| Switch expressions | Supported | Not supported |
| Text blocks | Supported | Not supported |
| Pattern matching | Supported | Not supported |
| Private interface methods | Supported | Not supported |
| Generics | Full support | Partial |
| Annotation processing | Built-in | Not built-in |
| Static analysis | Yes | No |

## Core Architecture

```
Javanter
  ├── lexer/          Lexical analysis, token generation
  ├── parser/         Recursive descent parser, Pratt parser
  ├── ast/            Complete AST node definitions
  ├── interpreter/     Core execution engine
  │     ├── evaluator/   Expression evaluation
  │     ├── executor/    Statement and declaration execution
  │     ├── exception/   Control flow exceptions
  │     ├── AccessController.java  Access control checking
  │     └── TypeConverter.java     Type conversion utilities
  ├── runtime/
  │     ├── model/       Runtime objects, ScriptClass, ScriptMethod
  │     ├── environment/  Scoped environment chain
  │     ├── nativesupport/ Standard library bridge
  │     │     ├── MethodBuilder.java    Method registration builder
  │     │     └── ReflectionInvoker.java Reflection invocation utilities
  │     ├── TypeConstants.java    Type constant definitions
  │     ├── ExceptionConstants.java Exception constant definitions
  │     └── NumericUtils.java     Numeric operation utilities
  ├── analyzer/       Static code analysis
  └── annotation/     Annotation processing framework
```

### Lexer

Converts source code strings into token sequences. Supports:
- Keywords, identifiers, literals
- String escaping sequences
- Numeric literals (binary, hex, decimal, scientific notation)
- Single-line and multi-line comments

### Parser

Recursive descent parser implementation:
- `DeclarationParser`: Classes, interfaces, enums, methods, fields
- `ExpressionParser`: Pratt parser for operator precedence
- `StatementParser`: Control flow statements
- `TypeParser`: Type parsing including generics

### Interpreter

Core execution engine coordinating all components:
- Method invocation and dispatch
- Class initialization
- Runtime environment management
- Android runtime detection and DEX file scanning

### Runtime Model

- `RuntimeObject`: Base for all runtime objects
- `ScriptClass`: Metadata for classes (fields, methods, constructors)
- `ScriptMethod`: Method metadata with body
- `ScriptField`: Field metadata
- `LambdaObject`: Closure with captured environment

### Expression Evaluator

Visitor pattern implementation for expression evaluation:
- Binary/unary operators
- Method invocations
- Lambda expressions
- Method references
- Field access

### Statement Executor

Handles all statement types:
- Control flow: if/while/for/do/switch
- Exception handling: try/catch/finally
- Jump statements: break/continue/return

## Building

```bash
./gradlew build
```

## Running

```bash
java -cp target/javanter-1.0.jar cn.langlang.javainterpreter.Main [options]
```

### Options

- `-main <class>`: Specify main class name
- `-lint <file>`: Static analysis only
- `-exec <file>`: Execute script file
- `-cp <path>`: Additional classpath

### Programmatic Usage

```java
// Static analysis
JavaInterpreter interpreter = new JavaInterpreter();
StaticAnalyzer.AnalysisResult result = interpreter.lint(source, fileName);
if (result.hasErrors()) {
    result.printReport();
}

// Load and execute
interpreter.load(source, fileName);
Object result = interpreter.runMain();

// Direct execution
Object result = interpreter.execute("System.out.println(\"Hello World\");");
```

### API Example

```java
// Create interpreter
JavaInterpreter interpreter = new JavaInterpreter();

// Define a class
String code = `
public class Hello {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        System.out.println("Javanter works!");
    }
}
`;
interpreter.load(code, "Hello.java");
interpreter.runMain();

// Lambda example
interpreter.execute("List<String> names = Arrays.asList(\"Alice\", \"Bob\");");
interpreter.execute("names.stream().map(x -> x.toUpperCase()).forEach(System.out::println);");
```

## Project Structure

```
src/main/java/cn/langlang/javainterpreter/
├── Main.java              # CLI entry point
├── api/
│   └── JavaInterpreter.java   # Public API
├── lexer/
│   ├── Lexer.java         # Token scanner
│   ├── Token.java         # Token representation
│   └── TokenType.java     # Token type enum
├── parser/
│   ├── Parser.java        # Main parser
│   ├── DeclarationParser.java
│   ├── ExpressionParser.java
│   ├── StatementParser.java
│   ├── TypeParser.java
│   └── TokenReader.java
├── ast/
│   ├── base/              # Base AST classes
│   ├── declaration/       # Declaration nodes
│   ├── expression/        # Expression nodes
│   ├── statement/         # Statement nodes
│   ├── misc/              # Misc nodes
│   └── type/              # Type nodes
├── interpreter/
│   ├── Interpreter.java   # Core engine
│   ├── AccessController.java  # Access control
│   ├── TypeConverter.java     # Type conversion
│   ├── evaluator/         # Expression evaluation
│   ├── executor/          # Statement/declaration execution
│   └── exception/         # Control flow exceptions
├── runtime/
│   ├── environment/       # Scoped environments
│   ├── model/             # Runtime objects
│   ├── nativesupport/     # Java stdlib bridge
│   │   ├── MethodBuilder.java    # Method builder
│   │   └── ReflectionInvoker.java # Reflection invocation
│   ├── TypeConstants.java    # Type constants
│   ├── ExceptionConstants.java # Exception constants
│   └── NumericUtils.java     # Numeric operations
├── analyzer/
│   └── StaticAnalyzer.java
└── annotation/
    └── DataAnnotationProcessor.java
```

## Supported Java Features

| Category | Features |
|----------|----------|
| Types | Classes, interfaces, enums, annotations, generics, records, sealed classes |
| Members | Fields, methods, constructors, static initializers |
| Control | if/else, while, for, do/while, switch, switch expressions |
| Exceptions | try/catch/finally, throw, multi-catch |
| Expressions | Lambda, method reference, ternary, assignments, pattern matching |
| Modifiers | public, private, protected, static, final, synchronized, sealed, non-sealed |
| Advanced | Anonymous classes, local classes, static imports, text blocks |
| Java 10+ | var keyword for local variable type inference |
| Java 14+ | Switch expressions with arrow syntax, yield statement |
| Java 15+ | Text blocks (multi-line strings) |
| Java 16+ | Records, pattern matching for instanceof |
| Java 17+ | Sealed classes and interfaces |

## Limitations

- Native method declarations not supported
- Some reflection features limited
- Explicit locks (ReentrantLock) not supported

## License

Apache License 2.0 - see [LICENSE](LICENSE) for details.
