package com.controlj.regexc.tree.node;

import com.controlj.regexc.automata.NFA;
import com.controlj.regexc.stack.OperatingStack;
import com.controlj.regexc.stack.ShuntingStack;

/**
 * Created on 2015/5/10.
 */
public class LChar extends LeafNode {

    public final char c;

    public LChar(char c) {
        this.c = c;
    }

    @Override
    public void accept(NFA nfa) {
        nfa.visit(this);
    }

    @Override
    public String toString() {
        String result;
        if (c == ' ') {
            result = "\\s";
        } else if (c == '\t') {
            result = "\\t";
        } else {
            result = String.valueOf(c);
        }
        return result + super.toString();
    }

    @Override
    public Node copy() {
        return new LChar(c);
    }

    @Override
    public void accept(OperatingStack operatingStack) {
        operatingStack.visit(this);
    }

    @Override
    public void accept(ShuntingStack shuntingStack) {
        shuntingStack.visit(this);
    }
}
