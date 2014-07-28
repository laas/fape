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

import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import fape.util.Reporter;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class TemporalDatabaseManager implements Reporter {


    /**
     * All temporal databases.
     */
    public List<TemporalDatabase> vars = new LinkedList<>();

    /**
     * Creates a new database containing the Statement s and adds it to this Manager
     *
     * @param s Statement that will appear in the the Database
     * @return
     */
    public TemporalDatabase GetNewDatabase(LogStatement s) {
        TemporalDatabase db = new TemporalDatabase(s);
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

    public TemporalDatabase getDBContaining(LogStatement s) {
        for(TemporalDatabase db : vars) {
            if(db.contains(s)) {
                return db;
            }
        }
        throw new FAPEException("Unable to find a temporal database containing the statement "+s);
    }

    public TemporalDatabaseManager DeepCopy() {
        TemporalDatabaseManager mng = new TemporalDatabaseManager();
        mng.vars = new LinkedList<>();
        for (TemporalDatabase b : this.vars) {
            TemporalDatabase db = b.DeepCopy();
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
    public void InsertDatabaseAfter(State st, TemporalDatabase tdb, TemporalDatabase included, ChainComponent after) {
        assert tdb.chain.size() != 0;
        assert tdb.chain.contains(after);

        int afterIndex = tdb.chain.indexOf(after);

        if(afterIndex+1 < tdb.chain.size() && !tdb.chain.get(afterIndex+1).change) {
            // we were about to perform the insertion just before a persistence event.
            // instead, we make the insertion after the persistence
            afterIndex = afterIndex + 1;
            after = tdb.chain.get(afterIndex);
        }

        if(!included.chain.getFirst().change && !after.change) {
            //  'after' and first ChainComp of 'included'  are both persistence events. We merge them
            // into 'after' before going any further.
            st.conNet.AddUnificationConstraint(after.GetSupportValue(), included.GetGlobalConsumeValue());

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
            for (ChainComponent c : included.chain) {
                tdb.chain.add(nextInclusion, c);
                nextInclusion += 1;
            }

            // add connstraints before and after the inserted chain
            EnforceChainConstraints(st, tdb, afterIndex);
            EnforceChainConstraints(st, tdb, nextInclusion-1);
        }

        enforceAllConstraints(st, tdb);

        // the new domain is the intersection of both domains
        st.conNet.AddUnificationConstraint(tdb.stateVariable, included.stateVariable);

        // Remove the included database from the system
        st.tdb.vars.remove(included);
        st.consumers.remove(included);
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
            ChainComponent first = tdb.chain.get(chainCompIndex);
            ChainComponent second = tdb.chain.get(chainCompIndex + 1);

            assert first.change || second.change : "There should not be two persistence following each other";

            st.conNet.AddUnificationConstraint(first.GetSupportValue(), second.GetConsumeValue());

            // Enforce all statements of first to be before all statements of second
            for(LogStatement sa : first.contents) {
                for(LogStatement sb : second.contents) {
                    st.enforceBefore(sa.end(), sb.start());
                }
            }
        }
    }

    public void enforceAllConstraints(State st, TemporalDatabase tdb) {
        for(int i=0 ; i<tdb.chain.size()-1 ; i++) {
            //EnforceChainConstraints(st, tdb, i);
            int j = i+1;
            //for(int j=i+1 ; j<tdb.chain.size() ; j++) {
                for(LogStatement a : tdb.chain.get(i).contents) {
                    for(LogStatement b : tdb.chain.get(j).contents) {
                        st.enforceBefore(a.end(), b.start());
                    }
                }
            //}

        }
    }

    @Override
    public String Report() {
        String ret = "";

        ret += "  size: " + this.vars.size() + "\n";
        for (TemporalDatabase b : vars) {
            ret += b.Report();
        }
        ret += "\n";

        return ret;

    }
}
