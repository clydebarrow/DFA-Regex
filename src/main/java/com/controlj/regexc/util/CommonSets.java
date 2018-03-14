package com.controlj.regexc.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

/**
 * Created on 2015/5/9.
 */
public class CommonSets {
    private static final char[] SLW; // slash lower w \w

    static {
        List<Character> chList = new ArrayList<>();
        for (char i = 'a'; i <= 'z'; i++) {
            chList.add(i);
        }
        for (char i = 'A'; i <= 'Z'; i++) {
            chList.add(i);
        }
        for (char i = '0'; i <= '9'; i++) {
            chList.add(i);
        }
        chList.add('_');
        SLW = listToArray(chList);
    }

    private static final char[] SUW = complementarySet(SLW); // slash upper w \W

    private static final char[] SLS = new char[]{' ', '\t'}; // slash lower s \s

    private static final char[] SUS = complementarySet(SLS); // slash upper s \S

    private static final char[] SLD = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    private static final char[] SUD = complementarySet(SLD);

    private static final char[] DOT = complementarySet(new char[]{'\n'});

    private static final List<Character> SLW_L = Collections.unmodifiableList(arrayToList(SLW));

    private static final List<Character> SUW_L = Collections.unmodifiableList(arrayToList(SUW));

    private static final List<Character> SLD_L = Collections.unmodifiableList(arrayToList(SLD));

    private static final List<Character> SUD_L = Collections.unmodifiableList(arrayToList(SUD));

    private static final List<Character> SLS_L = Collections.unmodifiableList(arrayToList(SLS));

    private static final List<Character> SUS_L = Collections.unmodifiableList(arrayToList(SUS));

    private static final List<Character> DOT_L = Collections.unmodifiableList(arrayToList(DOT));

    public static final int ENCODING_LENGTH = 256; // ascii encoding length, to support unicode, change this num, and change above sets also.

    public static char[] listToArray(List<Character> charList) {
        char[] result = new char[charList.size()];
        for (int i = 0; i < charList.size(); i++) {
            result[i] = charList.get(i);
        }
        return result;
    }

    public static List<Character> dotCollection() {
        return DOT_L;
    }

    private static List<Character> arrayToList(char[] charArr) {
        List<Character> chList = new ArrayList<>(charArr.length);
        for (char ch : charArr) {
            chList.add(ch);
        }
        return chList;
    }

    public static char[] complementarySet(char[] set) { // complementary set among ascii
        boolean[] book = emptyBook();
        for (char b : set) {
            book[b] = true;
        }
        return bookToSet(book, false);
    }

    public static char[] minimum(char[] set) { // [e, a, d, f, f, c, c, k, \s] -> {a, c, d, e, f, k, \0, \t}
        boolean[] book = emptyBook();
        for (char b : set) {
            book[b] = true;
        }
        return bookToSet(book, true);
    }

    // called with the queue pointing to the character after the \ character. Should consume all required
    // characters from queue
    public static List<Character> interpretEscape(Queue<Character> queue) {
        List<Character> result;
        char ch = queue.remove();

        switch (ch) {
            case 'n':
                result = Collections.singletonList('\n');
                break;
            case 'r':
                result = Collections.singletonList('\r');
                break;
            case 't':
                result = Collections.singletonList('\t');
                break;
            // octal constants are not implemented, as the usual syntax is dodgy
            // hex constants - two digit version only implemented
            case 'x':
                String sb = new String(new char[]{queue.remove(), queue.remove()});
                result = Collections.singletonList((char)Integer.parseInt(sb, 16));
                break;

                // control characters - implemented cheaply
            case 'c':
                result = Collections.singletonList((char)(queue.remove() & 0x1F));
                break;

                // escape character
            case 'e':
                result = Collections.singletonList('\u001B');
                break;

                // TODO - word and line boundaries

            case 'w':
                result = SLW_L;
                break;
            case 'W':
                result = SUW_L;
                break;
            case 's':
                result = SLS_L;
                break;
            case 'S':
                result = SUS_L;
                break;
            case 'd':
                result = SLD_L;
                break;
            case 'D':
                result = SUD_L;
                break;
            default:
                result = Collections.singletonList(ch);
                break;
        }
        return result;
    }

    private static boolean[] emptyBook() {
        boolean[] book = new boolean[ENCODING_LENGTH];
        for (int i = 0; i < book.length; i++) {
            book[i] = false;
        }
        return book;
    }

    private static char[] bookToSet(boolean[] book, boolean persistedFlag) {
        char[] newSet = new char[ENCODING_LENGTH];
        int i = 0;
        for (char j = 0; j < book.length; j++) {
            boolean e = book[j];
            if (e == persistedFlag) {
                newSet[i++] = j;
            }
        }
        return newSet;
    }
}
