package fape.core.planning.resources;

import fape.core.planning.resources.solvers.MCS;
import fape.core.planning.search.flaws.flaws.ResourceFlaw;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.TPRef;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class Reusable extends Resource {

    @Override
    public void MergeInto(Resource b) {
        this.events.addAll(((Reusable)b).events);
    }

    @Override
    public Resource DeepCopy() {
        Reusable r = new Reusable();
        r.mID = this.mID;
        r.continuous = this.continuous;
        r.max = this.max;
        r.min = this.min;
        //r.name = this.name;
        r.stateVariable = this.stateVariable;
        r.events = new LinkedList<>(this.events);                
        return r;
    }

    private class Event {

        TPRef start, end;
        float value;

        Event(TPRef st, TPRef ed, float val) {
            start = st;
            end = ed;
            value = val;
        }
    }

    private List<Event> events = new ArrayList<>();
    public float min, max;

    private boolean mayCollide(Reusable.Event one, Reusable.Event two, State st) {
        return st.canBeBefore(one.start, two.end) && st.canBeBefore(two.start, one.end);
    }

    @Override
    public List<ResourceFlaw> GatherFlaws(State st) {
        List<ResourceFlaw> ret = new LinkedList<>();
        
        //create a possible intersection graph
        ArrayList<LinkedList<Integer>> edges = new ArrayList<>(events.size());
        for (int i = 0; i < events.size(); i++) {
            edges.add(new LinkedList<Integer>());
        }
        for (int i = 0; i < events.size(); i++) {
            for (int j = i + 1; j < events.size(); j++) {
                if (mayCollide(events.get(i), events.get(j), st)) {
                    edges.get(i).add(j);
                    edges.get(j).add(i);
                }
            }
        }

        //find minimal critical sets
        float[] consumptionMap = new float[events.size()];
        for (int i = 0; i < events.size(); i++) {
            consumptionMap[i] = events.get(i).value;
        }
        LinkedList<LinkedList<Integer>> criticalSets = MCS.FindSets(consumptionMap, max, edges);

        //generate resolvers
        for (LinkedList<Integer> set : criticalSets) {
            ResourceFlaw f = new ResourceFlaw();
            for (Integer i : set) {
                for (Integer j : set) {
                    if (i < j) {
                        if (st.canBeBefore(events.get(i).end, events.get(j).start)) {
                            Resolver o = Resource.createTemporalConstrainOption(events.get(i).end, events.get(j).start, 0, Integer.MAX_VALUE);
                            f.resolvers.add(o);
                        }
                        if (st.canBeBefore(events.get(j).end, events.get(i).start)) {
                            Resolver o = Resource.createTemporalConstrainOption(events.get(j).end, events.get(i).start, 0, Integer.MAX_VALUE);
                            f.resolvers.add(o);
                        }
                    }
                }
            }
            ret.add(f);
        }
        return ret;
    }

    @Override
    public void addConsumption(State st, TPRef start, TPRef end, float value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addProduction(State st, TPRef start, TPRef end, float value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addUsage(State st, TPRef start, TPRef end, float value) {
        events.add(new Event(start, end, value));
    }

    /**
     * we assume this sets up the maximal capacity of the resource at the beginning
     * @param st
     * @param start
     * @param end
     * @param value 
     */
    @Override
    public void addAssignement(State st, TPRef start, TPRef end, float value) {
        this.max = value;
    }

    @Override
    public void addRequirement(State st, TPRef start, TPRef end, float value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addLending(State st, TPRef start, TPRef end, float value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
