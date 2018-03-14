package com.controlj.regexc.tree;

import com.controlj.regexc.stack.OperatingStack;
import com.controlj.regexc.stack.ShuntingStack;
import com.controlj.regexc.tree.node.*;
import com.controlj.regexc.tree.node.bracket.LeftBracket;
import com.controlj.regexc.tree.node.bracket.RightBracket;
import com.controlj.regexc.util.CommonSets;
import com.controlj.regexc.util.InvalidSyntaxException;

import java.util.*;

/**
 * Created on 2015/5/8.
 */
public class SyntaxTree {
    private String regex;
    private boolean itemTerminated;
    private List<Node> nodeList;
    private Stack<Node> nodeStack;

    private Node root;
    private Map<String, String> names;

    public SyntaxTree(String regex, Map<String, String> names) {
        this.names = names;
        root = null;
        this.regex = regex;
        nodeList = new ArrayList<>();
        itemTerminated = false;
        try {
            normalize();
        } catch (NoSuchElementException e) {
            throw new InvalidSyntaxException("Syntax error at end of regex");
        }
//        System.out.println(nodeList);
        shunt();
//        System.out.println(nodeStack);
        buildTree();
    }

    private void error(String msg, Queue<Character> queue) throws InvalidSyntaxException {
        throw new InvalidSyntaxException(msg + " at " + String.valueOf(queue));
    }

    private void buildTree() {
        OperatingStack operatingStack = new OperatingStack();
        while (!nodeStack.isEmpty()) {
            Node node = nodeStack.pop();
            node.accept(operatingStack);
        }
        try {
            root = operatingStack.pop();
        } catch (EmptyStackException e) {
            throw new InvalidSyntaxException(e);
        }
        if (!operatingStack.isEmpty()) {
            throw new InvalidSyntaxException("Operating stack not empty");
        }
    }

    private void shunt() {
        ShuntingStack shuntingStack = new ShuntingStack();
        for (Node node : nodeList) {
            node.accept(shuntingStack);
        }
        nodeStack = shuntingStack.finish();
    }

    private int getNumber(Queue<Character> queue) {
        StringBuilder sb = new StringBuilder();
        while (!queue.isEmpty() && Character.isDigit(queue.element())) {
            sb.append(queue.remove());
        }
        if (sb.length() != 0)
            return Integer.parseInt(sb.toString());
        return 0;
    }

    private void addTokenSet(List<Character> tokenSet) {
        if (tokenSet.size() == 1) {
            nodeList.add(new LChar(tokenSet.get(0)));
            return;
        }
        nodeList.add(new LeftBracket());
        nodeList.add(new LChar(tokenSet.get(0)));
        for (int i = 1; i != tokenSet.size(); i++) {
            nodeList.add(new BOr());
            nodeList.add(new LChar(tokenSet.get(i)));
        }
        nodeList.add(new RightBracket());
    }

    private void addString(char delimiter, Queue<Character> queue) {
        for (; ; ) {
            char c = queue.remove();
            if (c == delimiter)
                return;
            if (c == '\\') {
                c = queue.remove();
                switch (c) {
                    case 'n':
                        c = '\n';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    // octal constants are not implemented, as the usual syntax is dodgy
                    // hex constants - two digit version only implemented
                    case 'x':
                        String sb = new String(new char[]{queue.remove(), queue.remove()});
                        c = (char) Integer.parseInt(sb, 16);
                        break;

                    // control characters - implemented cheaply
                    case 'c':
                        c = (char) (queue.remove() & 0x1F);
                        break;

                    // escape character
                    case 'e':
                        c = '\u001B';
                        break;

                    default:
                        break;
                }
            }
            tryConcat();
            nodeList.add(new LChar(c));
            itemTerminated = true;
        }
    }

    // extract a word from the queue and push the replacement text onto the front of the queue.
    // TODO check for recursive replacement

    private void addWord(char c, LinkedList<Character> queue) {
        StringBuilder sb = new StringBuilder();
        sb.append(c);
        while (Character.isLetterOrDigit(queue.peek()))
            sb.append(queue.remove());
        String tok = names.get(sb.toString());
        if (tok == null)
            error("Unrecognised name " + sb.toString(), queue);
        // push the replacement onto the front of the queue
        ArrayList<Character> list = new ArrayList<>();
        for (char ch : tok.toCharArray())
            list.add(ch);
        queue.addAll(0, list);
    }

