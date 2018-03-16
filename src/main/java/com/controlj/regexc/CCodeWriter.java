package com.controlj.regexc;

import com.controlj.regexc.automata.DFA;
import com.controlj.regexc.automata.DFAState;
import com.controlj.regexc.automata.TransitionSet;
import com.controlj.regexc.util.Actions;
import com.controlj.regexc.util.CommonSets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Copyright (C) Control-J Pty. Ltd. ACN 103594190
 * All rights reserved
 * <p>
 * User: clyde
 * Date: 9/3/18
 * Time: 17:34
 */
public class CCodeWriter {
    private DFA dfa;
    private String prefix;
    private File path;
    private String accept;
    private String contnue;
    private String fail;
    private Actions actions;
    private BufferedWriter writer;

    public CCodeWriter(DFA dfa, File path, String prefix, Actions actions) {
        this.dfa = dfa;
        if (path == null)
            path = new File(".");
        this.path = path;
        this.prefix = prefix;
        accept = prefix + "_ACCEPT";
        contnue = prefix + "_CONTINUE";
        fail = prefix + "_FAIL";
        this.actions = actions;
    }

    private void format(String s, Object... args) throws IOException {
        writer.write(String.format(Locale.US, s, args));
    }

    private static String charRep(char val) {
        if (val < 0x20 || val > 0x7E)
            return String.format("0x%02x", (int) val);
        return "'" + val + "'";
    }

    private void putCode(TransitionSet set, int thisId) throws IOException {
        int nextId = set.getNext().getId();
        if (!set.getActions().isEmpty()) {
            format("                    {%s }\n", actions.getText(set.getActions()));
        }
        if (set.getNext().isAccept())
            format("                    return %s;\n", accept);
        else {
            if (nextId == thisId + 1)
                format("                    ++%s_state; // = %s_STATE_%d;\n", prefix, prefix, nextId);
            else
                format("                    %s_state = %s_STATE_%d;\n", prefix, prefix, nextId);
            format("                    return %s;\n", contnue);
        }
    }

    private void putSwitch(DFAState state) throws IOException {
        if (state.isAccept())
            return;
        int thisId = state.getId();
        format("        case %s_STATE_%d:\n", prefix, thisId);
        boolean first;
        for (TransitionSet set : state.getTransitionSets()) {
            // do ranges now for this transition target
            List<TransitionSet.Range> ranges = set.getRanges();
            if (!ranges.isEmpty()) {
                first = true;
                format("            if(");
                for (TransitionSet.Range range : ranges) {
                    if (!first)
                        format("||\n                  ");
                    first = false;
                    format("token >= %s && token <= %s", charRep(range.first), charRep(range.last));
                }
                format(") {\n");
                putCode(set, thisId);
                format("            }\n");
            }
        }
        // now do any switching necessary
        first = true;
        for (TransitionSet set : state.getTransitionSets()) {
            int cnt = 0;
            for (char c : set.getPoints()) {
                if (first)
                    format("            switch(token) {");
                first = false;
                if ((cnt % 5) == 0)
                    format("\n                ");
                format(" case %s: ", charRep(c));
                cnt++;
            }
            if (!first) {
                format("\n");
                putCode(set, thisId);
            }
        }
        if (state.getTransitionMap().size() != CommonSets.ENCODING_LENGTH) {
            if (!first) {
                format("                default:\n");
                format("                    break;\n");
                format("            }\n");
            }
            format("            return %s;\n", fail);
        }
        format("\n");
    }

    public void write() throws IOException {
        //noinspection ResultOfMethodCallIgnored
        path.mkdirs();
        String filename = "lex_" + prefix;
        writer = new BufferedWriter(new FileWriter(new File(path, filename + ".h")));
        format("typedef enum {%s, %s, %s} %s_action_t;\n", accept, contnue, fail, prefix);
        format("typedef enum {\n");
        for (DFAState state : dfa.getDfaStates()) {
            if (!state.isAccept())
                format("    %s_STATE_%d = %d,\n", prefix, state.getId(), state.getId());
        }
        format("} %s_state_t;\n", prefix);
        format("extern %s_action_t %s_lex(unsigned char token);\n", prefix, prefix);
        format("#define %s_reset() (%s_state = %s_STATE_%d)\n", prefix, prefix, prefix, dfa.getInitState());
        writer.close();
        writer = new BufferedWriter(new FileWriter(new File(path, filename + ".c")));
        format("#include \"%s\"\n\n", filename + ".h");
        format("static %s_state_t %s_state;\n\n", prefix, prefix);
        format("%s\n", actions.getHeader());
        format("%s_action_t %s_lex(unsigned char token) {\n", prefix, prefix);
        format("    switch(%s_state) {\n", prefix);
        for (DFAState state : dfa.getDfaStates()) {
            putSwitch(state);
        }
        format("    }\n");
        format("}\n");
        writer.close();
    }
}