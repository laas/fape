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
import fape.planning.causalities.CausalNetworkManager;
import fape.planning.constraints.ConstraintNetworkManager;
import fape.planning.stn.STNManager;
import fape.planning.tasknetworks.TaskNetworkManager;
import fape.planning.temporaldatabases.TemporalDatabaseManager;

/**
 *
 * @author FD
 */
public class State {

    TemporalDatabaseManager tdb;
    STNManager tempoNet;
    TaskNetworkManager taskNet;
    ConstraintNetworkManager conNet;
    BindingManager bindings;
    CausalNetworkManager causalNet;

    /**
     * this constructor is only for the initial state!! other states are constructed
     * from from the existing states
     */
    public State(){
        tempoNet = new STNManager();
        taskNet = new TaskNetworkManager();
        conNet = new ConstraintNetworkManager();
        bindings = new BindingManager();
        causalNet = new CausalNetworkManager();
    }
}
