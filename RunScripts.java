import cn.langlang.javainterpreter.Main;
import cn.langlang.javainterpreter.api.JavaInterpreter;
import java.nio.file.*;

public class RunScripts {
    public static void main(String[] args) throws Exception {
        JavaInterpreter interpreter = new JavaInterpreter();
        interpreter.enableLombokStyleAnnotations();

        String helperSource = new String(Files.readAllBytes(Paths.get("Helper.java")));
        String mainSource = new String(Files.readAllBytes(Paths.get("MainScript.java")));

        System.out.println("=== Loading Helper ===");
        interpreter.execute(helperSource);

        System.out.println("=== Loading MainScript ===");
        interpreter.execute(mainSource);

        System.out.println("=== Done ===");
    }
}
