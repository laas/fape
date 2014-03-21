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

import fape.core.planning.states.State;
import planstack.anml.model.concrete.*;

import java.util.LinkedList;
import java.util.List;

/**
 * An atomic action is a representation of an action to be carried out by an actor.
 * Its parameters are ground (refer to instances of the domain) and has a start time and expected duration.
 */
public class AtomicAction {


    /**
     * Creates a new actomic action.
     * @param action The concrete action to serve as a base.
     * @param startTime The start time of the action.
     * @param duration The expected duration of the action.
     * @param st State in which the action appears. It is used to translate global variables to actual problem instances.
     */
    public AtomicAction(Action action, long startTime, int duration, State st) {
        id = action.id();
        name = action.name();
        mStartTime = startTime;
        this.duration = duration;
        params = new LinkedList<>();
        for(VarRef arg : action.jArgs()) {
            String[] possibleValues = (String[]) st.conNet.domainOf(arg).toArray();
            assert possibleValues.length == 1 : "Argument "+arg+" of action "+action+" has more than one possible value.";
            params.add(possibleValues[0]);
        }
    }
    /*
    private static int idCounter = 0;
    public int mID = idCounter++;*/
    
    public enum EResult{
        SUCCESS, FAILURE
    }

    /**
     * Time at which the action execution has to start.
     */
    public final long mStartTime;

    /**
     * Reference to the concrete action in the plan.
     */
    public final ActRef id;

    /**
     * Expected duration of the action. If unknown, it should the maximum expected duration.
     */
    public final int duration;

    /**
     * Name of the action.
     */
    public final String name;

    /**
     * Parameters of the action in the form of domain instances.
     */
    public final List<String> params;

    /**
     * @return A human readable string representing the action.
     */
    public String GetDescription(){
        String ret = "";
        
        ret += "("+name;
        for(String st:params){
            ret += " "+st;
        }
        ret += ")";        
        return ret;
    }

    @Override
    public String toString() {
        return GetDescription();
    }
}
