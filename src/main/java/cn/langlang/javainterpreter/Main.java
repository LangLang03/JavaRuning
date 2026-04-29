package cn.langlang.javainterpreter;

import cn.langlang.javainterpreter.api.JavaInterpreter;
import cn.langlang.javainterpreter.interpreter.exception.InterpreterException;
import cn.langlang.javainterpreter.runtime.model.ScriptClass;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    private static final String VERSION = "1.0";
    private static final String USAGE =
        "Usage: java Main [options] <file.java>...\n" +
        "Options:\n" +
        "  -cp <path>    Additional classpath for imports\n" +
        "  -v, --version Show version\n" +
        "  -h, --help    Show this help\n" +
        "First file is treated as main if it contains main method.";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println(USAGE);
            System.exit(0);
        }

        List<String> files = new ArrayList<>();
        List<String> classpaths = new ArrayList<>();

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
                case "-cp":
                    if (i + 1 < args.length) {
                        classpaths.add(args[++i]);
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

        for (String file : files) {
            String source = new String(Files.readAllBytes(Paths.get(file)), "UTF-8").trim();
            String fileName = new File(file).getName();
            interpreter.load(source, fileName);
        }

        try {
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
