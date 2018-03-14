package com.controlj.regexc.tree.node;

import com.controlj.regexc.automata.NFA;
import com.controlj.regexc.stack.OperatingStack;
import com.controlj.regexc.stack.ShuntingStack;

/**
 * Created on 5/5/15.
 */
public abstract class Node {

    private Node left;
    private Node right;
    private int tag = -1;

    public Node() {
        left = right = null;
    }

    public Node right() {
        return right;
    }

    public Node left() {
        return left;
    }

    public boolean hasLeft() {
        return left != null;
    }

    public boolean hasRight() {
        return right != null;
    }

    public void setLeft(Node left) {
        this.left = left;
    }

    public void setRight(Node right) {
        this.right = right;
    }

    public abstract void accept(NFA nfa);

    protected abstract Node copy();

    public abstract void accept(OperatingStack operatingStack);

    public abstract void accept(ShuntingStack shuntingStack);

    @Override
    public String toString() {
        if(tag != -1)
            return "@" + tag;
        return "";
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return tag;
    }

    static public Node copy(Node node) {
        Node nodeCopy = node.copy();
        nodeCopy.setTag(node.getTag());
        return nodeCopy;
    }
}
