package com.controlj.regexc;

import com.controlj.regexc.automata.DFA;
import com.controlj.regexc.automata.DFAState;
import com.controlj.regexc.automata.DFATransition;
import com.controlj.regexc.util.Actions;
import com.controlj.regexc.util.CommonSets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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

    private void putSwitch(DFAState state) throws IOException {
        if (state.isAccept())
            return;
        Map<Character, DFATransition> map = state.getTransitionMap();
        Set<Character> keySet = map.keySet();
        List<Character> keyList = new ArrayList<>(keySet);
        keyList.sort(Character::compareTo);
        format("        case %s_STATE_%d:\n", prefix, state.getId());
        format("            switch(token) {\n");
        for (int i = 0; i != keyList.size(); i++) {
            char c = keyList.get(i);
            DFATransition trans = map.get(c);
            int nextId = trans.getNext().getId();
            char last = c;
            format("                case %s:", charRep(c));
            while (keySet.contains((char) (last + 1)) && map.get((char) (last + 1)).getNext().getId() == nextId) {
                i++;
                last++;
                format(" case %s: ", charRep(last));
            }
            format("\n");
            if (!trans.getActions().isEmpty()) {
                format("                    {%s }\n", actions.getText(trans.getActions()));
            }
            if (trans.getNext().isAccept())
                format("                    return %s;\n", accept);
            else {
                format("                    %s_state = %s_STATE_%d;\n", prefix, prefix, nextId);
                format("                    return %s;\n", contnue);
            }
        }
        if (keyList.size() != CommonSets.ENCODING_LENGTH) {
            format("                default:\n");
            format("                    %s_state = %s_STATE_%d;\n", prefix, prefix, dfa.getInitState());
            format("                    return %s;\n", fail);
        }
        format("            }\n");
    }

    public void write() throws IOException {
        //noinspection ResultOfMethodCallIgnored
        path.mkdirs();
        String filename = "lex_" + prefix;
        writer = new BufferedWriter(new FileWriter(new File(path, filename + ".h")));
        format("typedef enum {%s, %s, %s} %s_action_t;\n", accept, contnue, fail, prefix);
        format("typedef enum {\n");
        for(DFAState state : dfa.getDfaStates()) {
            if(!state.isAccept())
                format("    %s_STATE_%d,\n", prefix, state.getId());
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
