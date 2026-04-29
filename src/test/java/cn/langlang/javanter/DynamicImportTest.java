package cn.langlang.javanter;

import cn.langlang.javanter.api.JavaInterpreter;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class DynamicImportTest {
    
    private JavaInterpreter interpreter;
    
    @BeforeEach
    public void setUp() {
        interpreter = new JavaInterpreter();
    }
    
    @Test
    public void testImportLinkedList() {
        String source = "import java.util.LinkedList; " +
            "public class Test { public static void main(String[] args) { " +
            "LinkedList<String> list = new LinkedList<>(); list.add(\"a\"); System.out.println(list.get(0)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportTreeMap() {
        String source = "import java.util.TreeMap; " +
            "public class Test { public static void main(String[] args) { " +
            "TreeMap<String, Integer> map = new TreeMap<>(); map.put(\"a\", 1); System.out.println(map.get(\"a\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportTreeSet() {
        String source = "import java.util.TreeSet; " +
            "public class Test { public static void main(String[] args) { " +
            "TreeSet<Integer> set = new TreeSet<>(); set.add(5); set.add(3); System.out.println(set.first()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportLinkedHashMap() {
        String source = "import java.util.LinkedHashMap; " +
            "public class Test { public static void main(String[] args) { " +
            "LinkedHashMap<String, String> map = new LinkedHashMap<>(); map.put(\"key\", \"value\"); System.out.println(map.get(\"key\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportVector() {
        String source = "import java.util.Vector; " +
            "public class Test { public static void main(String[] args) { " +
            "Vector<String> v = new Vector<>(); v.add(\"test\"); System.out.println(v.get(0)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportStack() {
        String source = "import java.util.Stack; " +
            "public class Test { public static void main(String[] args) { " +
            "Stack<Integer> stack = new Stack<>(); stack.push(1); stack.push(2); System.out.println(stack.pop()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportPriorityQueue() {
        String source = "import java.util.PriorityQueue; " +
            "public class Test { public static void main(String[] args) { " +
            "PriorityQueue<Integer> pq = new PriorityQueue<>(); pq.add(5); pq.add(1); System.out.println(pq.peek()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportArrayDeque() {
        String source = "import java.util.ArrayDeque; " +
            "public class Test { public static void main(String[] args) { " +
            "ArrayDeque<String> deque = new ArrayDeque<>(); deque.add(\"a\"); System.out.println(deque.peek()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportBitSet() {
        String source = "import java.util.BitSet; " +
            "public class Test { public static void main(String[] args) { " +
            "BitSet bs = new BitSet(); bs.set(0); bs.set(5); System.out.println(bs.get(0)); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportDate() {
        String source = "import java.util.Date; " +
            "public class Test { public static void main(String[] args) { " +
            "Date d = new Date(); System.out.println(d != null); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportSimpleDateFormat() {
        String source = "import java.text.SimpleDateFormat; import java.util.Date; " +
            "public class Test { public static void main(String[] args) { " +
            "SimpleDateFormat sdf = new SimpleDateFormat(\"yyyy-MM-dd\"); Date d = new Date(); System.out.println(sdf.format(d) != null); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportRandom() {
        String source = "import java.util.Random; " +
            "public class Test { public static void main(String[] args) { " +
            "Random r = new Random(); int n = r.nextInt(100); System.out.println(n >= 0 && n < 100); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportScanner() {
        String source = "import java.util.Scanner; " +
            "public class Test { public static void main(String[] args) { " +
            "Scanner s = new Scanner(\"hello world\"); System.out.println(s.hasNext()); s.close(); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportFile() {
        String source = "import java.io.File; " +
            "public class Test { public static void main(String[] args) { " +
            "File f = new File(\"/tmp\"); System.out.println(f.exists()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportFileReader() {
        String source = "import java.io.FileReader; import java.io.File; " +
            "public class Test { public static void main(String[] args) { " +
            "File f = new File(\"/etc/hostname\"); if (f.exists()) { FileReader fr = new FileReader(f); fr.close(); } System.out.println(true); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportBufferedReader() {
        String source = "import java.io.BufferedReader; import java.io.FileReader; import java.io.File; " +
            "public class Test { public static void main(String[] args) { " +
            "File f = new File(\"/etc/hostname\"); if (f.exists()) { BufferedReader br = new BufferedReader(new FileReader(f)); br.close(); } System.out.println(true); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportFileWriter() {
        String source = "import java.io.FileWriter; import java.io.File; " +
            "public class Test { public static void main(String[] args) { " +
            "File f = new File(\"/tmp/test.txt\"); FileWriter fw = new FileWriter(f); fw.write(\"test\"); fw.close(); System.out.println(true); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportBufferedWriter() {
        String source = "import java.io.BufferedWriter; import java.io.FileWriter; import java.io.File; " +
            "public class Test { public static void main(String[] args) { " +
            "File f = new File(\"/tmp/test.txt\"); BufferedWriter bw = new BufferedWriter(new FileWriter(f)); bw.write(\"test\"); bw.close(); System.out.println(true); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportPrintWriter() {
        String source = "import java.io.PrintWriter; import java.io.File; " +
            "public class Test { public static void main(String[] args) { " +
            "File f = new File(\"/tmp/test.txt\"); PrintWriter pw = new PrintWriter(f); pw.println(\"test\"); pw.close(); System.out.println(true); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportByteArrayInputStream() {
        String source = "import java.io.ByteArrayInputStream; " +
            "public class Test { public static void main(String[] args) { " +
            "byte[] data = \"hello\".getBytes(); ByteArrayInputStream bis = new ByteArrayInputStream(data); System.out.println(bis.available() > 0); bis.close(); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportByteArrayOutputStream() {
        String source = "import java.io.ByteArrayOutputStream; " +
            "public class Test { public static void main(String[] args) { " +
            "ByteArrayOutputStream bos = new ByteArrayOutputStream(); bos.write(\"test\".getBytes()); System.out.println(bos.size() > 0); bos.close(); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportStringWriter() {
        String source = "import java.io.StringWriter; " +
            "public class Test { public static void main(String[] args) { " +
            "StringWriter sw = new StringWriter(); sw.write(\"hello\"); System.out.println(sw.toString().equals(\"hello\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportStringReader() {
        String source = "import java.io.StringReader; " +
            "public class Test { public static void main(String[] args) { " +
            "StringReader sr = new StringReader(\"hello\"); System.out.println(sr.read() == 'h'); sr.close(); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportInetAddress() {
        String source = "import java.net.InetAddress; " +
            "public class Test { public static void main(String[] args) { " +
            "InetAddress local = InetAddress.getLocalHost(); System.out.println(local != null); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportURL() {
        String source = "import java.net.URL; " +
            "public class Test { public static void main(String[] args) { " +
            "URL url = new URL(\"http://example.com\"); System.out.println(url.getProtocol().equals(\"http\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportURI() {
        String source = "import java.net.URI; " +
            "public class Test { public static void main(String[] args) { " +
            "URI uri = new URI(\"http://example.com\"); System.out.println(uri.getScheme().equals(\"http\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportUUID() {
        String source = "import java.util.UUID; " +
            "public class Test { public static void main(String[] args) { " +
            "UUID uuid = UUID.randomUUID(); System.out.println(uuid != null); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportPattern() {
        String source = "import java.util.regex.Pattern; " +
            "public class Test { public static void main(String[] args) { " +
            "Pattern p = Pattern.compile(\"\\\\d+\"); System.out.println(p.matcher(\"123\").matches()); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportMatcher() {
        String source = "import java.util.regex.Pattern; import java.util.regex.Matcher; " +
            "public class Test { public static void main(String[] args) { " +
            "Pattern p = Pattern.compile(\"(\\\\d+)\"); Matcher m = p.matcher(\"abc123\"); " +
            "if (m.find()) { System.out.println(m.group(1).equals(\"123\")); } else { System.out.println(false); } } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportBase64() {
        String source = "import java.util.Base64; " +
            "public class Test { public static void main(String[] args) { " +
            "String encoded = Base64.getEncoder().encodeToString(\"hello\".getBytes()); " +
            "byte[] decoded = Base64.getDecoder().decode(encoded); " +
            "System.out.println(new String(decoded).equals(\"hello\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportObjects() {
        String source = "import java.util.Objects; " +
            "public class Test { public static void main(String[] args) { " +
            "String s = null; System.out.println(Objects.hash(\"a\", \"b\") != 0); " +
            "System.out.println(Objects.equals(\"a\", \"a\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportArrays() {
        String source = "import java.util.Arrays; " +
            "public class Test { public static void main(String[] args) { " +
            "int[] arr = {3, 1, 2}; Arrays.sort(arr); System.out.println(arr[0] == 1); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportCollections() {
        String source = "import java.util.Collections; import java.util.ArrayList; import java.util.List; " +
            "public class Test { public static void main(String[] args) { " +
            "List<Integer> list = new ArrayList<>(); list.add(1); list.add(2); " +
            "Collections.reverse(list); System.out.println(list.get(0) == 2); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportOptional() {
        String source = "import java.util.Optional; " +
            "public class Test { public static void main(String[] args) { " +
            "Optional<String> opt = Optional.of(\"hello\"); " +
            "System.out.println(opt.isPresent()); " +
            "System.out.println(opt.get().equals(\"hello\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportOptionalEmpty() {
        String source = "import java.util.Optional; " +
            "public class Test { public static void main(String[] args) { " +
            "Optional<String> opt = Optional.empty(); " +
            "System.out.println(opt.isPresent() == false); " +
            "System.out.println(opt.orElse(\"default\").equals(\"default\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportStream() {
        String source = "import java.util.stream.Stream; import java.util.List; import java.util.Arrays; " +
            "public class Test { public static void main(String[] args) { " +
            "Stream<Integer> s = Stream.of(1, 2, 3); " +
            "long count = s.count(); System.out.println(count == 3); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportCollectors() {
        String source = "import java.util.stream.Collectors; import java.util.stream.Stream; import java.util.List; " +
            "public class Test { public static void main(String[] args) { " +
            "Stream<Integer> s = Stream.of(1, 2, 3); " +
            "List<Integer> list = s.collect(Collectors.toList()); " +
            "System.out.println(list.size() == 3); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportStringBuilder() {
        String source = "import java.lang.StringBuilder; " +
            "public class Test { public static void main(String[] args) { " +
            "StringBuilder sb = new StringBuilder(); sb.append(\"hello\"); sb.append(\" \"); sb.append(\"world\"); " +
            "System.out.println(sb.toString().equals(\"hello world\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportStringBuffer() {
        String source = "import java.lang.StringBuffer; " +
            "public class Test { public static void main(String[] args) { " +
            "StringBuffer sb = new StringBuffer(); sb.append(\"hello\"); " +
            "System.out.println(sb.toString().equals(\"hello\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportThread() {
        String source = "import java.lang.Thread; " +
            "public class Test { public static void main(String[] args) { " +
            "Thread t = new Thread(() -> System.out.println(\"thread\")); t.start(); t.join(100); System.out.println(true); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportThreadLocal() {
        String source = "import java.lang.ThreadLocal; " +
            "public class Test { public static void main(String[] args) { " +
            "ThreadLocal<String> tl = ThreadLocal.withInitial(() -> \"initial\"); " +
            "System.out.println(tl.get().equals(\"initial\")); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportMathClass() {
        String source = "import java.lang.Math; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(Math.PI > 3.0); System.out.println(Math.sqrt(4.0) == 2.0); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportStrictMath() {
        String source = "import java.lang.StrictMath; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(StrictMath.sqrt(4.0) == 2.0); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportSystemClass() {
        String source = "import java.lang.System; " +
            "public class Test { public static void main(String[] args) { " +
            "System.out.println(System.currentTimeMillis() > 0); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportRuntime() {
        String source = "import java.lang.Runtime; " +
            "public class Test { public static void main(String[] args) { " +
            "Runtime r = Runtime.getRuntime(); System.out.println(r.availableProcessors() > 0); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
    
    @Test
    public void testImportProcessBuilder() {
        String source = "import java.lang.ProcessBuilder; " +
            "public class Test { public static void main(String[] args) throws Exception { " +
            "ProcessBuilder pb = new ProcessBuilder(\"echo\", \"hello\"); " +
            "Process p = pb.start(); p.waitFor(); System.out.println(true); } }";
        assertDoesNotThrow(() -> interpreter.execute(source));
    }
}
