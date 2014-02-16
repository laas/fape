/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.core.execution.model;

import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author FD
 */
public class Reference {

    public Reference() {}

    public Reference(String name) {
        this.refs.add(name);
    }

    public Reference(Reference ref) {
        this.refs.addAll(ref.refs);
    }

    /**
     *
     */
    public LinkedList<String> refs = new LinkedList<>();

    @Override
    public String toString() {
        String ret = "";
        for (String s : refs) {
            ret += s + ".";
        }
        ret = ret.substring(0, ret.length() - 1);
        return ret;
    }

    /**
     *
     * @return
     */
    public String GetConstantReference() {
        String st = toString();
        if (st.contains(".")) {
            st = st.substring(0, st.indexOf("."));
        }
        return st;
    }

    public void ReplaceFirstReference(Reference get) {
        this.refs.pollFirst();
        Iterator<String> it = get.refs.descendingIterator();
        while(it.hasNext()){
            refs.addFirst(it.next());
        }        
    }

    public void ReplaceFirstReference(String first) {
        this.refs.pollFirst();
        refs.addFirst(first);
    }
}
