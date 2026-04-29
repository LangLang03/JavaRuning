# Javanter

纯 Java 编写的 Java 解释器，支持 Java 8 语法，包括 Lambda 表达式和方法引用。

## 功能特性

- **Java 8 语法支持**: 完整支持 Lambda 表达式 (`x -> x * 2`) 和方法引用 (`String::compareTo`)
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
| Lambda `->` | 支持 | 不支持 |
| 方法引用 `::` | 支持 | 不支持 |
| 泛型 | 完整支持 | 部分支持 |
| 注解处理 | 内置 | 非内置 |
| 静态分析 | 支持 | 不支持 |
| Java 版本 | 8+ | 7 |

## 构建

```bash
mvn clean package
```

## 运行

```bash
java -cp target/javanter-1.0.jar cn.langlang.javainterpreter.Main [选项]
```

选项:
- `-main <类名>`: 指定主类名
- `-lint <文件>`: 仅静态分析
- `-exec <文件>`: 执行脚本文件
- `-cp <路径>`: 额外的类路径

## 项目结构

```
src/main/java/cn/langlang/javainterpreter/
├── lexer/          # 词法分析
├── parser/         # 语法解析
├── ast/            # 抽象语法树
├── interpreter/    # 执行引擎
├── runtime/        # 运行时支持
├── analyzer/       # 静态分析
└── annotation/      # 注解处理
```

## 许可证

Apache License 2.0
