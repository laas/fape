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

import fape.core.execution.model.Reference;
import fape.core.execution.model.TemporalInterval;
import fape.core.planning.states.State;
import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.exceptions.FAPEException;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class AbstractTemporalEvent {
    //this event works as an abstraction for further carbon-copying

    /**
     *
     */
    public TemporalEvent event;

    /**
     *
     */
    public TemporalInterval interval;

    /**
     *
     */
    public Reference stateVariableReference;

    /**
     *
     */
    public String varType;

    /**
     *
     * @param event
     * @param interval_
     * @param leftRef
     * @param varType_
     */
    public AbstractTemporalEvent(TemporalEvent event, TemporalInterval interval_, Reference leftRef, String varType_) {
        this.event = event;
        interval = interval_;
        stateVariableReference = leftRef;
        varType = varType_;
    }

    public boolean isTransitionEvent() {
        return this.event instanceof TransitionEvent;
    }

    public boolean isPersistenceEvent() {
        return this.event instanceof PersistenceEvent;
    }

    /**
     * Produces a temporal event whose parameter and time points are
     * binded to those of the action act.
     * @param act
     * @return
     */
    public TemporalEvent GetEventInAction(Action act) {
        TemporalEvent e = this.event.bindedCopy(act);
        interval.AssignTemporalContext(e, act.start, act.end);
        return e;
    }

    @Deprecated
    public boolean SupportsStateVariable(String var_type) {
        //TODO: this is probably wrong, we might have a type that is a
        // descendent, of simply a differenct predicate
        return varType.equals(var_type);
    }

}
