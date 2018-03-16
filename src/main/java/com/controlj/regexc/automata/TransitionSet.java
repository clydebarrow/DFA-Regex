package com.controlj.regexc.automata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class represents a set of transitions with the same target state and actions.
 * <p>
 * Copyright (C) Control-J Pty. Ltd. ACN 103594190
 * All rights reserved
 * <p>
 * User: clyde
 * Date: 15/3/18
 * Time: 06:17
 */
public class TransitionSet {
    public static class Range {
        public char first;
        public char last;

        public Range(char first, char last) {
            this.first = first;
            this.last = last;
        }
    }
    private Set<Character> tokens = new LinkedHashSet<>();
    private Set<Integer> actions = new LinkedHashSet<>();
    private boolean accept;     // true if this transition is to an accept
    private DFAState next;
    private ArrayList<Range> ranges;
    private ArrayList<Character> points;
    private boolean contiguous;

    public TransitionSet(DFATransition transition) {
        tokens.add(transition.getToken());
        next = transition.getNext();
        accept = transition.isAccept();
        actions.addAll(transition.getActions());
    }

    private void collect() {
        if(ranges != null)
            return;
        ArrayList<Character> sequence = new ArrayList<>(tokens);
        sequence.sort(Comparator.naturalOrder());
        ranges = new ArrayList<>();
        points = new ArrayList<>();
        for (int i = 0; i != sequence.size();) {
            char c = sequence.get(i);
            int j = 1;
            while (j + i != sequence.size() && sequence.get(j + i) == c + j)
                j++;
            if (j <= 2) {
                for(int k = 0 ; k != j ; k++)
                points.add((char) (c + k));
            } else
                ranges.add(new Range(c, (char) (c + j - 1)));
            i += j;
        }
    }
    public ArrayList<Range> getRanges() {
        collect();
        return ranges;
    }

    public ArrayList<Character> getPoints() {
        collect();
        return points;
    }

    private void addAction(int action) {
        actions.add(action);
    }

    public void setAccept(boolean accept) {
        this.accept = accept;
    }

    public void addToken(char token) {
        ranges = null;
        points = null;
        tokens.add(token);
    }

    public Set<Character> getTokens() {
        return tokens;
    }

    public Set<Integer> getActions() {
        return actions;
    }

    public DFAState getNext() {
        return next;
    }

    public boolean isAccept() {
        return accept;
    }

}
