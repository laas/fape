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
import fape.exceptions.FAPEException;
import fape.util.TinyLogger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class ConstraintNetworkManager {

    HashSet<UnificationConstraint> unificationConstraints = new HashSet<>(); // (database id|state variable value id) -> constraint
    public HashMap<Integer, IUnifiable> objectMapper = new HashMap<>();

    public void CheckConsistency() {
        for (UnificationConstraint c : unificationConstraints) {
            if (!objectMapper.containsKey(c.one)) {
                throw new FAPEException("Unknown IUnifiable reference: " + c.one);
            }
            if (!objectMapper.containsKey(c.two)) {
                throw new FAPEException("Unknown IUnifiable reference: " + c.two);
            }
        }
    }

    boolean AC3(HashSet<UnificationConstraint> set) {
        HashMap<Integer, List<UnificationConstraint>> smartList = new HashMap<>();
        for (UnificationConstraint u : set) {
            if (!smartList.containsKey(u.one)) {
                smartList.put(u.one, new LinkedList<UnificationConstraint>());
            }
            smartList.get(u.one).add(u);
            if (!smartList.containsKey(u.two)) {
                smartList.put(u.two, new LinkedList<UnificationConstraint>());
            }
            smartList.get(u.two).add(u);
        }
        LinkedList<UnificationConstraint> queue = new LinkedList<>(set);
        while (!queue.isEmpty()) {
            UnificationConstraint u = queue.pop();
            if (AC3_Revise(u)) {
                if (objectMapper.get(u.one).EmptyDomain() || objectMapper.get(u.two).EmptyDomain()) {
                    return false;
                }
                queue.addAll(smartList.get(u.one));
                queue.addAll(smartList.get(u.two));
            }
        }
        return true;
    }

    boolean AC3_Revise(UnificationConstraint u) {
        boolean reduced = false;
        reduced = reduced || objectMapper.get(u.one).ReduceDomain(new HashSet<>(objectMapper.get(u.two).GetDomainObjectConstants()));
        // TODO: this second reduction wight no be executed if reduced == true
        reduced = reduced || objectMapper.get(u.two).ReduceDomain(new HashSet<>(objectMapper.get(u.one).GetDomainObjectConstants()));
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
     * we maintain an index of unifiables across states
     *
     * @param a
     */
    public void AddUnifiable(IUnifiable a) {
        objectMapper.put(a.GetUniqueID(), a);
    }

    /**
     *
     * @param a
     * @param b
     */
    public void AddUnificationConstraint(IUnifiable a, IUnifiable b) {
        if (!objectMapper.containsKey(a.mID) || !objectMapper.containsKey(b.mID)) {
            throw new FAPEException("Unknown IUnifiable reference.");
        }
        TinyLogger.LogInfo("Adding new constraint between: " + a.mID + a.Explain() + " : " + a.GetDomainObjectConstants() + ", " + b.mID + b.Explain() + " : " + b.GetDomainObjectConstants());
        unificationConstraints.add(new UnificationConstraint(a, b));
    }

    public void Merge(IUnifiable mergeInto, IUnifiable mergeFrom) {
        /**
         * TODO: careful here, we need to rehash the parts we change ....
         */
        List<UnificationConstraint> remove = new LinkedList<>(), add = new LinkedList<>();
        for (UnificationConstraint p : unificationConstraints) {
            if (p.one == mergeFrom.mID) {
                remove.add(p);
            }
            if (p.two == mergeFrom.mID) {
                remove.add(p);
            }
        }
        unificationConstraints.removeAll(remove);
        for (UnificationConstraint u : remove) {
            if (u.one == mergeFrom.mID && u.two != mergeInto.mID) {
                UnificationConstraint c = new UnificationConstraint(mergeInto, objectMapper.get(u.two));
                add.add(c);
            }
            if (u.two == mergeFrom.mID && u.one != mergeInto.mID) {
                UnificationConstraint c = new UnificationConstraint(objectMapper.get(u.one), mergeInto);
                add.add(c);
            }
        }
        unificationConstraints.addAll(add);
    }

    public ConstraintNetworkManager DeepCopy() {
        ConstraintNetworkManager nm = new ConstraintNetworkManager();
        nm.unificationConstraints = new HashSet<>(this.unificationConstraints);
        return nm;
    }

    /**
     * Removes constraints between objects that doesn't appear in the object mapper.
     * This step is necessary on plan repair where some events can be removed (for
     * instance on action failure).
     */
    public void RemoveOutdatedConstraints() {
        List<UnificationConstraint> toRemove = new LinkedList<>();
        for(UnificationConstraint uc : this.unificationConstraints) {
            if(!objectMapper.containsKey(uc.one) || !objectMapper.containsKey(uc.two)) {
                toRemove.add(uc);
            }
        }
        this.unificationConstraints.removeAll(toRemove);
    }

    public String Report() {
        String ret = "";

        ret += "{" + "constraints: " + this.unificationConstraints.size() + ", mapper:" + this.objectMapper.size() + "}";

        return ret;
    }

}
