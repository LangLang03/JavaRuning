# Javanter

[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

纯 Java 编写的 Java 解释器，完整支持 Java 17 语法，包括记录类、密封类、模式匹配等现代特性。

## 功能特性

- **Java 17 语法支持**: 完整支持记录类、密封类、模式匹配、switch 表达式、文本块、var 关键字等现代 Java 特性
- **Java 8+ 特性**: Lambda 表达式 (`x -> x * 2`) 和方法引用 (`String::compareTo`)
- **完整泛型系统**: 类型擦除、桥接方法生成、类型推断
- **注解处理**: 内置 Lombok 风格注解处理器 (`@Data` 自动生成 getter/setter)
- **静态分析**: 执行前检查变量未定义、静态上下文错误等
- **Android 支持**: 可在 Android 运行环境运行，支持 DEX 文件扫描
- **线程安全**: 基于 ThreadLocal 的执行环境隔离

## 快速开始

```java
// 创建解释器
JavaInterpreter interpreter = new JavaInterpreter();

// 执行代码
interpreter.execute("int sum = 0; for(int i = 1; i <= 100; i++) { sum += i; } System.out.println(sum);");

// Lambda 支持
interpreter.execute("List<Integer> nums = Arrays.asList(1, 2, 3); nums.forEach(x -> System.out.println(x));");
```

## 与 BeanShell 对比

| 功能 | Javanter | BeanShell |
|------|----------|-----------|
| Java 版本 | 17 | 7 |
| Lambda `->` | 支持 | 不支持 |
| 方法引用 `::` | 支持 | 不支持 |
| `var` 关键字 | 支持 | 不支持 |
| 记录类 (Record) | 支持 | 不支持 |
| 密封类 (Sealed) | 支持 | 不支持 |
| Switch 表达式 | 支持 | 不支持 |
| 文本块 | 支持 | 不支持 |
| 模式匹配 | 支持 | 不支持 |
| 私有接口方法 | 支持 | 不支持 |
| 泛型 | 完整支持 | 部分支持 |
| 注解处理 | 内置 | 非内置 |
| 静态分析 | 支持 | 不支持 |

## 核心架构

```
Javanter
  ├── lexer/          词法分析，Token 生成
  ├── parser/         递归下降解析器，Pratt 解析器
  ├── ast/            完整的 AST 节点定义
  ├── interpreter/     核心执行引擎
  │     ├── evaluator/   表达式求值
  │     ├── executor/    语句和声明执行
  │     ├── exception/   控制流异常
  │     ├── AccessController.java  访问控制检查
  │     └── TypeConverter.java     类型转换工具
  ├── runtime/
  │     ├── model/       运行时对象、ScriptClass、ScriptMethod
  │     ├── environment/  作用域链环境
  │     ├── nativesupport/ Java 标准库桥接
  │     │     ├── MethodBuilder.java    方法注册构建器
  │     │     └── ReflectionInvoker.java 反射调用工具
  │     ├── TypeConstants.java    类型常量定义
  │     ├── ExceptionConstants.java 异常常量定义
  │     └── NumericUtils.java     数值运算工具
  ├── analyzer/       静态代码分析
  └── annotation/     注解处理框架
```

### Lexer（词法分析器）

将源代码字符串转换为 Token 序列，支持：
- 关键字、标识符、字面量
- 字符串转义序列
- 数值字面量（二进制、十六进制、十进制、科学计数法）
- 单行和多行注释

### Parser（语法解析器）

递归下降解析器实现：
- `DeclarationParser`: 类、接口、枚举、方法、字段
- `ExpressionParser`: Pratt 解析器处理运算符优先级
- `StatementParser`: 控制流语句
- `TypeParser`: 类型解析，包括泛型

### Interpreter（解释执行引擎）

核心执行引擎，协调所有组件：
- 方法调用和分派
- 类初始化
- 运行时环境管理
- Android 运行环境检测和 DEX 文件扫描

### Runtime Model（运行时模型）

- `RuntimeObject`: 所有运行时对象的基类
- `ScriptClass`: 类的元信息（字段、方法、构造器）
- `ScriptMethod`: 方法元信息，包含方法体
- `ScriptField`: 字段元信息
- `LambdaObject`: 闭包，捕获定义时的环境

### Expression Evaluator（表达式求值器）

访问者模式实现的表达式求值：
- 二元/一元运算符
- 方法调用
- Lambda 表达式
- 方法引用
- 字段访问

### Statement Executor（语句执行器）

处理所有语句类型：
- 控制流: if/while/for/do/switch
- 异常处理: try/catch/finally
- 跳转语句: break/continue/return

## 构建

```bash
./gradlew build
```

## 运行

```bash
java -cp target/javanter-1.0.jar cn.langlang.javainterpreter.Main [选项]
```

### 选项

- `-main <类名>`: 指定主类名
- `-lint <文件>`: 仅静态分析
- `-exec <文件>`: 执行脚本文件
- `-cp <路径>`: 额外的类路径

### 编程使用

```java
// 静态分析
JavaInterpreter interpreter = new JavaInterpreter();
StaticAnalyzer.AnalysisResult result = interpreter.lint(source, fileName);
if (result.hasErrors()) {
    result.printReport();
}

// 加载并执行
interpreter.load(source, fileName);
Object result = interpreter.runMain();

// 直接执行
Object result = interpreter.execute("System.out.println(\"Hello World\");");
```

### API 示例

```java
// 创建解释器
JavaInterpreter interpreter = new JavaInterpreter();

// 定义一个类
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

// Lambda 示例
interpreter.execute("List<String> names = Arrays.asList(\"Alice\", \"Bob\");");
interpreter.execute("names.stream().map(x -> x.toUpperCase()).forEach(System.out::println);");
```

## 项目结构

```
src/main/java/cn/langlang/javainterpreter/
├── Main.java              # CLI 入口
├── api/
│   └── JavaInterpreter.java   # 公共 API
├── lexer/
│   ├── Lexer.java         # Token 扫描器
│   ├── Token.java         # Token 表示
│   └── TokenType.java     # Token 类型枚举
├── parser/
│   ├── Parser.java        # 主解析器
│   ├── DeclarationParser.java
│   ├── ExpressionParser.java
│   ├── StatementParser.java
│   ├── TypeParser.java
│   └── TokenReader.java
├── ast/
│   ├── base/              # AST 基类
│   ├── declaration/       # 声明节点
│   ├── expression/        # 表达式节点
│   ├── statement/         # 语句节点
│   ├── misc/             # 杂项节点
│   └── type/             # 类型节点
├── interpreter/
│   ├── Interpreter.java   # 核心引擎
│   ├── AccessController.java  # 访问控制
│   ├── TypeConverter.java     # 类型转换
│   ├── evaluator/         # 表达式求值
│   ├── executor/          # 语句/声明执行
│   └── exception/         # 控制流异常
├── runtime/
│   ├── environment/       # 作用域环境
│   ├── model/            # 运行时对象
│   ├── nativesupport/     # Java 标准库桥接
│   │   ├── MethodBuilder.java    # 方法构建器
│   │   └── ReflectionInvoker.java # 反射调用
│   ├── TypeConstants.java    # 类型常量
│   ├── ExceptionConstants.java # 异常常量
│   └── NumericUtils.java     # 数值运算
├── analyzer/
│   └── StaticAnalyzer.java
└── annotation/
    └── DataAnnotationProcessor.java
```

## 支持的 Java 特性

| 类别 | 特性 |
|------|------|
| 类型 | 类、接口、枚举、注解、泛型、记录类、密封类 |
| 成员 | 字段、方法、构造器、静态初始化块 |
| 控制流 | if/else、while、for、do/while、switch、switch 表达式 |
| 异常 | try/catch/finally、throw、multi-catch |
| 表达式 | Lambda、方法引用、三元、赋值、模式匹配 |
| 修饰符 | public、private、protected、static、final、synchronized、sealed、non-sealed |
| 高级 | 匿名类、局部类、静态导入、文本块 |
| Java 10+ | var 关键字用于局部变量类型推断 |
| Java 14+ | Switch 表达式（箭头语法）、yield 语句 |
| Java 15+ | 文本块（多行字符串） |
| Java 16+ | 记录类、instanceof 模式匹配 |
| Java 17+ | 密封类和接口 |

## 已知限制

- 不支持 native 方法声明
- 部分反射功能受限
- 不支持显式锁（ReentrantLock 等）

## 许可证

Apache License 2.0
