package com.controlj.regexc;

import com.controlj.regexc.automata.DFA;
import com.controlj.regexc.automata.NFA;
import com.controlj.regexc.tree.SyntaxTree;

/**
 * Created on 2015/5/11.
 */
public class RegexMatcher {
    private int[][] transitionTable;
    private int is;
    private int rs;
    private boolean[] fs;

    public RegexMatcher(String regex) {
        compile(regex);
    }

    private void compile(String regex) {
        SyntaxTree syntaxTree = new SyntaxTree(regex, null);
        NFA nfa = new NFA(syntaxTree.getRoot());
        DFA dfa = new DFA(nfa.getStateList());
        transitionTable = dfa.getTransitionTable();
        is = dfa.getInitState();
        fs = dfa.getFinalStates();
        rs = dfa.getRejectedState();
    }

    public boolean match(String str) {
        int s = is;
        for (int i = 0, length = str.length(); i != length; i++) {
            char ch = str.charAt(i);
            s = transitionTable[s][ch];
            if (s == rs) {
                return false; // fast failed using rejected state
            }
        }
        return fs[s];
    }
}
