package com.controlj.regexc;

import com.controlj.regexc.automata.DFA;
import com.controlj.regexc.util.Actions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright (C) Control-J Pty. Ltd. ACN 103594190
 * <p>
 * User: clyde
 * Date: 9/3/18
 * Time: 18:52
 */
public class Rex {
    public static final String PREFIX = "%prefix";
    public static final String RULE = "%rule";
    public static final String NAMES = "%names";
    private final String source;
    private Actions actions;
    private DFA dfa;
    private String prefix;
    private LineNumberReader reader;
    private StringBuilder reBuilder;        // accumulate parts of the RE
    private List<String> rules = new ArrayList<>();
    private StringBuilder actionBuilder = new StringBuilder();    // accumulate an action
    private Map<String, String> names = new HashMap<>();
    private int nesting = 0;
    private Pattern namePattern = Pattern.compile("^([a-zA-Z]\\w*)\\h*=\\h*(.*)$");

    /*
        an RE file comprises:
        * a prefix, which will be used to name the generated functions and files
        * a header, containing code that will be prepended to the generated C file;
        * A set of names defining REs which can be used in rules to substitute common sequences
        * A set of rules, which may contain embedded actions

        The DFA will be built from the set of rules as alternates, e.g. rule1|rule2|rule3

        White space (unless in quotes) is removed.

        An example file:
        %prefix example
        #include <file.h>       // part of the header
        static char buffer[128];
        %names
        digit = [0-9]
        letter = [A-Fa-f]
        startchar = '\n'
        word = letter (digit|letter)*
        %rule
        startchar
            (digit | letter)+ {
                    ptr = x;
                  }
            '\r'
        %rule
        digit{1,5}','



     */
    public Rex(String source, InputStream stream) throws IOException {
        this.source = source;
        reader = new LineNumberReader(new InputStreamReader(stream));
    }

    private void error(String message) throws IOException {
        throw new IOException(message + " at line " + reader.getLineNumber());
    }

    public Actions getActions() {
        return actions;
    }

    public DFA getDfa() {
        return dfa;
    }

    public String getPrefix() {
        return prefix;
    }

    // add a line to the re.
    private void addLine(String line) throws IOException {
        line = line.trim();
        if (nesting == 0 && line.startsWith("#"))
            return;
        StringBuilder sb;
        CharacterIterator it = new StringCharacterIterator(line);
        char c = it.first();
        if (c == '{') {
            if (nesting == 0)
                actionBuilder.setLength(0);
            nesting++;
            actionBuilder.append(c);
            c = it.next();
        }
        if (nesting != 0)
            sb = actionBuilder;
        else {
            sb = reBuilder;
        }
        char delimiter = 0;
        for (; c != CharacterIterator.DONE; c = it.next()) {
            if (delimiter != 0) {
                sb.append(c);
                if (c == '\\') {
                    sb.append(it.next());
                    continue;
                }
                if (c == delimiter)
                    delimiter = 0;
                continue;
            }
            if (c == '\'' || c == '"')
                delimiter = c;
            if (nesting != 0) {
                if (c == '{') {
                    nesting++;
                    sb = actionBuilder;
                    actionBuilder.append(c);
                } else if (c == '}') {
                    actionBuilder.append(c);
                    nesting--;
                    if (nesting == 0) {
                        reBuilder.append((char) ('\uE000' + actions.add(sb.toString())));
                        sb = reBuilder;
                    }
                }
            } else
                reBuilder.append(c);
        }
        sb.append(' ');
    }

    private void addRule() {
        if (reBuilder.length() == 0)
            return;
        rules.add(reBuilder.toString());
        reBuilder.setLength(0);
    }

    public void read() throws IOException {
        reBuilder = new StringBuilder();
        String s;
        try {
            StringBuilder sb = new StringBuilder();
            for (; ; ) {
                s = reader.readLine();
                if (s == null)
                    error("Unexpected EOF looking for header");
                if (s.startsWith(PREFIX)) {
                    if (prefix != null)
                        error("Duplicate prefix");
                    prefix = s.substring(PREFIX.length()).trim();
                    continue;
                }
                if (s.startsWith(NAMES) || s.startsWith(RULE)) {
                    break;
                }
                sb.append(s);
                sb.append("\n");
            }
            sb.append('\n');
            actions.setHeader(sb.toString());
            sb.setLength(0);
            if (s.startsWith(NAMES)) {
                for (; ; ) {
                    s = reader.readLine();
                    if (s.startsWith(RULE))
                        break;
                    Matcher matcher = namePattern.matcher(s.trim());
                    if (matcher.matches()) {
                        names.put(matcher.group(1), matcher.group(2));
                    } else
                        error("Syntax error in name definition");
                }
            }
            for (; ; ) {
                reBuilder.setLength(0);
                s = reader.readLine();
                if (s == null)
                    break;
                if (s.trim().matches(RULE))
                    addRule();
                else
                    addLine(s);
            }

        } finally {
            reader.close();
        }
    }
}
