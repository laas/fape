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

import fape.core.execution.model.ActionRef;
import fape.core.execution.model.Instance;
import fape.core.execution.model.TemporalConstraint;
import fape.core.planning.states.State;
import fape.util.Pair;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class AbstractAction {

    /**
     *
     */
    public String name;

    /**
     *
     */
    public List<AbstractTemporalEvent> events = new ArrayList<>();

    /**
     *
     */
    public List<Instance> params;

    /**
     *
     */
    public List<Pair<List<ActionRef>, List<TemporalConstraint>>> strongDecompositions;
    List<Pair<Integer, Integer>> localBindings;

    /**
     *
     * @return
     */
    public float GetDuration() {
        return 1.0f;
    }

    /**
     * we use relative references here .. if they share the same variable, they are tied together by the same predecesor constraint
     * @return 
     */
    public List<Pair<Integer, Integer>> GetLocalBindings() {
        if (localBindings == null) {
            localBindings = new LinkedList<>();
            for (int i = 0; i < events.size(); i++) {
                for (int j = i + 1; j < events.size(); j++) {
                    AbstractTemporalEvent e1 = events.get(i), e2 = events.get(j);
                    if (e1.stateVariableReference.refs.getFirst().equals(e2.stateVariableReference.refs.getFirst())) {
                        localBindings.add(new Pair(i,j));
                    }
                }
            }
        }
        return localBindings;

    }

    public String toString() {
        return name;
    }
}
