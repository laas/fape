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

import fape.core.planning.Planner;
import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.model.StateVariable;
import fape.core.planning.stn.STNManager;
import fape.core.planning.tasknetworks.TaskNetworkManager;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.TemporalDatabaseManager;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
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
        tempoNet = STNManager.newInstance();
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
        if (Planner.debugging) {
            st.ExtensiveCheck();
            st.StrongCheck();
        }
        conNet = st.conNet.DeepCopy(); //goes first, since we need to keep track of unifiables
        tempoNet = st.tempoNet.DeepCopy();
        tdb = st.tdb.DeepCopy(conNet); //we send the new conNet, so we can create a new mapping of unifiables

        // copy the task network and updates the events pointers of actions to the newly cloned ones.
        taskNet = st.taskNet.DeepCopy(tdb.AllEvents());

        // remove constraints on objects that don't exist anymore
        conNet.RemoveOutdatedConstraints();

        consumers = new LinkedList<>();
        for (TemporalDatabase sb : st.consumers) {
            consumers.add((TemporalDatabase) conNet.objectMapper.get(sb.GetUniqueID()));
        }
        if (Planner.debugging) {
            this.ExtensiveCheck();
            try {
                this.StrongCheck();
            } catch (FAPEException e) {
                System.out.println("BREAK");
            }
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
        return distance * 3;
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

    /**
     * we need to track the databases by their ids, not the object references,
     * this method creates the mapping
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

    public void SplitDatabase(TemporalEvent t) {
        TemporalDatabase theDatabase = tdb.GetDB(t.tdbID);
        if (t instanceof TransitionEvent) {
            int ct = 0;
            for (TemporalDatabase.ChainComponent comp : theDatabase.chain) {
                if (comp.contents.getFirst().mID == t.mID) {
                    TemporalDatabase one = theDatabase;
                    if (ct + 1 < theDatabase.chain.size()) {
                        //this was not the last element, we need to create another database and make split
                        TemporalDatabase newDB = tdb.GetNewDatabase(conNet);
                        for (StateVariable var : theDatabase.domain) {
                            newDB.domain.add(var);
                        }
                        //add all extra chain components to the new database
                        List<TemporalDatabase.ChainComponent> remove = new LinkedList<>();
                        for (int i = ct + 1; i < theDatabase.chain.size(); i++) {
                            TemporalDatabase.ChainComponent origComp = theDatabase.chain.get(i);
                            remove.add(origComp);
                            TemporalDatabase.ChainComponent pc = origComp.DeepCopy(conNet);
                            newDB.chain.add(pc);
                            for (TemporalEvent eve : pc.contents) {
                                eve.tdbID = newDB.mID;
                            }
                        }
                        this.consumers.add(newDB);
                        this.tdb.vars.add(newDB);                        
                        theDatabase.chain.remove(comp);
                        theDatabase.chain.removeAll(remove);
                        break;
                    } else {
                        //this was the last element so we can just remove it and we are done
                        theDatabase.chain.remove(comp);
                        break;
                    }
                }
                ct++;
            }
        } else if (t instanceof PersistenceEvent) {
            TemporalDatabase.ChainComponent theComponent = null;
            TemporalEvent theEvent = null;
            for (TemporalDatabase.ChainComponent comp : theDatabase.chain) {
                for (TemporalEvent e : comp.contents) {
                    if (e.mID == t.mID) {
                        theComponent = comp;
                        theEvent = e;
                    }
                }
            }
            if (theComponent == null) {
                throw new FAPEException("Unknown event.");
            } else if (theComponent.contents.size() == 1) {
                theDatabase.chain.remove(theComponent);
            } else {
                theComponent.contents.remove(theEvent);
            }
        } else {
            throw new FAPEException("Unknown event type.");
        }
    }

    public void StrongCheck() {
        this.taskNet.CheckEventDBBindings(this);
    }

    /**
     * This method is to be used while debugging to make sure the state is consistent
     */
    public void ExtensiveCheck() {
        if(!Planner.debugging) {
            throw new FAPEException("Those checks are very expensive and shouldn't be done while not in debugging mode");
        }
        this.conNet.CheckConsistency();
        this.tempoNet.TestConsistent();
        this.taskNet.CheckEventDBBindings(this);
    }
}
