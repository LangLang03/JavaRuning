package cn.langlang.javanter;

import cn.langlang.javanter.api.JavaInterpreter;
import cn.langlang.javanter.interpreter.exception.InterpreterException;
import cn.langlang.javanter.analyzer.StaticAnalyzer;
import cn.langlang.javanter.runtime.model.ScriptClass;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Main entry point for the JavaInterpreter command-line interface.
 * This class provides a CLI wrapper around the JavaInterpreter API,
 * allowing users to execute Java source code files directly from the command line.
 *
 * <p>Supported command-line options:</p>
 * <ul>
 *   <li>{@code -lint} - Perform static analysis only without execution</li>
 *   <li>{@code -cp <path>} - Additional classpath for imports</li>
 *   <li>{@code -main <class>} - Explicitly specify the main class name</li>
 *   <li>{@code -v, --version} - Display version information</li>
 *   <li>{@code -h, --help} - Show help message</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>
 *   java Main MyScript.java
 *   java Main -lint MyScript.java
 *   java Main -cp /path/to/libs MyScript.java
 * </pre>
 *
 * @author Javanter Development Team
 * @version 1.0
 */
public class Main {
    /** Current version of the JavaInterpreter */
    private static final String VERSION = "1.0";

    /**
     * Usage message displayed when the program is invoked without arguments
     * or with the -h/--help flag.
     */
    private static final String USAGE =
        "Usage: java Main [options] <file.java>...\n" +
        "Options:\n" +
        "  -lint         Static analysis only (no execution)\n" +
        "  -cp <path>    Additional classpath for imports\n" +
        "  -main <class> Specify main class name\n" +
        "  -v, --version Show version\n" +
        "  -h, --help    Show this help\n" +
        "Note: Static analysis is always performed before execution.\n" +
        "First file is treated as main if it contains main method.";

    /**
     * Main entry point for the JavaInterpreter CLI.
     *
     * <p>This method processes command-line arguments, loads and analyzes Java source files,
     * and executes the main method of the specified script. The execution flow is:</p>
     * <ol>
     *   <li>Parse command-line arguments</li>
     *   <li>Validate input files exist</li>
     *   <li>Create a JavaInterpreter instance</li>
     *   <li>Run static analysis on all source files</li>
     *   <li>If not lint-only mode, load and execute the code</li>
     *   <li>Invoke the main method and print the result</li>
     * </ol>
     *
     * @param args Command-line arguments controlling program behavior
     * @throws Exception if file I/O errors occur or execution fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println(USAGE);
            System.exit(0);
        }

        List<String> files = new ArrayList<>();
        List<String> classpaths = new ArrayList<>();
        String mainClass = null;
        boolean lintOnly = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h":
                case "--help":
                    System.out.println(USAGE);
                    System.exit(0);
                case "-v":
                case "--version":
                    System.out.println("JavaInterpreter " + VERSION);
                    System.exit(0);
                case "-lint":
                    lintOnly = true;
                    break;
                case "-cp":
                    if (i + 1 < args.length) {
                        classpaths.add(args[++i]);
                    }
                    break;
                case "-main":
                    if (i + 1 < args.length) {
                        mainClass = args[++i];
                    }
                    break;
                default:
                    if (!arg.startsWith("-")) {
                        files.add(arg);
                    }
                    break;
            }
        }

        if (files.isEmpty()) {
            System.err.println("Error: No script files specified");
            System.out.println(USAGE);
            System.exit(1);
        }

        for (String file : files) {
            Path path = Paths.get(file);
            if (!Files.exists(path)) {
                System.err.println("Error: File not found: " + file);
                System.exit(1);
            }
        }

        JavaInterpreter interpreter = new JavaInterpreter();
        interpreter.enableLombokStyleAnnotations();

        try {
            for (String file : files) {
                String source = new String(Files.readAllBytes(Paths.get(file)), "UTF-8").trim();
                String fileName = new File(file).getName();
                
                StaticAnalyzer.AnalysisResult result = interpreter.lint(source, fileName);
                if (result.hasErrors()) {
                    result.printReport();
                    System.exit(1);
                }
                if (result.hasWarnings()) {
                    result.printReport();
                }
                
                if (lintOnly) {
                    continue;
                }
                
                interpreter.load(source, fileName);
            }
            
            if (lintOnly) {
                System.out.println("Static analysis passed.");
                System.exit(0);
            }
            
            if (mainClass != null) {
                interpreter.setMainClassName(mainClass);
            }

            Object result = interpreter.runMain();
            if (result != null) {
                System.out.println("Result: " + result);
            }
        } catch (InterpreterException e) {
            System.err.println(e.getFullStackTrace());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
