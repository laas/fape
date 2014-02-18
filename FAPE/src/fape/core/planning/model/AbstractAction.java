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
import fape.core.planning.constraints.UnificationConstraintSchema;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class AbstractAction {
    
    public AbstractAction(){
        
    }

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

    public String typeOfParameter(String paramName) {
        for(Instance i : params) {
            if(i.name.equals(paramName))
                return i.type;
        }

        throw new FAPEException("Unable to find param: " + paramName);
    }

    /**
     *
     * @return
     */
    public float GetDuration() {
        return 10.0f;
    }

    @Override
    public String toString() {
        return name;
    }
}
