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

import fape.exceptions.FAPEException;
import fape.util.Reporter;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.VarRef;

import java.util.*;

/**
 *
 * @author FD
 */
public class ConstraintNetworkManager implements Reporter {

    /**
     * Contains all constraints of the CSP (now limited to equality constraints)
     */
    final HashSet<UnificationConstraint> unificationConstraints;

    /**
     * Maps every variable to its domain
     */
    public final HashMap<VarRef, IUnifiable> domains;

    public ConstraintNetworkManager() {
        unificationConstraints = new HashSet<>();
        domains = new HashMap<>();
    }

    public ConstraintNetworkManager(ConstraintNetworkManager toCopy) {
        unificationConstraints = new HashSet<>(toCopy.unificationConstraints);
        domains = new HashMap<>();
        for(Map.Entry<VarRef, IUnifiable> entry : toCopy.domains.entrySet()) {
            domains.put(entry.getKey(), entry.getValue().DeepCopy());
        }
    }

    /**
     * @return True if the network is consistent (no vars with empty domain),
     *         False otherwise
     */
    public boolean isConsistent() {
        return PropagateAndCheckConsistency(); //TODO: smarter update
    }

    public void CheckConsistency() {
        for (UnificationConstraint c : unificationConstraints) {
            if (!domains.containsKey(c.one)) {
                throw new FAPEException("Unknown IUnifiable reference: " + c.one);
            }
            if (!domains.containsKey(c.two)) {
                throw new FAPEException("Unknown IUnifiable reference: " + c.two);
            }
        }
    }

    boolean AC3(HashSet<UnificationConstraint> set) {
        HashMap<VarRef, List<UnificationConstraint>> smartList = new HashMap<>();
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
                if (domains.get(u.one).EmptyDomain() || domains.get(u.two).EmptyDomain()) {
                    return false;
                }
                queue.addAll(smartList.get(u.one));
                queue.addAll(smartList.get(u.two));
            }
        }
        return true;
    }

    /** Reduces the domains of both variables in the unification constraint */
    boolean AC3_Revise(UnificationConstraint u) {
        boolean reduced = false;
        reduced = reduced || domains.get(u.one).ReduceDomain(domains.get(u.two).GetDomainObjectConstants());
        reduced = domains.get(u.two).ReduceDomain(domains.get(u.one).GetDomainObjectConstants()) || reduced;
        return reduced;
    }

    /**
     * Propagates all unification constraints
     *
     * @return True if the CSP is consistent, False otherwise
     */
    public boolean PropagateAndCheckConsistency() {
        return AC3(unificationConstraints);
    }

    public boolean restrictDomain(VarRef var, Collection<String> toValues) {
        domains.get(var).ReduceDomain(toValues);
        return true;
    }

    /**
     * Records a new variable in the CSP
     * @param var Reference of the variable
     * @param domain All elements in the domain of the variable
     */
    public void AddVariable(VarRef var, Collection<String> domain) {
        domains.put(var, new VariableValues(domain));
    }

    public void AddUnificationConstraint(VarRef a, VarRef b) {
        assert domains.containsKey(a);
        assert domains.containsKey(b);
        unificationConstraints.add(new UnificationConstraint(a, b));
    }

    public void AddUnificationConstraints(List<VarRef> as, List<VarRef> bs) {
        assert as.size() == bs.size();
        for(int i=0 ; i < as.size() ; i++) {
            AddUnificationConstraint(as.get(i), bs.get(i));
        }
    }

    public void AddUnificationConstraint(ParameterizedStateVariable a, ParameterizedStateVariable b) {
        if(!a.func().equals(b.func()))
            throw new FAPEException("Error: adding unification constraint between two different predicates: "+ a +"  --  "+ b);
        AddUnificationConstraints(a.jArgs(), b.jArgs());
    }

    /**
     * @param v Variable to look up
     * @return True if variable v is declared in the CSP.
     */
    public boolean contains(VarRef v) {
        return domains.containsKey(v);
    }

    public Collection<String> domainOf(VarRef v) {
        return domains.get(v).GetDomainObjectConstants();
    }

    /* TODO: Was not used and is probably a bit outdated
    public void Merge(IUnifiable mergeInto, IUnifiable mergeFrom) {
        // careful here, we need to rehash the parts we change ....
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
    } */

    public ConstraintNetworkManager DeepCopy() {
        return new ConstraintNetworkManager(this);
    }

    /**
     * Removes constraints between objects that doesn't appear in the object mapper.
     * This step is necessary on plan repair where some events can be removed (for
     * instance on action failure).
     */
    @Deprecated
    public void RemoveOutdatedConstraints() {
        List<UnificationConstraint> toRemove = new LinkedList<>();
        for(UnificationConstraint uc : this.unificationConstraints) {
            if(!domains.containsKey(uc.one) || !domains.containsKey(uc.two)) {
                toRemove.add(uc);
            }
        }
        this.unificationConstraints.removeAll(toRemove);
    }

    @Override
    public String Report() {
        String ret = "";

        ret += "{" + "constraints: " + this.unificationConstraints.size() + ", mapper:" + this.domains.size() + "}";

        return ret;
    }

}
