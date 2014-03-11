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

import fape.core.execution.model.statements.Statement;
import fape.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class Action {

    /**
     *
     */
    public String name;

    /**
     *
     */
    public List<Instance> params = new LinkedList<>(); //typed parameters

    /**
     *
     */
    public List<Statement> statements = new LinkedList<>(); //what happens
    //public float duration; //end - start of the action

    /**
     *
     */
        public float softRefinement; //how critical is it to make some strongDecomposition for this action

    /**
     *
     */
    public Statement hardRefinement; //if this holds, we need to make strongDecomposition

    /**
     *
     */
    public Pair<List<Action>, List<TemporalConstraint>> weakDecomposition; //we can always try to apply this decomposition, it contains some actions and constraints on their ordering

    /**
     *
     */
    public List<Pair<List<ActionRef>, List<TemporalConstraint>>> strongDecompositions = new LinkedList<>(); //same as weak though we choose only one from those

    @Override
    public String toString() {
        String st = name + "(";
        for (Instance r : params) {
            st += r + ",";
        }
        if (params.size() > 0) {
            st = st.substring(0, st.length() - 1);
        }
        st += ")";
        return st;
    }
}
