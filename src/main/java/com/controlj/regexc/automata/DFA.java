package com.controlj.regexc.automata;

import com.controlj.regexc.util.CommonSets;

import java.util.*;

/**
 * Created on 2015/5/10.
 */
public class DFA {

    private int[][] transitionTable;
    // init state
    private DFAState initialState = null;
    // rejected state
    private int rejectState = -1;
    // final states
    private boolean[] finalStates;
    private int charLimit = CommonSets.ENCODING_LENGTH;       // character set size plus number of tags
    private ArrayList<DFAState> dfaStates;

    public DFA(List<NFAState> nfaStateList) {
        for (NFAState state : nfaStateList) {
            for (Character c : state.getTransitionMap().keySet()) {
                if (c >= charLimit)
                    charLimit = c + 1;
            }
        }
        convert(nfaStateList);
    }

    public int[][] getTransitionTable() {
        return transitionTable;
    }

    public int getRejectedState() {
        return rejectState;
    }

    public int getInitState() {
        return initialState.getId();
    }

    public boolean[] getFinalStates() {
        return finalStates;
    }

    private void convert(List<NFAState> nfaStateList) {
        NFAState initState = nfaStateList.get(0);
        NFAState finalState = nfaStateList.get(1);

        Map<NFAState, Set<NFAState>> closureMap = calculateClosure(nfaStateList);

        // construct a NFA first
        Map<NFAState, Map<Character, Set<NFAState>>> nfaTransitionMap = new HashMap<>();
        for (NFAState state : nfaStateList) {
            Map<Character, Set<NFAState>> subMap = new HashMap<>();
            for (char ch = 0; ch != charLimit; ch++) {
                Set<NFAState> closure = closureMap.get(state);
                Set<NFAState> reachable = traceReachable(closure, ch, closureMap);
                if (!reachable.isEmpty()) {
                    subMap.put(ch, reachable);
                }
            }
            nfaTransitionMap.put(state, subMap);
        }

        // Construct an original DFA using the constructed NFA. Each key which is set of nfa states is a new dfa state.
        Map<Set<NFAState>, Map<Character, Set<NFAState>>> originalDFATransitionMap = new HashMap<>();
        constructOriginalDFA(closureMap.get(initState), nfaTransitionMap, originalDFATransitionMap);

        // construct minimum DFA
        minimize(originalDFATransitionMap, closureMap.get(initState), finalState);
    }

    private void constructOriginalDFA(Set<NFAState> stateSet, Map<NFAState, Map<Character, Set<NFAState>>> nfaTransitionMap, Map<Set<NFAState>, Map<Character, Set<NFAState>>> originalDFATransitionMap) {

        Stack<Set<NFAState>> stack = new Stack<>();
        stack.push(stateSet);

        do {
            Set<NFAState> pop = stack.pop();
            Map<Character, Set<NFAState>> subMap = originalDFATransitionMap.computeIfAbsent(pop, k -> new HashMap<>());
            for (char ch = 0; ch != charLimit; ch++) {
                Set<NFAState> union = new HashSet<>();
                for (NFAState state : pop) {
                    Set<NFAState> nfaSet = nfaTransitionMap.get(state).get(ch);
                    if (nfaSet != null) {
                        union.addAll(nfaSet);
                    }
                }
                if (!union.isEmpty()) {
                    subMap.put(ch, union);
                    if (!originalDFATransitionMap.containsKey(union)) {
                        stack.push(union);
                    }
                }
            }
        } while (!stack.isEmpty());
    }

    private Map<NFAState, Set<NFAState>> calculateClosure(List<NFAState> nfaStateList) {
        Map<NFAState, Set<NFAState>> map = new HashMap<>();
        for (NFAState state : nfaStateList) {
            Set<NFAState> closure = new HashSet<>();
            dfsClosure(state, closure);
            map.put(state, closure);
        }
        return map;
    }

    private void dfsClosure(NFAState state, Set<NFAState> closure) {
        Stack<NFAState> nfaStack = new Stack<>();
        nfaStack.push(state);
        do {
            NFAState pop = nfaStack.pop();
            closure.add(pop);
            for (NFAState next : pop.getDirectTable()) {
                if (!closure.contains(next)) {
                    nfaStack.push(next);
                }
            }
        } while (!nfaStack.isEmpty());
    }

    private Set<NFAState> traceReachable(Set<NFAState> closure, char ch, Map<NFAState, Set<NFAState>> closureMap) {
        Set<NFAState> result = new HashSet<>();
        for (NFAState closureState : closure) {
            Map<Character, Set<NFAState>> transitionMap = closureState.getTransitionMap();
            Set<NFAState> stateSet = transitionMap.get(ch);
            if (stateSet != null) {
                for (NFAState state : stateSet) {
                    result.addAll(closureMap.get(state)); // closure of all the reachable states by scanning a char of the given closure.
                }
            }
        }
        return result;
    }

