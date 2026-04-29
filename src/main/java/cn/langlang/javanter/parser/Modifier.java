package cn.langlang.javanter.parser;

public class Modifier {
    public static final int PUBLIC = 1;
    public static final int PRIVATE = 1 << 1;
    public static final int PROTECTED = 1 << 2;
    public static final int STATIC = 1 << 3;
    public static final int FINAL = 1 << 4;
    public static final int SYNCHRONIZED = 1 << 5;
    public static final int VOLATILE = 1 << 6;
    public static final int TRANSIENT = 1 << 7;
    public static final int NATIVE = 1 << 8;
    public static final int ABSTRACT = 1 << 9;
    public static final int STRICTFP = 1 << 10;
    public static final int DEFAULT = 1 << 11;
    public static final int INTERFACE = 1 << 12;
    public static final int SYNTHETIC = 1 << 13;
    public static final int BRIDGE = 1 << 14;
    public static final int ENUM = 1 << 15;
    
    public static String toString(int modifiers) {
        StringBuilder sb = new StringBuilder();
        if ((modifiers & PUBLIC) != 0) sb.append("public ");
        if ((modifiers & PRIVATE) != 0) sb.append("private ");
        if ((modifiers & PROTECTED) != 0) sb.append("protected ");
        if ((modifiers & STATIC) != 0) sb.append("static ");
        if ((modifiers & FINAL) != 0) sb.append("final ");
        if ((modifiers & SYNCHRONIZED) != 0) sb.append("synchronized ");
        if ((modifiers & VOLATILE) != 0) sb.append("volatile ");
        if ((modifiers & TRANSIENT) != 0) sb.append("transient ");
        if ((modifiers & NATIVE) != 0) sb.append("native ");
        if ((modifiers & ABSTRACT) != 0) sb.append("abstract ");
        if ((modifiers & STRICTFP) != 0) sb.append("strictfp ");
        if ((modifiers & DEFAULT) != 0) sb.append("default ");
        if ((modifiers & INTERFACE) != 0) sb.append("interface ");
        if ((modifiers & ENUM) != 0) sb.append("enum ");
        return sb.toString().trim();
    }
}