    private void normalize() {
        LinkedList<Character> queue = new LinkedList<>();
        for (char c : regex.toCharArray())
            queue.offer(c);
        while (!queue.isEmpty()) {
            char ch = queue.remove();
            // tags are encoded using the unicode private use area range E000-Efff
            if (ch >= '\uE000' && ch <= '\uEFFF') {
                tryConcat();
                int tag = ch - '\uE000';
                LChar closure = new LChar((char) (CommonSets.ENCODING_LENGTH + tag));
                nodeList.add(closure);
                itemTerminated = true;
                continue;
            }
            if (Character.isLetter(ch)) {
                addWord(ch, queue);
                continue;
            }
            switch (ch) {
                case '[': {
                    tryConcat();
                    List<Character> all = new ArrayList<>();
                    boolean isComplementarySet;
                    if (queue.element() == '^') {
                        isComplementarySet = true;
                        queue.remove();
                    } else {
                        isComplementarySet = false;
                    }
                    do {
                        ch = queue.remove();
                        if (ch == '\\')
                            all.addAll(CommonSets.interpretEscape(queue));
                        else if (ch == '.')
                            all.addAll(CommonSets.dotCollection());
                        else if (queue.element() == '-') {
                            queue.remove();
                            if (queue.element() == ']') {
                                all.add(ch);
                                all.add('-');
                            } else {
                                char nch = queue.remove();
                                if (nch < ch)
                                    error("Backward character range", queue);
                                do {
                                    all.add(ch);
                                } while (ch++ != nch);
                            }
                        } else {
                            all.add(ch);
                        }
                    } while(queue.element() != ']');
                    queue.remove();        // remove ]
                    char[] chSet = CommonSets.minimum(CommonSets.listToArray(all));
                    if (isComplementarySet) {
                        chSet = CommonSets.complementarySet(chSet);
                    }
                    nodeList.add(new LeftBracket());
                    for (int i = 0; i != chSet.length; i++) {
                        nodeList.add(new LChar(chSet[i]));
                        if (i == chSet.length - 1 || chSet[i + 1] == 0) {
                            break;
                        }
                        nodeList.add(new BOr());
                    }
                    nodeList.add(new RightBracket());
                    itemTerminated = true;
                    break;
                }
                case '{': {
                    int least = 0;
                    int most = 0;
                    if (Character.isDigit(queue.element())) {
                        most = least = getNumber(queue);
                    }
                    char next = queue.remove();
                    if (next == ',') {
                        if (Character.isDigit(queue.element())) {
                            most = getNumber(queue);
                        } else
                            most = -1;
                        next = queue.remove();
                    }
                    if (next != '}') {
                        error("Missing }", queue);
                    }
                    performMany(least, most);
                    itemTerminated = true;
                    break;
                }
                case '(': {
                    tryConcat();
                    nodeList.add(new LeftBracket());
                    itemTerminated = false;
                    break;
                }
                case ')': {
                    nodeList.add(new RightBracket());
                    itemTerminated = true;
                    break;
                }
                case '*': {
                    performMany(0, -1);
                    itemTerminated = true;
                    break;
                }
                case '?': {
                    performMany(0, 1);
                    itemTerminated = true;
                    break;
                }
                case '+': {
                    performMany(1, -1);
                    itemTerminated = true;
                    break;
                }
                case '|': {
                    nodeList.add(new BOr());
                    itemTerminated = false;
                    break;
                }
                case ' ':
                case '\t':
                    continue;       // ignore white space

                case '.':
                    tryConcat();
                    addTokenSet(CommonSets.dotCollection());
                    itemTerminated = true;
                    break;

                case '\\':
                    tryConcat();
                    addTokenSet(CommonSets.interpretEscape(queue));
                    itemTerminated = true;
                    break;
                case '\'':
                case '"':
                    addString(ch, queue);
                    break;

                default:
                    error("Unrecognised character: " + ch, queue);
            }
        }
    }

    // look back for a completed term
    private void performMany(int least, int most) {
        if (!(least == 1 && most == 1)) {
            if (least == 0 && most == -1) {
                nodeList.add(new BMany());
                nodeList.add(new LNull());
            } else {
                List<Node> sample;
                if (last() instanceof RightBracket) {
                    sample = new LinkedList<>();
                    sample.add(nodeList.remove(nodeList.size() - 1));
                    int stack = 1;
                    for (int i = nodeList.size() - 1; i >= 0; i--) {
                        Node node = nodeList.remove(i);
                        if (node instanceof RightBracket) {
                            stack++;
                        } else if (node instanceof LeftBracket) {
                            stack--;
                        }
                        sample.add(0, node);
                        if (stack == 0) {
                            break;
                        }
                    }
                } else {
                    sample = Collections.singletonList(nodeList.remove(nodeList.size() - 1));
                }

                if (most == -1) {
                    for (int i = 0; i < least; i++) {
                        nodeList.addAll(copyNodes(sample));
                        nodeList.add(new BConcat());
                    }
                    nodeList.addAll(copyNodes(sample));
                    nodeList.add(new BMany());
                    nodeList.add(new LNull());
                } else {
                    if (least != most) {
                        nodeList.add(new LeftBracket());
                        for (int i = least; i <= most; i++) {
                            nodeList.add(new LeftBracket());
                            if (i == 0) {
                                nodeList.add(new LClosure());
                            } else {
                                for (int j = 0; j != i; j++) {
                                    nodeList.addAll(copyNodes(sample));
                                    if (j != i - 1) {
                                        nodeList.add(new BConcat());
                                    }
                                }
                            }
                            nodeList.add(new RightBracket());
                            if (i != most) {
                                nodeList.add(new BOr());
                            }
                        }
                        nodeList.add(new RightBracket());
                    } else {
                        nodeList.add(new LeftBracket());
                        for (int i = 0; i < least; i++) {
                            nodeList.addAll(copyNodes(sample));
                            if (i != least - 1) {
                                nodeList.add(new BConcat());
                            }
                        }
                        nodeList.add(new RightBracket());
                    }
                }
            }
        }
    }

    public List<Node> copyNodes(List<Node> sample) {
        List<Node> result = new ArrayList<>(sample.size());
        for (Node node : sample) {
            result.add(Node.copy(node));
        }
        return result;
    }

    private Node last() {
        return nodeList.get(nodeList.size() - 1);
    }

    private void tryConcat() {
        if (itemTerminated) {
            nodeList.add(new BConcat());
            itemTerminated = false;
        }
    }

    public Node getRoot() {
        return root;
    }

}
