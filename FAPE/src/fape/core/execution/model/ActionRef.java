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

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class ActionRef {

    /**
     *
     */
    public String name;

    /**
     *
     */
    public List<Reference> args = new LinkedList<>();

    @Override
    public String toString() {
        String st = name + "(";
        for (Reference r : args) {
            st += r + ",";
        }
        if (args.size() > 0) {
            st = st.substring(0, st.length() - 1);
        }
        st += ")";
        return st;
    }
}
