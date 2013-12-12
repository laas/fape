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
package fape.core.planning.model;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class StateVariableValue {

    public boolean Unifiable(StateVariableValue val1) {
        List<String> vals = new LinkedList<>(val1.values);
        vals.retainAll(this.values);
        return vals.size() > 0;
    }

    public List<String> values = new LinkedList<>();

    /**
     *
     *
     * defines the parameter representing the value
     */
    public String valueDescription;

    /**
     *
     */
    public int index = -1;
    //public int index = -1;

    public String toString() {
        return valueDescription + " " + values.toString();
    }
}
