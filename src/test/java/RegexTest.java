import com.controlj.regexc.CCodeWriter;
import com.controlj.regexc.RegexMatcher;
import com.controlj.regexc.automata.DFA;
import com.controlj.regexc.automata.NFA;
import com.controlj.regexc.tree.SyntaxTree;
import com.controlj.regexc.tree.node.Node;
import com.controlj.regexc.util.Actions;
import com.controlj.regexc.util.InvalidSyntaxException;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Created on 5/6/15.
 */
public class RegexTest {

    private static final String HEADER =
            "#include    <stdio.h>\n" +
                    "#include    <stdlib.h>\n" +
                    "\n" +
                    "#define BUFLEN  16\n" +
                    "static unsigned char * ptr;\n" +
                    "static unsigned char buffer[BUFLEN];\n" +
                    "\n" +
                    "static char * data = \"\\n1234,*,3456,ABDE\\r\\n5,6,*,AB12\\r\";\n" +
                    "\n" +
                    "static test_action_t addChar(char c) {\n" +
                    "    test_state_t prev = test_state;\n" +
                    "    if(ptr >= buffer && ptr < buffer+BUFLEN)\n" +
                    "        *ptr++ = c;\n" +
                    "    test_action_t action = test_lex((unsigned char)c);\n" +
                    "    if(c <= ' ')\n" +
                    "        fprintf(stderr, \"state %d: %02x - > state %d, action %d\\n\", prev, c, test_state, action);\n" +
                    "    else\n" +
                    "        fprintf(stderr, \"state %d: '%c' - > state %d, action %d\\n\", prev, c, test_state, action);\n" +
                    "    return action;" +
                    "}\n" +
                    "\n" +
                    "\n" +
                    "int main(void) {\n" +
                    "    test_reset();\n" +
                    "    while(*data != 0)\n" +
                    "        switch(addChar(*data++)) {\n" +
                    "        case test_CONTINUE:\n" +
                    "            continue;\n" +
                    "        case test_FAIL:\n" +
                    "            fprintf(stderr, \"match failed at %s!\\n\", data);\n" +
                    "            exit(1);\n" +
                    "        case test_ACCEPT:\n" +
                    "            fprintf(stderr, \"match succeeded at %s - continuing\\n\", data);\n" +
                    "            test_reset();\n" +
                    "            continue;\n" +
                    "    }\n" +
                    "    exit(0);\n" +
                    "}";
    private static final String TEMPPATH = "src/test/temp";

    private static void runCmd(String cmd) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(cmd);
        InputStream stderr = process.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stderr));
        for (; ; ) {
            String s = reader.readLine();
            if (s == null)
                break;
            System.err.println(s);
        }

        process.waitFor();
        Assert.assertEquals(0, process.exitValue());

    }
    //"([ab]([^cd]*\\w+(abc|abcd){2,5})+)?.*"
    @Test
    public void testProcessing() {
        final long pre = System.currentTimeMillis();
        String regex = "\\n\uE000(('*' | digit+)\",\"\uE001)* hexdigit{4}\uE002\\r\uE003";
        Map<String, String> words = new HashMap<>();
        words.put("digit", "[0-9\\.]");
        words.put("hexdigit", "[A-F0-9]");
        SyntaxTree tree = new SyntaxTree(regex, words);
        Node root = tree.getRoot();
        System.out.println("For regex: " + regex);
        //System.out.println("Syntax tree: ");
        //TreePrinter.getInstance().printTree(root);
        NFA nfa = new NFA(root);
        System.out.println("NFA has " + nfa.getStateList().size() + " states");
        DFA dfa = new DFA(nfa.getStateList());
        System.out.println("DFA has " + dfa.getTransitionTable().length + " states");
        System.out.println("Cost " + (System.currentTimeMillis() - pre) + " ms to compile");
        System.out.println(dfa.toString());
        Actions actions = new Actions();
        actions.add("ptr = buffer;");
        actions.add("*ptr = 0; fprintf(stderr, \"got element %s\\n\", buffer); ptr = buffer;");
        actions.add("*ptr = 0; fprintf(stderr, \"got checksum %s\\n\", buffer);");
        actions.add("fprintf(stderr, \"action complete\\n\");");
        actions.addHeader(HEADER);
        actions.setPrefix("test");
        CCodeWriter codeWriter = new CCodeWriter(dfa, new File(TEMPPATH), actions);
        try {

            File aout = new File(TEMPPATH + "/a.out");
            if(aout.exists())
                //noinspection ResultOfMethodCallIgnored
                aout.delete();
            codeWriter.write();
            runCmd("clang -o " + TEMPPATH + "/a.out " + TEMPPATH + "/lex_test.c");
            runCmd(TEMPPATH + "/a.out");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("File error.");
        } catch (InterruptedException e) {
            e.printStackTrace();
            Assert.fail("Execution of state machine code failed");
        }
    }

    @Test
    public void testUUID() {
        String regex = "\\w{8}[-]\\w{4}[-]\\w{4}[-]\\w{4}[-]\\w{12}[;]";
        String str = UUID.randomUUID().toString() + ";";

        testFor(regex, str);
    }

    @Test
    public void testAddress() {
        String regex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3} ";
        String str = "192.168.0.255\\w*";

        testFor(regex, str);
    }

    @Test
    public void testHex() {
        String regex = "\\x61{4}[u]\\xAF{1,5}\\e\\cJ\\x00";
        testFor(regex, "aaaau\u00AF\u00AF\u001B\n\u0000");
        testFor(regex, "aaaau\u00AE\u00AF\u001B\n\u0000");
        try {
            new RegexMatcher("[a]{4");
            Assert.fail("Exception not thrown.");
        } catch (InvalidSyntaxException ignored) {

        }
    }

    @Test
    public void testReDoS() {
        String regex = "([a]*)*";
        String str1 = "aaaaaaaaaaaaaaaaab";
        testFor(regex, str1);

    }

    @Test
    public void testLog() {
        String regex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}' - - '\\[[^\\]]+\\]\\s\\\"[^\"]+\\\"\\s\\d+\\s\\d+\\s\\\"[^\"]+\\\"\\s\\\"[^\"]+\\\"";
        String str = "11.11.11.11 - - [25/Jan/2000:14:00:01 +0100] \"GET /1986.js HTTP/1.1\" 200 932 \"http://domain.com/index.html\" \"Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.9.1.7) Gecko/20091221 Firefox/3.5.7 GTB6\"";

        testFor(regex, str);
    }

    public void testFor(String regex, String str) {
        long prev;
        prev = System.currentTimeMillis();
        final boolean expected = Pattern.compile(regex.replaceAll("'", "")).matcher(str).matches();
        System.out.println(System.currentTimeMillis() - prev);
        prev = System.currentTimeMillis();
        boolean actual = new RegexMatcher(regex).match(str);
        System.out.format("Time: %dms\n", System.currentTimeMillis() - prev);
        Assert.assertEquals(expected, actual);
    }

}