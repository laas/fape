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
package fape.core.planning.states;

import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.stn.STNManager;
import fape.core.planning.tasknetworks.TaskNetworkManager;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.TemporalDatabaseManager;
import fape.exceptions.FAPEException;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class State {

    public static int idCounter = 0;
    public int mID = idCounter++;
    /**
     *
     */
    public TemporalDatabaseManager tdb;

    public void RemoveDatabase(TemporalDatabase d) {
        for (TemporalDatabase db : tdb.vars) {
            if (db.mID == d.mID) {
                tdb.vars.remove(db);
                return;
            }
        }
    }

    /**
     *
     */
    public STNManager tempoNet;

    /**
     *
     */
    public TaskNetworkManager taskNet;

    /**
     *
     */
    public List<TemporalDatabase> consumers;
    public ConstraintNetworkManager conNet;
    //public BindingManager bindings;
    //public CausalNetworkManager causalNet;

    /**
     *
     */
    public boolean isInitState = false;

    /**
     * this constructor is only for the initial state!! other states are
     * constructed from from the existing states
     */
    public State() {
        isInitState = true;
        tdb = new TemporalDatabaseManager();
        tempoNet = new STNManager();
        tempoNet.Init();
        taskNet = new TaskNetworkManager();
        consumers = new LinkedList<>();
        conNet = new ConstraintNetworkManager();
        //bindings = new BindingManager();
        //causalNet = new CausalNetworkManager();
    }

    /**
     *
     * @param st
     */
    public State(State st) {
        st.conNet.CheckConsistency();
        conNet = st.conNet.DeepCopy(); //goes first, since we need to keep track of unifiables
        taskNet = st.taskNet.DeepCopy();
        tdb = st.tdb.DeepCopy(conNet); //we send the new conNet, so we can create a new mapping of unifiables
        tempoNet = st.tempoNet.DeepCopy();
        consumers = new LinkedList<>();
        for (TemporalDatabase sb : st.consumers) {
            consumers.add((TemporalDatabase) conNet.objectMapper.get(sb.GetUniqueID()));
        }
        conNet.CheckConsistency();
    }

    /**
     *
     * @return
     */
    public float GetCurrentCost() {
        float costs = this.taskNet.GetActionCosts();
        return costs;
    }

    /**
     *
     * @return
     */
    public float GetGoalDistance() {
        float distance = this.consumers.size() * 2;
        return distance;
    }

    public String Report() {
        String ret = "";
        ret += "{\n";
        ret += "  state[" + mID + "]\n";
        ret += "  cons: " + conNet.Report() + "\n";
        ret += "  stn: " + this.tempoNet.Report() + "\n";
        ret += "  consumers: " + this.consumers.size() + "\n";
        for (TemporalDatabase b : consumers) {
            ret += b.Report();
        }
        ret += "\n";
        ret += "  tasks: " + this.taskNet.Report() + "\n";
        //ret += "  databases: "+this.tdb.Report()+"\n";

        ret += "}\n";

        return ret;
    }

    /**
     * we need to track the databases by their ids, not the object references, this method creates the mapping
     *
     * @param value1
     * @return
     */
    public TemporalDatabase GetConsumer(TemporalDatabase value1) {
        for (TemporalDatabase db : consumers) {
            if (db.mID == value1.mID) {
                return db;
            }
        }
        throw new FAPEException("Consumer id mismatch.");
    }

    public TemporalDatabase GetDatabase(int temporalDatabase) {
        for(TemporalDatabase db:tdb.vars){
            if(db.mID == temporalDatabase){
                return db;
            }
        }
        throw new FAPEException("Reference to unknown database.");
    }
}
