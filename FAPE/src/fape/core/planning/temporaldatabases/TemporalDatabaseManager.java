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

    class UnificationConstraint {

        TemporalDatabase one, two;

        @Override
        public boolean equals(Object obj) {
            UnificationConstraint u = (UnificationConstraint) obj;
            return (u.one == one && u.two == two) || (u.one == two && u.two == one);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + this.one.mID + this.two.mID;
            return hash;
        }

        public UnificationConstraint(TemporalDatabase f, TemporalDatabase s) {
            one = f;
            two = s;
        }
    }

    HashSet<UnificationConstraint> unificationConstraints = new HashSet<>(); // database id -> constraints on that db with db with smaller id

    /**
     *
     */
    public List<TemporalDatabase> vars = new LinkedList<>();

    /**
     *
     * @return
     */
    public TemporalDatabase GetNewDatabase() {
        TemporalDatabase db = new TemporalDatabase();
        vars.add(db);
        return db;
    }

    boolean AC3(HashSet<UnificationConstraint> set) {
        HashMap<Integer, List<UnificationConstraint>> smartList = new HashMap<>();
        for (UnificationConstraint u : set) {
            if (!smartList.containsKey(u.one.mID)) {
                smartList.put(u.one.mID, new LinkedList<UnificationConstraint>());
            }
            smartList.get(u.one.mID).add(u);
            if (!smartList.containsKey(u.two.mID)) {
                smartList.put(u.two.mID, new LinkedList<UnificationConstraint>());
            }
            smartList.get(u.two.mID).add(u);
        }
        LinkedList<UnificationConstraint> queue = new LinkedList<>(set);
        while (!queue.isEmpty()) {
            UnificationConstraint u = queue.pop();
            if(AC3_Revise(u)){
                queue.addAll(smartList.get(u.one.mID));
                queue.addAll(smartList.get(u.two.mID));
            }
        }
        for(TemporalDatabase d:this.vars){
            if(d.domain.isEmpty()){
                return false;
            }
        }
        return true;
    }

    boolean AC3_Revise(UnificationConstraint u) {
        HashSet<String> supportOne = new HashSet<>(), supportTwo = new HashSet<>();
        for (StateVariable v : u.one.domain) {
            supportOne.add(v.GetObjectConstant());
        }
        for (StateVariable v : u.two.domain) {
            supportTwo.add(v.GetObjectConstant());
        }
        boolean reduced = false;
        {
            LinkedList<StateVariable> remove = new LinkedList<>();
            for (StateVariable v : u.one.domain) {
                if (!supportTwo.contains(v.GetObjectConstant())) {
                    remove.add(v);
                }
            }
            if (!remove.isEmpty()) {
                reduced = true;
                u.one.domain.removeAll(remove);
            }
        }
        {
            LinkedList<StateVariable> remove = new LinkedList<>();
            for (StateVariable v : u.two.domain) {
                if (!supportOne.contains(v.GetObjectConstant())) {
                    remove.add(v);
                }
            }
            if (!remove.isEmpty()) {
                reduced = true;
                u.two.domain.removeAll(remove);
            }
        }
        return reduced;
    }

    /**
     * propagates the necessary unification constraints
     *
     * @param st
     * @return
     */
    public boolean PropagateAndCheckConsistency(State st) {
        return AC3(unificationConstraints);
    }

    /**
     *
     * @param a
     * @param b
     */
    public void AddUnificationConstraint(TemporalDatabase a, TemporalDatabase b) {
        /*UnificationConstraint uc = new UnificationConstraint(a, b);
         int max = Math.max(a.mID, b.mID);
         if(!unificationConstraints.containsKey(max)){
         unificationConstraints.put(max, new HashSet<UnificationConstraint>());
         }*/
        unificationConstraints.add(new UnificationConstraint(a, b));
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
    public void Merge(TemporalDatabase tdb, TemporalDatabase consumer) {
        // merging consumer into tdb, which means removing all the references for consumer from the system and replacing them with tdb
        // also intersecting the domains
        tdb.domain.retainAll(consumer.domain);

        for (TemporalDatabase.ChainComponent comp : tdb.chain) {
            for (TemporalEvent e : comp.contents) {
                e.mDatabase = tdb;
            }
        }

        /**
         * TODO: careful here, we need to rehash the parts we change ....
         */
        List<UnificationConstraint> remove = new LinkedList<>(), add = new LinkedList<>();
        for (UnificationConstraint p : unificationConstraints) {
            if (p.one.mID == consumer.mID) {
                //p.one = tdb;
                remove.add(p);
            }
            if (p.two.mID == consumer.mID) {
                //p.two = tdb;
                remove.add(p);
            }
        }
        unificationConstraints.removeAll(remove);
        for (UnificationConstraint u : remove) {
            if (u.one.mID == consumer.mID) {
                u.one = tdb;
                if (u.two != tdb) {
                    add.add(u);
                }
            }
            if (u.two.mID == consumer.mID) {
                u.two = tdb;
                if (u.one != tdb) {
                    add.add(u);
                }
            }
        }
        unificationConstraints.addAll(add);
        vars.remove(consumer);
    }

}
