package com.controlj.regexc.automata;

import java.util.*;

/**
 * Copyright (C) Control-J Pty. Ltd. ACN 103594190
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * <p>
 * <p>
 * User: clyde
 * Date: 9/3/18
 * Time: 09:16
 */
public class DFAState {
    private int id;     // state number
    private boolean reject;     // if this state represents rejection of the match
    private boolean accept;       // if this is an accept state
    private Map<Character, DFATransition> transitionMap = new LinkedHashMap<>();      // transitions
    private String action;      // the action to be performed when this state

    public DFAState() {
    }

    public DFAState(int id, boolean accept) {
        this.id = id;
        this.accept = accept;
    }

    public Map<Character, DFATransition> getTransitionMap() {
        return transitionMap;
    }

    public DFATransition add(char c, DFATransition transition) {
        transitionMap.put(c, transition);
        return transition;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isReject() {
        return reject;
    }

    public void setReject(boolean reject) {
        this.reject = reject;
    }

    public boolean isAccept() {
        return accept;
    }

    public void setAccept(boolean accept) {
        this.accept = accept;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    // convert our transitions to a list of transition sets
    public List<TransitionSet> getTransitionSets() {
        ArrayList<TransitionSet> list = new ArrayList<>();
        Set<DFATransition> done = new HashSet<>();
        for (DFATransition trans : transitionMap.values()) {
            if (!done.contains(trans)) {
                TransitionSet set = new TransitionSet(trans);
                list.add(set);
                done.add(trans);
                for (DFATransition trans2 : transitionMap.values()) {
                    if (!done.contains(trans2) && trans.matchingTarget(trans2)) {
                        set.addToken(trans2.getToken());
                        done.add(trans2);
                    }
                }
            }
        }
        return list;
    }
}
