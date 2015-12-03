package fape.core.planning.resources;

import fape.core.planning.search.flaws.flaws.ResourceFlaw;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.TPRef;

import java.util.LinkedList;
import java.util.List;

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
        this.produced = 0;
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