    private void minimize(Map<Set<NFAState>, Map<Character, Set<NFAState>>> oriDFATransitionMap, Set<NFAState> initClosure, NFAState finalNFAState) {
        Set<DFATransition> transitions = new HashSet<>();
        Map<Set<NFAState>, DFAState> nfaStateMap = new HashMap<>();
        // conscruct list of DFA states
        for (Set<NFAState> nfaState : oriDFATransitionMap.keySet()) {
            DFAState dfaState = new DFAState();
            nfaStateMap.put(nfaState, dfaState);
            if (nfaState.contains(finalNFAState))
                dfaState.setAccept(true);
        }
        initialState = nfaStateMap.get(initClosure);
        // add transitions
        for (Map.Entry<Set<NFAState>, Map<Character, Set<NFAState>>> entry : oriDFATransitionMap.entrySet()) {
            DFAState dfaState = nfaStateMap.get(entry.getKey());
            for (Map.Entry<Character, Set<NFAState>> trans : entry.getValue().entrySet()) {
                DFAState next = nfaStateMap.get(trans.getValue());
                DFATransition transition = dfaState.add(trans.getKey(), new DFATransition(trans.getKey(), next));
                transition.setAccept(next.isAccept());
                transitions.add(transition);
            }
        }

        // look for states with only one transition, which involves an action rather than a real token.
        // Replace that state with the target state
        // if a state has action transitions but more than one transition, replace the action transitions with the set of
        // transitions for the next state, tagged with the action
        for (DFAState state : nfaStateMap.values()) {
            for (DFATransition trans : new ArrayList<>(state.getTransitionMap().values())) {
                if (!trans.isMetaToken())
                    continue;
                DFAState next = trans.getNext();
                Set<Integer> actions = trans.getActions();
                // if only one transition in this state, simply make any references to it point to the successor state, with action
                if (state.getTransitionMap().size() == 1) {
                    for (DFATransition otrans : transitions) {
                        if (otrans.getNext() == state) {
                            otrans.setNext(next);
                            otrans.addActions(actions);
                        }
                    }
                    continue;
                }
                // we have multiple transitions; expand this one with the subsequent tokens
                state.getTransitionMap().remove(trans.getToken());
                for (DFATransition nextTrans : next.getTransitionMap().values()) {
                    DFATransition newTrans = nextTrans.copy();
                    newTrans.addActions(actions);
                    state.add(nextTrans.getToken(), newTrans);
                }
            }
        }
        // remove unused states and renumber.
        Set<DFAState> stateSet = new HashSet<>();
        addUsed(initialState, stateSet);
        dfaStates = new ArrayList<>();
        // make the initial state 0 for convenience
        dfaStates.add(initialState);
        initialState.setId(0);
        for (DFAState state : stateSet) {
            if (state != initialState) {
                state.setId(dfaStates.size());
                dfaStates.add(state);
            }
        }
        rejectState = dfaStates.size();
        transitionTable = new int[rejectState][];
        finalStates = new boolean[rejectState];
        for (DFAState state : dfaStates) {
            finalStates[state.getId()] = state.isAccept();
            transitionTable[state.getId()] = new int[CommonSets.ENCODING_LENGTH];
            Arrays.fill(transitionTable[state.getId()], rejectState);
            for (Map.Entry<Character, DFATransition> transition : state.getTransitionMap().entrySet()) {
                transitionTable[state.getId()][transition.getKey()] = transition.getValue().getNextId();
            }
        }
    }

    private void addUsed(DFAState state, Set<DFAState> states) {
        if (states.contains(state))
            return;
        states.add(state);
        for (DFATransition transition : state.getTransitionMap().values()) {
            addUsed(transition.getNext(), states);
        }
    }

    public static String charRep(int val) {
        if (val < 0x20 || val > 0x7E)
            return String.format("\\x%02x", val);
        return "" + (char) val;

    }

    public ArrayList<DFAState> getDfaStates() {
        return dfaStates;
    }

    private void collect(DFAState state, StringBuilder sb) {
        Map<Character, DFATransition> map = state.getTransitionMap();
        Set<Character> keySet = map.keySet();
        List<Character> keyList = new ArrayList<>(keySet);
        keyList.sort(Character::compareTo);
        for (int i = 0; i != keyList.size(); i++) {
            char c = keyList.get(i);
            DFATransition trans = map.get(c);
            int nextId = trans.getNext().getId();
            char last = c;
            while (keySet.contains((char) (last + 1)) && map.get((char) (last + 1)).getNext().getId() == nextId) {
                i++;
                last++;
            }
            if (c == last) {
                sb.append('\'');
                sb.append(charRep(c));
                sb.append('\'');
            } else {
                sb.append('[');
                sb.append(charRep(c));
                sb.append('-');
                sb.append(charRep(last));
                sb.append(']');
            }
            sb.append("->");
            if (!trans.getActions().isEmpty()) {
                sb.append('@');
                sb.append(trans.getActions());
                sb.append(' ');
            }
            sb.append(nextId);
            if (trans.isAccept())
                sb.append('*');
            sb.append(", ");
        }
        sb.append('\n');
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Initial State = ");
        sb.append(initialState.getId());
        sb.append('\n');
        for (DFAState state : dfaStates) {
            sb.append("State ");
            sb.append(String.format("%3d", state.getId()));
            if (state.isAccept())
                sb.append('*');
            else
                sb.append(": ");
            collect(state, sb);
        }
        return sb.toString();
    }
}
