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

    /*class UnificationConstraint {

        
    }*/

    

    /**
     *
     */
    public List<TemporalDatabase> vars = new LinkedList<>();

    /**
     *
     * @return
     */
    public TemporalDatabase GetNewDatabase() {
        TemporalDatabase db = new TemporalDatabase(true);
        vars.add(db);
        return db;
    }

    



    /**
     *
     * @return
     */
    public TemporalDatabaseManager DeepCopy() {
        TemporalDatabaseManager mng = new TemporalDatabaseManager();
        mng.vars = new LinkedList<>();
        for (TemporalDatabase b : this.vars) {
            mng.vars.add(b.DeepCopy());
        }
        return mng;
    }

    /**
     *
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
        
        //propagate merge into the constraints
        st.conNet.Merge(tdb, consumer);
        
    }
}
