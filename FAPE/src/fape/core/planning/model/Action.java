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
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.util.Pair;
import java.util.LinkedList;
import java.util.List;

/**
 * this is an action in the task network, it may be decomposed
 * @author FD
 */
public class Action {
    String name;
    List<ObjectVariable> parameters = new LinkedList<>(); // we should have all the parameters here
    List<TemporalEvent> events; //all variables from the events map to parameters
    List<Pair<List<ActionRef>, List<TemporalConstraint>>> refinements;
    public boolean IsRefinable(){
        return refinements != null;
    }
    List<Action> decomposition;
}
