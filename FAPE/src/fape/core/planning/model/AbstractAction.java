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

    public class SharedParameterStruct {

        public UnificationConstraintSchema.EConType type;
        public int relativeEventIndex;

        public SharedParameterStruct(int index, UnificationConstraintSchema.EConType tp) {
            type = tp;
            relativeEventIndex = index;
        }
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
     * maps parametr static variable into relative index of an event itself or
     * one of its values
     */
    public List<List<SharedParameterStruct>> param2Event;

    public void MapParametersToEvents() {
        //int cnt = 0;
        for (Instance i : params) {
            List<SharedParameterStruct> l = new LinkedList<>();
            //now check all events and its values, if they use the given parameter
            int eventCount = 0;
            for (AbstractTemporalEvent ae : events) {
                if (i.name.equals(ae.stateVariableReference.refs.getFirst())) {
                    //sharing the event itself
                    l.add(new SharedParameterStruct(eventCount, UnificationConstraintSchema.EConType.EVENT));
                }
                if(ae.event instanceof PersistenceEvent && ((PersistenceEvent)ae.event).value.GetObjectParameter().equals(i.name)){
                    l.add(new SharedParameterStruct(eventCount, UnificationConstraintSchema.EConType.FIRST_VALUE));
                }
                if(ae.event instanceof TransitionEvent && ((TransitionEvent)ae.event).from.GetObjectParameter().equals(i.name)){
                    l.add(new SharedParameterStruct(eventCount, UnificationConstraintSchema.EConType.FIRST_VALUE));
                }
                if(ae.event instanceof TransitionEvent && ((TransitionEvent)ae.event).to.GetObjectParameter().equals(i.name)){
                    l.add(new SharedParameterStruct(eventCount, UnificationConstraintSchema.EConType.SECOND_VALUE));
                }
                eventCount++;
            }
            param2Event.add(l);
            //cnt++;
        }
    }
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
        return 10.0f;
    }

    /**
     * we use relative references here .. if they share the same variable, they
     * are tied together by the same predecesor constraint
     *
     * @return
     */
    /*public List<Pair<Integer, Integer>> GetLocalBindings() {
        if (localBindings == null) {
            localBindings = new LinkedList<>();
            for (int i = 0; i < events.size(); i++) {
                for (int j = i + 1; j < events.size(); j++) {
                    AbstractTemporalEvent e1 = events.get(i), e2 = events.get(j);
                    if (e1.stateVariableReference.refs.getFirst().equals(e2.stateVariableReference.refs.getFirst())) {
                        localBindings.add(new Pair(i, j));
                    }
                }
            }
        }
        return localBindings;
    }*/

    @Override
    public String toString() {
        return name;
    }
}
