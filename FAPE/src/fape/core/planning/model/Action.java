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
import fape.core.planning.bindings.ObjectVariable;
import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.util.Pair;
import java.util.LinkedList;
import java.util.List;

/**
 * this is an action in the task network, it may be decomposed
 * @author FD
 */
public class Action {
    public float duration = -1.0f;
    public TemporalVariable start, end;
    public String name;
    //public List<ObjectVariable> parameters = new LinkedList<>(); // we should have all the parameters here
    public List<TemporalEvent> events = new LinkedList<>(); //all variables from the events map to parameters
    public List<Pair<List<ActionRef>, List<TemporalConstraint>>> refinementOptions;
    public boolean IsRefinable(){
        return refinementOptions != null;
    }
    List<Action> decomposition;
    
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
}
