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
import fape.core.execution.model.TemporalConstraint;
import fape.core.planning.constraints.UnificationConstraintSchema;

import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.IUnifiable;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import java.beans.PersistenceDelegate;
import java.util.LinkedList;
import java.util.List;

/**
 * this is an action in the task network, it may be decomposed
 *
 * @author FD
 */
public class Action {

    /**
     *
     */
    public float duration = -1.0f;

    /**
     *
     */
    public TemporalVariable start,
            /**
             *
             */
            end;

    /**
     *
     */
    public String name;
    //public List<ObjectVariable> parameters = new LinkedList<>(); // we should have all the parameters here

    /**
     *
     */
    public List<TemporalEvent> events = new LinkedList<>(); //all variables from the events map to parameters

    /**
     *
     */
    public List<Pair<List<ActionRef>, List<TemporalConstraint>>> refinementOptions; //those are the options how to decompose

    /**
     *
     * @return
     */
    public boolean IsRefinable() {
        return refinementOptions.size() > 0 && decomposition == null;
    }
    List<Action> decomposition; //this is the truly realized decomposition

    /*
     public void AddBindingConstraintsBetweenMyEvents(){
     for(int i = 0; i < events.size(); i++){
     for(int j = i + 1; j < events.size(); j++){
                
     TemporalEvent e1 = events.get(i), e2 = events.get(j);
                
     }
     }
     for(TemporalEvent e1:events){
     for(TemporalEvent e2:events){
     if(e1.objectVar.)
     }
     }
     }*/
    /**
     *
     * @return
     */
    public Action DeepCopy() {
        Action a = new Action();
        if (this.decomposition == null) {
            a.decomposition = null;
        } else {
            a.decomposition = new LinkedList<>();
            for (Action b : this.decomposition) {
                a.decomposition.add(b.DeepCopy());
            }
        }

        a.duration = this.duration;
        a.end = this.end;
        a.events = this.events;
        a.name = this.name;
        a.refinementOptions = this.refinementOptions;
        a.start = this.start;
        return a;
    }

    public IUnifiable GetUnifiableComponent(AbstractAction.SharedParameterStruct get) {
        TemporalEvent e = events.get(get.relativeEventIndex);
        if (get.type == UnificationConstraintSchema.EConType.EVENT) {
            return e.mDatabase;
        } else if (get.type == UnificationConstraintSchema.EConType.FIRST_VALUE && e instanceof TransitionEvent) {
            return ((TransitionEvent) e).from;
        } else if (get.type == UnificationConstraintSchema.EConType.FIRST_VALUE && e instanceof PersistenceEvent) {
            return ((PersistenceEvent) e).value;
        } else if (get.type == UnificationConstraintSchema.EConType.SECOND_VALUE) {
            return ((TransitionEvent) e).from;
        }
        throw new FAPEException("unsupported unification");
    }
}
