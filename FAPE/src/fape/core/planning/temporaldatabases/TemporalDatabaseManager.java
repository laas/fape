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

import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.model.StateVariable;
import fape.core.planning.search.SupportOption;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.util.Pair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

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
        // merging consumer into tdb, which means removing all the references for consumer from the system and replacing them with tdb
        // also intersecting the domains
        tdb.domain.retainAll(consumer.domain);

        for (TemporalDatabase.ChainComponent comp : tdb.chain) {
            for (TemporalEvent e : comp.contents) {
                e.mDatabase = tdb;
            }
        }
        
        tdb.actionAssociations.putAll(consumer.actionAssociations);

        //propagate merge into the constraints
        st.conNet.Merge(tdb, consumer);
        st.tdb.vars.remove(consumer);

    }

    public String Report() {
        String ret = "";

        ret += "  size: " + this.vars.size() + "\n";
        for(TemporalDatabase b:vars){
            ret += b.Report();
        }
        ret += "\n";
        
        return ret;

    }
}
