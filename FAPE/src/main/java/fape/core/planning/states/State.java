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

import fape.core.execution.model.Reference;
import fape.core.planning.Planner;
import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.model.ObjectVariableValues;
import fape.core.planning.model.ParameterizedStateVariable;
import fape.core.planning.model.VariableRef;
import fape.core.planning.stn.STNManager;
import fape.core.planning.tasknetworks.TaskNetworkManager;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.TemporalDatabaseManager;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.exceptions.FAPEException;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Persistence;
import planstack.anml.model.concrete.statements.Transition;

import java.util.Collection;
import java.util.HashMap;
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

    public final AnmlProblem pb;
    public int problemRevision = -1;


    /**
     * this constructor is only for the initial state!! other states are
     * constructed from from the existing states
     */
    public State(AnmlProblem pb) {
        this.pb = pb;
        tdb = new TemporalDatabaseManager();
        tempoNet = new STNManager();
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
        if (Planner.debugging) {
            st.ExtensiveCheck();
        }
        pb = st.pb;
        problemRevision = st.problemRevision;
        conNet = st.conNet.DeepCopy(); //goes first, since we need to keep track of unifiables
        tempoNet = st.tempoNet.DeepCopy();
        tdb = st.tdb.DeepCopy();
        taskNet = st.taskNet.DeepCopy();

        consumers = new LinkedList<>();
        for (TemporalDatabase sb : st.consumers) {
            consumers.add(this.GetDatabase(sb.mID));
        }
        if (Planner.debugging) {
            this.ExtensiveCheck();
        }

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
        float distance = this.consumers.size();
        return distance;
    }

    public String Report() {
        String ret = "";
        ret += "{\n";
        ret += "  state[" + mID + "]\n";
        ret += "  cons: " + conNet.Report() + "\n";
        //ret += "  stn: " + this.tempoNet.Report() + "\n";
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

    public TemporalDatabase GetDatabase(int temporalDatabase) {
        for (TemporalDatabase db : tdb.vars) {
            if (db.mID == temporalDatabase) {
                return db;
            }
        }
        throw new FAPEException("Reference to unknown database.");
    }

    public void FailAction(Integer pop) {
        taskNet.FailAction(pop);
    }

    public void SplitDatabase(LogStatement s) {
        TemporalDatabase theDatabase = tdb.getDBContaining(s);

        // First find which component contains s
        ChainComponent comp = null; // component containing the statement
        int ct = 0; // index of the component in the chain
        for(ct=0 ; ct<theDatabase.chain.size() ; ct++) {
            if(theDatabase.chain.get(ct).contains(s)) {
                comp = theDatabase.chain.get(ct);
                break;
            }
        }

        assert comp != null && theDatabase.chain.get(ct) == comp;

        if (s instanceof Transition) {
            if (ct + 1 < theDatabase.chain.size()) {
                //this was not the last element, we need to create another database and make split
                
                // the two databases share the same state variable
                TemporalDatabase newDB = new TemporalDatabase(theDatabase.stateVariable);

                //add all extra chain components to the new database
                List<ChainComponent> remove = new LinkedList<>();
                for (int i = ct + 1; i < theDatabase.chain.size(); i++) {
                    ChainComponent origComp = theDatabase.chain.get(i);
                    remove.add(origComp);
                    ChainComponent pc = origComp.DeepCopy();
                    newDB.chain.add(pc);
                }
                this.consumers.add(newDB);
                this.tdb.vars.add(newDB);
                theDatabase.chain.remove(comp);
                theDatabase.chain.removeAll(remove);
            } else {
                assert comp.contents.size() == 1;
                //this was the last element so we can just remove it and we are done
                theDatabase.chain.remove(comp);
            }

        } else if (s instanceof Persistence) {
            if (comp.contents.size() == 1) {
                // only one statement, remove the whole component
                theDatabase.chain.remove(comp);
            } else {
                // more than one statement, remove only this statement
                comp.contents.remove(s);
            }
        } else {
            throw new FAPEException("Unknown event type.");
        }
    }

    /**
     * Return all possible values of an object variable.
     * @param var
     * @return
     */
//    public Collection<String> possibleValues(VariableRef var) {
//        if(parameterBindings.containsKey(var.var)) {
//            // variable is binded, return domain
//            return parameterBindings.get(var.var).domain;
//        } else {
//            // variable is unkonwn, return all instances of its type
//            return pb.types.instances(var.type);
//        }
//    }

//    public boolean Unifiable(TemporalDatabase a, TemporalDatabase b) {
//        return Unifiable(a.stateVariable, b.stateVariable);
//    }

    /**
     * Returns true if two state variables are unifiable (ie: they are on the same predicate
     * and their variables are unifiable).
     * @param a
     * @param b
     * @return
     */
//    public boolean Unifiable(ParameterizedStateVariable a, ParameterizedStateVariable b) {
//        if(a.predicateName.equals(b.predicateName)) {
//            return Unifiable(a.variable, b.variable);
//        } else {
//            return false;
//        }
//    }

    /**
     * Return true if the two variables are unifiable (ie: share at least one value)
     * @param a
     * @param b
     * @return
     */
//    public boolean Unifiable(VariableRef a, VariableRef b) {
//        LinkedList<String> inter = new LinkedList<>(possibleValues(a));
//
//        inter.retainAll(possibleValues(b));
//        return inter.size() > 0;
//    }

    /**
     * Returns true if the temporal event e can be an enabler for the database db.
     *
     * Its means e has to be a transition event and that both state variables and the
     * consume/produce values must be unifiable.
     * @param e
     * @param db
     * @return
     */
//    public boolean canBeEnabler(TemporalEvent e, TemporalDatabase db) {
//        boolean canSupport = e instanceof TransitionEvent;
//        canSupport = canSupport && Unifiable(e.stateVariable, db.stateVariable);
//        canSupport = canSupport && Unifiable(e.endValue(), db.GetGlobalConsumeValue());
//        return canSupport;
//    }

    /**
     * This method is to be used while debugging to make sure the state is consistent
     */
    public void ExtensiveCheck() {
        if(!Planner.debugging) {
            throw new FAPEException("Those checks are very expensive and shouldn't be done while not in debugging mode");
        }
        for(TemporalDatabase db : this.tdb.vars) {
            db.CheckChainComposition();
        }
    }
}
