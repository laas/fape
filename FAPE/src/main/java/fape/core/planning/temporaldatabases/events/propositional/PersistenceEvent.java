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
package fape.core.planning.temporaldatabases.events.propositional;

import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.model.Action;
import fape.core.planning.model.ParameterizedStateVariable;
import fape.core.planning.model.VariableRef;
import fape.core.planning.temporaldatabases.events.TemporalEvent;

/**
 *
 * @author FD
 */
public class PersistenceEvent extends TemporalEvent {

    /**
     * The value (in the form of variable) at which this persistence condition refers
     */
    public final VariableRef value;

    public PersistenceEvent(ParameterizedStateVariable sv, VariableRef value) {
        this.stateVariable = sv;
        this.value = value;
        this.mID = counter++;
    }

    public PersistenceEvent bindedCopy(Action a) {
        PersistenceEvent pe = new PersistenceEvent(
                a.getBindedStateVariable(stateVariable),
                a.GetBindedVariableRef(value));
        pe.start = start;
        pe.end = end;
        pe.mID = counter++;
        return pe;
    }

    @Override
    public String toString() {
        return "@[" + start + "," + end + "):=" + value;
    }

    @Override

    public TemporalEvent DeepCopy(boolean assignNewID) {
        PersistenceEvent e = new PersistenceEvent(stateVariable, value);
        e.mID = this.mID;
        e.start = this.start;
        e.end = this.end;
        return e;
    }

    @Override
    public String Report() {
        return "[" + start + "," + end + "] persistence "+value;
    }

    @Override
    public VariableRef endValue() {
        return value;
    }

    @Override
    public VariableRef startValue() {
        return value;
    }
}
