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

import fape.core.planning.search.SupportOption;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.util.Pair;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class TemporalDatabaseManager {

    List<Pair<Integer, Integer>> unificationConstraints = new LinkedList<>();

    public List<TemporalDatabase> vars = new LinkedList<>();

    public TemporalDatabase GetNewDatabase() {
        TemporalDatabase db = new TemporalDatabase();
        vars.add(db);
        return db;
    }

    /**
     * propagates the necessary unification constraints
     *
     * @param st
     * @return 
     */
    public boolean PropagateAndCheckConsistency(State st) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void AddUnificationConstraint(TemporalDatabase a, TemporalDatabase b){
        unificationConstraints.add(new Pair(a.mID,b.mID));
    }

    public TemporalDatabaseManager DeepCopy() {
        TemporalDatabaseManager mng = new TemporalDatabaseManager();
        mng.vars = new LinkedList<>();
        for(TemporalDatabase b:this.vars){
            mng.vars.add(b.DeepCopy());
        }
        return mng;
    }

    public void Merge(TemporalDatabase tdb, TemporalDatabase consumer) {
        // merging consumer into tdb, which means removing all the references for consumer from the system and replacing them with tdb
        // also intersecting the domains
        tdb.domain.retainAll(consumer.domain);
        
        for(TemporalDatabase.ChainComponent comp:tdb.chain){
            for(TemporalEvent e:comp.contents){
                e.mDatabase = tdb;
            }
        }
        
        for(Pair<Integer,Integer> p :unificationConstraints){
            if(p.value1 == consumer.mID){
                p.value1 = tdb.mID;
            }
            if(p.value2 == consumer.mID){
                p.value2 = tdb.mID;
            }
        }
        vars.remove(consumer);
    }

    

}
