/*
 * Author:  Filip Dvo��k <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvo��k <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.core.planning.resources;

import fape.core.planning.search.Flaw;
import fape.core.planning.search.ResourceFlaw;
import fape.core.planning.search.StateVariableBinding;
import fape.core.planning.search.SupportOption;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import planstack.anml.model.FloatFunction;
import planstack.anml.model.IntFunction;
import planstack.anml.model.NumFunction;

import java.util.*;

/**
 *
 * @author FD
 */
public class ResourceManager {

    /**
     * adds a new resource entity into a state
     *
     * @param f
     * @return
     */
    public static Resource generateResourcePrototype(NumFunction f) {
        Resource ret = null;
        switch (f.resourceType()) {
            case "replenishable": {
                Replenishable r = new Replenishable();
                if (f instanceof FloatFunction) {
                    r.continuous = true;
                    r.min = ((FloatFunction) f).minValue();
                    r.max = ((FloatFunction) f).maxValue();
                } else {
                    r.continuous = false;
                    r.min = ((IntFunction) f).minValue();
                    r.max = ((IntFunction) f).maxValue();
                }
                ret = r;
                break;
            }
            case "reusable": {
                Reusable r = new Reusable();
                if (f instanceof FloatFunction) {
                    r.continuous = true;
                    r.min = ((FloatFunction) f).minValue();
                    r.max = ((FloatFunction) f).maxValue();
                } else {
                    r.continuous = false;
                    r.min = ((IntFunction) f).minValue();
                    r.max = ((IntFunction) f).maxValue();
                }
                ret = r;
                break;
            }
            case "consumable": {
                Consumable r = new Consumable();
                if (f instanceof FloatFunction) {
                    r.continuous = true;
                    r.max = ((FloatFunction) f).maxValue();
                } else {
                    r.continuous = false;
                    r.max = ((IntFunction) f).maxValue();
                }
                ret = r;
                break;
            }
            case "producible": {
                Producible r = new Producible();
                r.continuous = f instanceof FloatFunction;
                ret = r;
                break;
            }
            default:
                throw new FAPEException("Unrecognized resource type.");
        }
        //ret.name = name;
        //ret.args = args;
        return ret;
    }

    public int GetInconsistentResources(State st){
        int ret = 0;
        for(Resource r:resources){
            if(!r.isConsistent(st)){
                ret++;
            }
        }
        return ret;
    }
    
    List<Resource> resources = new LinkedList<>();

    /**
     * we first check if some resources can be merged, then we check their
     * consistency individually
     *
     * @param st
     * @return
     */
    public boolean isConsistent(State st) {
        Shrink(st);
        boolean consistency = true;
        for (Resource r : this.resources) {
            consistency &= r.isConsistent(st);
        }
        return consistency;
    }

    /**
     * merges all merge-able pairs of resource
     *
     * @param st
     */
    public void Shrink(State st) {
        List<Pair<Resource, Resource>> candidates = new LinkedList<>();
        ArrayList<Resource> ar = new ArrayList<>(resources);
        HashSet<Resource> exists = new HashSet<>();
        for (int i = 0; i < ar.size(); i++) {
            for (int j = i + 1; j < ar.size(); j++) {
                if (Unified(ar.get(i), ar.get(j), st)) {
                    candidates.add(new Pair(ar.get(i), ar.get(j)));
                    exists.add(ar.get(i));
                    exists.add(ar.get(j));
                }
            }
        }
        for (Pair<Resource, Resource> p : candidates) {
            if (exists.contains(p.value1) && exists.contains(p.value2)) {
                Merge(p.value1, p.value2);
                exists.remove(p.value2);
            }
        }
    }

    public Collection<? extends Flaw> GatherFlaws(State st) {
        List<Flaw> ret = new LinkedList<>();
        for (Resource r : resources) {
            List<ResourceFlaw> l = r.GatherFlaws(st);
            ret.addAll(l);
        }
        return ret;
    }

    /**
     * the two resource must be the same
     *
     * @param a
     * @param b
     * @param st
     * @return
     */
    public boolean Unified(Resource a, Resource b, State st) {
        if (!a.stateVariable.func().name().equals(b.stateVariable.func().name()) || a.stateVariable.jArgs().size() != b.stateVariable.jArgs().size()) {
            return false;
        } else {
            boolean unifies = true;
            for (int i = 0; i < a.stateVariable.jArgs().size() && unifies; i++) {
                unifies &= st.conNet.unified(a.stateVariable.jArgs().get(i), b.stateVariable.jArgs().get(i));
            }
            return unifies;
        }
    }
    
    /**
     * can the two resources be the same?
     *
     * @param a
     * @param b
     * @param st
     * @return
     */
    public boolean Unifiable(Resource a, Resource b, State st) {
        if (!a.stateVariable.func().name().equals(b.stateVariable.func().name()) || a.stateVariable.jArgs().size() != b.stateVariable.jArgs().size()) {
            return false;
        } else {
            boolean unifies = true;
            for (int i = 0; i < a.stateVariable.jArgs().size() && unifies; i++) {
                unifies &= st.conNet.unifiable(a.stateVariable.jArgs().get(i), b.stateVariable.jArgs().get(i));
            }
            return unifies;
        }
    }

    /**
     * merge the second resource into the first one
     *
     * @param a
     * @param b
     */
    public void Merge(Resource a, Resource b) {
        a.MergeInto(b);
        resources.remove(b);
    }

    public boolean AddResourceEvent(Resource r, State aThis) {
        //we either merge the new resource into an existing one
        for (Resource res : resources) {
            if (Unified(res, r, aThis)) {
                Merge(res, r);
                return res.isConsistent(aThis);
            }
        }
        //or we just add it
        resources.add(r);
        return true;
    }

    public ResourceManager DeepCopy() {
        ResourceManager m = new ResourceManager();
        for (Resource r : this.resources) {
            m.resources.add(r.DeepCopy());
        }
        return m;
    }

    /**
     * find resources that can be unifined with the one given to adjust the
     * requirement f
     *
     * @param aThis
     * @param f
     * @return
     */
    Collection<? extends SupportOption> GetResolvingBindings(Replenishable aThis, float f, State st) {
        List<SupportOption> l = new LinkedList<>();
        for (Resource r : resources) {
            if(aThis.equals(r)){
                continue;
            }
            if (Unifiable(r, aThis, st)) { 
                Resource x = aThis.DeepCopy();
                x.MergeInto(r);
                if (x.isTriviallyConsistent(st)) { //create options that bindigs with resources that make this one consistent
                    StateVariableBinding o = new StateVariableBinding();
                    o.one = r.stateVariable;
                    o.two = aThis.stateVariable;
                    l.add(o);
                }
            }
        }
        return l;
    }
}
