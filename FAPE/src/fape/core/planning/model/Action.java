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

import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.IUnifiable;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import java.util.LinkedList;
import java.util.List;

/**
 * this is an action in the task network, it may be decomposed
 *
 * @author FD
 */
public class Action {

    public static int idCounter = 0;
    public int mID = idCounter++;

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
    public List<Instance> params;

    /**
     *
     * @return
     */
    public boolean IsRefinable() {
        return refinementOptions.size() > 0 && decomposition == null;
    }
    public List<Action> decomposition; //this is the truly realized decomposition

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
        a.mID = mID;
        a.params = this.params;
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
            return ((TransitionEvent) e).to;
        }
        throw new FAPEException("unsupported unification");
    }

    @Override
    public String toString() {
        return name;
    }

    public float GetCost() {
        return 1.0f;
    }

    public List<String> ProduceParameters(State st) {
        List<String> ret = new LinkedList<>();
        String foundConstantValue = "";
        
        for (Instance i : this.params) {
            StateVariableValue val = null;
            //first search databases
            for (TemporalDatabase db : st.tdb.vars) {
                String param = db.actionAssociations.get(mID);
                if (param != null && param.equals(i.name)) {
                    StateVariable sv = db.domain.getFirst();
                    foundConstantValue = sv.name.split("\\.")[0];
                }
            }
            //then search event values
            for (TemporalEvent ev : this.events) {
                if (ev instanceof TransitionEvent) {
                    String fromVal = ((TransitionEvent) ev).from.valueDescription.split("\\.")[0];
                    String toVal = ((TransitionEvent) ev).to.valueDescription.split("\\.")[0];
                    if (fromVal.equals(i.name)) {
                        val = ((TransitionEvent) ev).from;
                    }
                    if (toVal.equals(i.name)) {
                        val = ((TransitionEvent) ev).to;
                    }
                } else if (ev instanceof PersistenceEvent) {
                    String value = ((PersistenceEvent) ev).value.valueDescription.split("\\.")[0];
                    if (value.equals(i.name)) {
                        val = ((PersistenceEvent) ev).value;
                    }
                }
            }
            if(val != null){
                StateVariableValue ev = (StateVariableValue)st.conNet.objectMapper.get(val.mID);
                foundConstantValue = ev.values.get(0);
            }
            //get the most recent version of the value
            
            if (foundConstantValue.equals("")) {
                throw new FAPEException("Cannot discover parameter value.");
            }
            ret.add(foundConstantValue);
        }
        return ret;
    }

}
