package com.controlj.regexc.automata;

import com.controlj.regexc.tree.node.*;

import java.util.*;

/**
 * Only able to accept wfs accessing order, the class constructs a NFA by walking the input syntax tree recursively
 * from the root node.
 *
 * Created on 2015/5/10.
 */
public class NFA {

    private final Stack<NFAState> stateStack;
    private final NFAStateFactory stateFactory;
    private final List<NFAState> stateList;

    public NFA(Node root) {
        stateList = new ArrayList<>();
        stateFactory = new NFAStateFactory();
        NFAState initState = newState();
        NFAState finalState = newState();
        stateStack = new Stack<>();
        stateStack.push(finalState);
        stateStack.push(initState);
        dfs(root);
    }

    private NFAState newState() {
        NFAState nfaState = stateFactory.create();
        stateList.add(nfaState);
        return nfaState;
    }

    private void dfs(Node node) {
        node.accept(this);
        if (node.hasLeft()) {
            dfs(node.left());
            dfs(node.right());
        }
    }

    public List<NFAState> getStateList() {
        return stateList;
    }

    public void visit(LChar lChar) {
        NFAState i = stateStack.pop();
        NFAState f = stateStack.pop();
        i.transitionRule(lChar.c, f);
    }

    public void visit(LNull lNull) {
        // do nothing
    }

    public void visit(BOr bOr) {
        NFAState i = stateStack.pop();
        NFAState f = stateStack.pop();
        stateStack.push(f);
        stateStack.push(i);
        stateStack.push(f);
        stateStack.push(i);
    }

    public void visit(BConcat bConcat) {
        NFAState i = stateStack.pop();
        NFAState f = stateStack.pop();
        NFAState n = newState();
        stateStack.push(f);
        stateStack.push(n);
        stateStack.push(n);
        stateStack.push(i);
    }

    public void visit(BMany bMany) {
        NFAState i = stateStack.pop();
        NFAState f = stateStack.pop();
        NFAState n = newState();
        i.directRule(n);
        n.directRule(f);
        stateStack.push(n);
        stateStack.push(n);
    }

    public void visit(LClosure lClosure) {
        NFAState i = stateStack.pop();
        NFAState f = stateStack.pop();
        i.directRule(f);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(NFAState state : stateList) {
            sb.append(String.format("State %4d: ", state.getId()));
            Map<Character, Set<NFAState>> map = state.getTransitionMap();
            for(char c : map.keySet()) {
                sb.append(String.format("'%c' -> ", c));
                Set<NFAState> trans = map.get(c);
                for(NFAState n : trans) {
                    sb.append(n.getId());
                    sb.append(", ");
                }
                sb.append("; ");
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
