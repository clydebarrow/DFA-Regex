package com.controlj.regexc.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Copyright (C) Control-J Pty. Ltd. ACN 103594190
 * All rights reserved
 * <p>
 * User: clyde
 * Date: 9/3/18
 * Time: 09:48
 */
public class Actions {
    private List<String> actions = new ArrayList<>();
    private String header;

    public List<String> getActions() {
        return actions;
    }

    public int add(String action) {
        int i = actions.size();
        actions.add(action);
        return i;
    }

    public String getAction(int idx) {
        return actions.get(idx);
    }

    public String getText(Collection<Integer> actions) {
        ArrayList<Integer> list = new ArrayList<>(actions);
        list.sort(Comparator.naturalOrder());
        StringBuilder sb = new StringBuilder();
        for(int idx : list) {
            sb.append(getAction(idx));
        }
        return sb.toString();
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}
