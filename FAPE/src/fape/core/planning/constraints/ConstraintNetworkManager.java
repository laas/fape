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
package fape.core.planning.constraints;

import fape.core.planning.model.StateVariable;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.IUnifiable;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.TemporalDatabaseManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class ConstraintNetworkManager {

    HashSet<UnificationConstraint> unificationConstraints = new HashSet<>(); // database id -> constraints on that db with db with smaller id

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
            if (AC3_Revise(u)) {
                queue.addAll(smartList.get(u.one.mID));
                queue.addAll(smartList.get(u.two.mID));
            }
        }
        for (TemporalDatabase d : this.vars) {
            if (d.domain.isEmpty()) {
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
    public void AddUnificationConstraint(IUnifiable a, IUnifiable b) {
        unificationConstraints.add(new UnificationConstraint(a, b));
    }

    public void Merge(IUnifiable mergeInto, IUnifiable mergeFrom) {
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

    public ConstraintNetworkManager DeepCopy() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
