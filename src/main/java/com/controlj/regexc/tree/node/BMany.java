package com.controlj.regexc.tree.node;

import com.controlj.regexc.automata.NFA;
import com.controlj.regexc.stack.OperatingStack;
import com.controlj.regexc.stack.ShuntingStack;

/**
 * Created on 2015/5/10.
 */
public class BMany extends BranchNode {
    @Override
    public String toString() {
        return "[M]" + super.toString();
    }

    @Override
    public void accept(NFA nfa) {
        nfa.visit(this);
    }

    @Override
    protected Node copy() {
        return new BMany();
    }

    @Override
    public void accept(OperatingStack operatingStack) {
        operatingStack.visit(this);
    }

    @Override
    public void accept(ShuntingStack shuntingStack) {
        shuntingStack.visit(this);
    }

    @Override
    public int getPri() {
        return 2;
    }
}
