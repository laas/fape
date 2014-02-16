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
package fape.core.planning.temporaldatabases;

import fape.core.planning.Planner;
import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.exceptions.FAPEException;

import java.util.*;

/**
 *
 * @author FD
 */
public class TemporalDatabaseManager {

    /**
     *
     */
    public List<TemporalDatabase> vars = new LinkedList<>();


    /**
     *
     * @param m
     * @return
     */
    public TemporalDatabase GetNewDatabase(ConstraintNetworkManager m) {
        TemporalDatabase db = new TemporalDatabase(true);
        vars.add(db);
        return db;
    }

    public TemporalDatabase GetDB(int tdbID) {
        for(TemporalDatabase db : vars) {
            if(db.mID == tdbID)
                return db;
        }
        throw new FAPEException("DB with id "+tdbID+" does not appears in vars \n"+Report());
    }

    /**
     *
     * @param m
     * @return
     */
    public TemporalDatabaseManager DeepCopy(ConstraintNetworkManager m) {
        TemporalDatabaseManager mng = new TemporalDatabaseManager();
        mng.vars = new LinkedList<>();
        for (TemporalDatabase b : this.vars) {
            TemporalDatabase db = b.DeepCopy(m);
            mng.vars.add(db);
        }
        return mng;
    }

    /**
     * Inserts all chains of the database @included after the ChainComponent @after of database @tdb.
     * All necessary constraints (temporal precedence and unification) are added to State st.
     *
     * If the both the first chain component of @included and the chain component @after are persistence events, those
     * two are merged before proceeding to the insertion.
     *
     * At the the included database is completely remove from the search state. All reference to it are replaced by references to tdb
     *
     * @param st State in which the changes are applied
     * @param tdb Temporal database in which the events will we included
     * @param included database that will disappear, all of its components being included in tdb
     * @param after a chain component of tdb after which the chain of included will be added
     */
    public void InsertDatabaseAfter(State st, TemporalDatabase tdb, TemporalDatabase included, TemporalDatabase.ChainComponent after) {
        assert tdb.chain.size() != 0;
        assert tdb.chain.contains(after);

        int afterIndex = tdb.chain.indexOf(after);

        // bind all events in included to their new database
        for (TemporalDatabase.ChainComponent comp : included.chain) {
            for (TemporalEvent e : comp.contents) {
                e.tdbID = tdb.mID;
            }
        }

        if(afterIndex+1 < tdb.chain.size() && !tdb.chain.get(afterIndex+1).change) {
            // we were about to perform the insertion just before a persistence event.
            // instead, we make the insertion after the persitence
            afterIndex = afterIndex + 1;
            after = tdb.chain.get(afterIndex);
        }

        if(!included.chain.getFirst().change && !after.change) {
            //  'after' and first ChainComp of 'included'  are both persistence events. We merge them
            // into 'after' before going any further.
            st.conNet.AddUnificationConstraint(st, after.GetSupportValue(), included.GetGlobalConsumeValue());

            // add persitence events to after, and removing them from the included database
            after.Add(included.chain.getFirst());
            included.chain.removeFirst();

            EnforceChainConstraints(st, tdb, afterIndex-1);
            EnforceChainConstraints(st, tdb, afterIndex);
        }

        // copy all remaining components
        if(included.chain.size() > 0) {
            assert tdb.chain.getLast() == after || included.chain.size() == 1 && !included.chain.getFirst().change:
                    "Integrating a database with transitions in the middle of another one. " +
                    "While this should work with the current implementation it might create unexpected problems " +
                    "because of unexpected unification constraints between two non adjacent chain components.";

            int nextInclusion = afterIndex + 1;
            for (TemporalDatabase.ChainComponent c : included.chain) {
                tdb.chain.add(nextInclusion, c);
                nextInclusion += 1;
            }

            // add connstraints before and after the inserted chain
            EnforceChainConstraints(st, tdb, afterIndex);
            EnforceChainConstraints(st, tdb, nextInclusion-1);
        }

        // the new domain is the intersection of both domains
        st.conNet.AddUnificationConstraint(st, tdb.stateVariable, included.stateVariable);

        // all actions that pointed to events in included should now point to the containing tdb
        tdb.actionAssociations.putAll(included.actionAssociations);

        // Remove the included database from the system
        st.tdb.vars.remove(included);
        st.consumers.remove(included);

        if(Planner.debugging) {
            st.ExtensiveCheck();
        }
    }

    /**
     * Given a database tdb, enforces the unification and temporal constraints between
     * the elements of indexes chainCompIndex and chainCompIndex+1
     * @param st
     * @param tdb
     * @param chainCompIndex
     */
    public void EnforceChainConstraints(State st, TemporalDatabase tdb, int chainCompIndex) {
        assert chainCompIndex < tdb.chain.size();

        if(chainCompIndex < tdb.chain.size()-1 && chainCompIndex >= 0) {
            // if we are not already the last element of the chain, we add constraints between
            // the component and its direct follower.
            TemporalDatabase.ChainComponent first = tdb.chain.get(chainCompIndex);
            TemporalDatabase.ChainComponent second = tdb.chain.get(chainCompIndex + 1);

            assert first.change || second.change : "There should not be two persistence following each other";

            st.conNet.AddUnificationConstraint(st, first.GetSupportValue(), second.GetConsumeValue());
            TemporalDatabase.PropagatePrecedence(first, second, st);
        }
    }

    public String Report() {
        String ret = "";

        ret += "  size: " + this.vars.size() + "\n";
        for (TemporalDatabase b : vars) {
            ret += b.Report();
        }
        ret += "\n";

        return ret;

    }

    /**
     * Returns all temporal events contained in all temporal databases.
     * @return
     */
    public List<TemporalEvent> AllEvents() {
        LinkedList<TemporalEvent> events = new LinkedList<>();
        for(TemporalDatabase db : vars) {
            for(TemporalDatabase.ChainComponent comp : db.chain) {
                for(TemporalEvent ev : comp.contents) {
                    events.add(ev);
                }
            }
        }
        return events;
    }
/*
    public void SplitDatabase(TemporalEvent t) {
        TemporalDatabase theDatabase = t.mDatabase;
        if (t instanceof TransitionEvent) {
            int ct = 0;
            for (TemporalDatabase.ChainComponent comp : theDatabase.chain) {
                if (comp.contents.getFirst().mID == t.mID) {
                    TemporalDatabase one = theDatabase;
                    if (ct + 1 < theDatabase.chain.size()) {
                        //this was not the last element, we need to create another database and make split
                        GetNewDatabase()
                                
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
            if (theComponent.contents.size() == 1) {
                theDatabase.chain.remove(theComponent);
            } else {
                theComponent.contents.remove(theEvent);
            }
        } else {
            throw new FAPEException("Unknown event type.");
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }*/
}
