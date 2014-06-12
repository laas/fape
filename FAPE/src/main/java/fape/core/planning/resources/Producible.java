/*
 * Author:  Filip Dvoøák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvoøák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */

package fape.core.planning.resources;

import fape.core.planning.search.Flaw;
import fape.core.planning.search.ResourceFlaw;
import fape.core.planning.states.State;
import java.util.LinkedList;
import java.util.List;
import planstack.anml.model.concrete.TPRef;

/**
 *
 * @author FD
 */
public class Producible extends Resource {

    float produced = 0;
    
    @Override
    public List<ResourceFlaw> GatherFlaws(State st) {
        return new LinkedList<>();
    }

    @Override
    public boolean isConsistent(State st) {
        return true;
    }

    @Override
    public void addConsumption(State st, TPRef start, TPRef end, float value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addProduction(State st, TPRef start, TPRef end, float value) {
        produced += value;
    }

    @Override
    public void addUsage(State st, TPRef start, TPRef end, float value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addAssignement(State st, TPRef start, TPRef end, float value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addRequirement(State st, TPRef start, TPRef end, float value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addLending(State st, TPRef start, TPRef end, float value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void MergeInto(Resource b) {
        this.produced += ((Producible)b).produced;
    }

    @Override
    public Resource DeepCopy() {
        Producible c = new Producible();
        c.produced = this.produced;        
        c.continuous = this.continuous;
        c.mID = this.mID;
        //c.name = this.name;
        c.stateVariable = this.stateVariable;
        return c;
    }

}
