package com.controlj.regexc.automata;

import com.controlj.regexc.util.CommonSets;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Copyright (C) Control-J Pty. Ltd. ACN 103594190
 * Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 * <p>
 * User: clyde
 * Date: 9/3/18
 * Time: 09:39
 */
public class DFATransition {
    private Set<Integer> actions = new LinkedHashSet<>();
    private DFAState next;
    private char token;
    private boolean accept;     // true if this transition is to an accept

    public DFATransition(char token, DFAState next) {
        this.token = token;
        this.next = next;
        if(isMetaToken())
            actions.add(token - CommonSets.ENCODING_LENGTH);
    }

    public Set<Integer> getActions() {
        return actions;
    }

    public DFAState getNext() {
        return next;
    }

    public DFATransition copy() {
        DFATransition newTrans = new DFATransition(token, next);
        newTrans.actions = new LinkedHashSet<>(actions);
        newTrans.setAccept(accept);
        return newTrans;
    }

    public boolean isMetaToken() {
        return token >= CommonSets.ENCODING_LENGTH;
    }

    public boolean isAccept() {
        return accept;
    }

    public void setAccept(boolean accept) {
        this.accept = accept;
    }

    public int getNextId() {
        return next.getId();
    }

    public void setNext(DFAState next) {
        this.next = next;
    }

    public void addAction(int action) {
        actions.add(action);
    }

    public char getToken() {
        return token;
    }

    public void addActions(Collection<Integer> actions) {
        this.actions.addAll(actions);
    }
}
