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
package fape.planning.states;

import fape.planning.bindings.BindingManager;
import fape.planning.causalities.CausalNetwork;
import fape.planning.constraints.ConstraintNetwork;
import fape.planning.stn.STN;
import fape.planning.tasknetworks.TaskNetwork;
import fape.planning.temporaldatabases.TemporalDatabase;

/**
 *
 * @author FD
 */
public class State {

    TemporalDatabase tdb;
    STN tempoNet;
    TaskNetwork taskNet;
    ConstraintNetwork conNet;
    BindingManager bindings;
    CausalNetwork causalNet;

    public State(){
        tempoNet = new STN();
        taskNet = new TaskNetwork();
        conNet = new ConstraintNetwork();
        bindings = new BindingManager();
        causalNet = new CausalNetwork();
    }
}
