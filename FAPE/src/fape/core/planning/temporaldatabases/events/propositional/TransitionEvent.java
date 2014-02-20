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

import fape.core.planning.model.Action;
import fape.core.planning.model.ParameterizedStateVariable;
import fape.core.planning.model.VariableRef;
import fape.core.planning.temporaldatabases.events.TemporalEvent;

/**
 *
 * @author FD
 */
public class TransitionEvent extends TemporalEvent {

    /**
     *
     */
    public final VariableRef from, to;

    public TransitionEvent(ParameterizedStateVariable sv, VariableRef from, VariableRef to) {
        this.stateVariable = sv;
        this.from = from;
        this.to = to;
        this.mID = counter++;
    }

    public TransitionEvent bindedCopy(Action a) {
        TransitionEvent te = new TransitionEvent(
                a.getBindedStateVariable(stateVariable),
                a.GetBindedVariableRef(from),
                a.GetBindedVariableRef(to));
        te.start = start;
        te.end = end;
        te.mID = counter++;
        return te;
    }

    @Override
    public String toString() {
        return "@[" + start + "," + end + "):" + from + "->" + to;
    }

    @Override
    public TemporalEvent DeepCopy(boolean assignNewID) {
        TransitionEvent e = new TransitionEvent(stateVariable, from, to);
        e.mID = this.mID;
        e.start = this.start;
        e.end = this.end;
        return e;
    }

    @Override
    public String Report() {
        return "[" + start + "," + end + "] transition "+((from==null)?"null":from)+" -> "+((to==null)?"null":to);
    }
}
