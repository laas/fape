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

import fape.core.planning.search.resolvers.TemporalConstraint;
import fape.core.planning.search.ResourceFlaw;
import fape.core.planning.search.resolvers.Resolver;
import fape.core.planning.states.State;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.TPRef;

import java.util.List;

/**
 *
 * @author FD
 */
public abstract class Resource {

    private static int idCounter = 0;
    protected int mID = idCounter++;
    
    @Override
    public boolean equals(Object obj) {
        return mID == ((Resource)obj).mID; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.mID;
        return hash;
    }

    
    
    //public String name;
    //public String[] args;
    public boolean continuous = false; //true for float    
    public ParameterizedStateVariable stateVariable;

    public abstract List<ResourceFlaw> GatherFlaws(State st);

    public boolean isConsistent(State st) {
        List<ResourceFlaw> l = GatherFlaws(st);
        for (ResourceFlaw f : l) {
            if (f.resolvers.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static Resolver createTemporalConstrainOption(TPRef first, TPRef second, int min, int max) {
        return new TemporalConstraint(first, second, min, max);
    }

    public abstract void addConsumption(State st, TPRef start, TPRef end, float value);

    public abstract void addProduction(State st, TPRef start, TPRef end, float value);

    public abstract void addUsage(State st, TPRef start, TPRef end, float value);

    public abstract void addAssignement(State st, TPRef start, TPRef end, float value);

    public abstract void addRequirement(State st, TPRef start, TPRef end, float value);

    public abstract void addLending(State st, TPRef start, TPRef end, float value);

    /*public String getQualifyingName() {
        String ret = name + "(";
        for (String s : args) {
            ret += s + ",";
        }
        if (args.length > 0) {
            ret = ret.substring(0, ret.length() - 1);
        }
        return ret + ")";
    }*/

    public abstract void MergeInto(Resource b);

    public abstract Resource DeepCopy();

    boolean isTriviallyConsistent(State st) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
