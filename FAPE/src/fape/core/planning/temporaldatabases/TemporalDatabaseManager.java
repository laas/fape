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
        m.AddUnifiable(db);
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
            m.AddUnifiable(db); //keep the index
            mng.vars.add(db);
        }
        return mng;
    }

    /**
     *
     * @param st
     * @param tdb
     * @param consumer
     */
    public void Merge(State st, TemporalDatabase tdb, TemporalDatabase consumer) {
        st.StrongCheck();

        if(!st.tdb.vars.contains(consumer))
            throw new FAPEException("Something is strange the consumer tdb is not in vars");
        System.err.println("MERGING BEFORE");
        System.err.println(Report());
        // merging consumer into tdb, which means removing all the references for consumer from the system and replacing them with tdb
        // also intersecting the domains
        tdb.domain.retainAll(consumer.domain);

        if(consumer.mID == 15) {
            int i = 0;
        }

        for (TemporalDatabase.ChainComponent comp : tdb.chain) {
            for (TemporalEvent e : comp.contents) {
                e.tdbID = tdb.mID;
            }
        }

        tdb.actionAssociations.putAll(consumer.actionAssociations);

        //propagate merge into the constraints
        st.conNet.Merge(tdb, consumer);
        st.tdb.vars.remove(consumer);

        System.err.println("MERGING AFTER");
        System.err.println(Report());

        st.StrongCheck();
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
