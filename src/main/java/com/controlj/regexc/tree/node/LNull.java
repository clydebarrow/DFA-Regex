package com.controlj.regexc.tree.node;

import com.controlj.regexc.automata.NFA;
import com.controlj.regexc.stack.OperatingStack;
import com.controlj.regexc.stack.ShuntingStack;

/**
 * Created on 2015/5/10.
 */
public class LNull extends LeafNode {
    @Override
    public Node copy() {
        return new LNull();
    }

    @Override
    public void accept(NFA nfa) {
        nfa.visit(this);
    }

    @Override
    public String toString() {
        return "{N}" + super.toString();
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
