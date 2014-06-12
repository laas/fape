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

import fape.core.planning.constraints.TemporalConstraint;
import fape.core.planning.resources.solvers.MCS;
import fape.core.planning.search.Flaw;
import fape.core.planning.search.ResourceFlaw;
import fape.core.planning.search.SupportOption;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import planstack.anml.model.concrete.TPRef;

/**
 * aka Reservoir
 *
 * @author FD
 */
public class Replenishable extends Resource {

    public ReservoirStrategy strategy = ReservoirStrategy.BALANCE_CONSTRAINT;

    private List<ResourceFlaw> GatherFlawsThroughMCSReformulation(State st) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public enum ReservoirStrategy {

        BALANCE_CONSTRAINT, MCS_REFORMULATION
    }

    @Override
    public void MergeInto(Resource b) {
        this.events.addAll(((Replenishable) b).events);
    }

    @Override
    public Resource DeepCopy() {
        Replenishable r = new Replenishable();
        r.continuous = this.continuous;
        r.max = this.max;
        r.min = this.min;
        r.mID = this.mID;
        //r.name = this.name;
        r.stateVariable = this.stateVariable;
        r.events = new ArrayList<>(this.events);
        return r;
    }

    private class Event {

        TPRef tp;
        float value;

        Event(TPRef ed, float val) {
            tp = ed;
            value = val;
        }
    }
    private ArrayList<Event> events = new ArrayList<>();
    public float min, max;

    private List<ResourceFlaw> GatherFlawsThroughBalanceConstraint(State st) {
        List<ResourceFlaw> ret = new LinkedList<>();

        //create precedence sets
        // BS - strictly before , AS - strictly after
        ArrayList<HashSet<Integer>> b = new ArrayList<>(), a = new ArrayList<>(), bs = new ArrayList<>(), as = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            b.add(new HashSet<Integer>());
            a.add(new HashSet<Integer>());
            bs.add(new HashSet<Integer>());
            as.add(new HashSet<Integer>());
        }
        for (int i = 0; i < events.size(); i++) {
            for (int j = 0; j < events.size(); j++) {
                if (i == j) {
                    continue;
                }
                if (st.tempoNet.CanBeBefore(events.get(i).tp, events.get(j).tp)) {
                    b.get(j).add(i);
                    a.get(i).add(j);
                } else {
                    // j happens strictly before i, i happens strictly after j
                    bs.get(i).add(j);
                    as.get(j).add(i);
                }
            }
        }
        //L^<_, L^>
        for (int i = 0; i < events.size(); i++) {
            float totalMinBefore = 0, totalMaxBefore = 0, totalMinAfter = events.get(i).value, totalMaxAfter = events.get(i).value; //after includes myself
            
            for (Integer j : b.get(i)) {//all possible productions
                if (events.get(j).value > 0) {
                    totalMinBefore += events.get(j).value;
                }
            }
            for(Integer j:bs.get(i)){//all necessary consumptions
                if (events.get(j).value < 0) {
                    totalMinBefore += events.get(j).value;
                }
            }
            
            for (Integer j : b.get(i)) {//all possible consumptions
                if (events.get(j).value < 0) {
                    totalMaxBefore += events.get(j).value;
                }
            }
            for(Integer j:bs.get(i)){//all necessary productions
                if (events.get(j).value > 0) {
                    totalMaxBefore += events.get(j).value;
                }
            }
            
            for (Integer j : a.get(i)) {//all possible productions
                if (events.get(j).value > 0) {
                    totalMinAfter += events.get(j).value;
                }
            }
            for(Integer j:as.get(i)){//all necessary consumptions
                if (events.get(j).value < 0) {
                    totalMinAfter += events.get(j).value;
                }
            }
            
            for (Integer j : a.get(i)) {//all possible consumptions
                if (events.get(j).value < 0) {
                    totalMaxAfter += events.get(j).value;
                }
            }
            for(Integer j:as.get(i)){//all necessary productions
                if (events.get(j).value > 0) {
                    totalMaxAfter += events.get(j).value;
                }
            }
            
            if (totalMinBefore < min) {//overconsumption
                ResourceFlaw f = new ResourceFlaw();
                //find an consumption event that can be moved after i
                /*LinkedList<Integer> candidates = new LinkedList<>(a.get(i));
                candidates.retainAll(b.get(i));
                for (Integer j : candidates) {
                    if (events.get(j).value <= totalBefore - min) {
                        //moving this event resolves the flaw
                        f.resolvers.add(Resource.createTemporalConstrainOption(events.get(i).tp, events.get(j).tp, 1, Integer.MAX_VALUE));
                    }
                }*/
                //we can merge this resource with another one
                f.resolvers.addAll(st.resMan.GetResolvingBindings(this, totalMinBefore - min, st));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<SupportOption> ol = st.ResourceBalancingActions(events.get(i).tp, false, totalMinBefore - min);
                f.resolvers.addAll(ol);
                ret.add(f);
            }
            if (totalMaxBefore > max) {//overproduction
                ResourceFlaw f = new ResourceFlaw();
                //find an production event that can be moved after i
                /*LinkedList<Integer> candidates = new LinkedList<>(a.get(i));
                candidates.retainAll(b.get(i));
                for (Integer j : candidates) {
                    if (events.get(j).value >= totalMaxBefore - max) {
                        //moving this event resolves the flaw
                        f.resolvers.add(Resource.createTemporalConstrainOption(events.get(i).tp, events.get(j).tp, 1, Integer.MAX_VALUE));
                    }
                }*/
                //we can merge this resource with another one
                f.resolvers.addAll(st.resMan.GetResolvingBindings(this, totalMaxBefore - max, st));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<SupportOption> ol = st.ResourceBalancingActions(events.get(i).tp, false, totalMaxBefore - max);
                f.resolvers.addAll(ol);
                ret.add(f);
            }
            if (totalMinAfter < min) {//overconsumption
                ResourceFlaw f = new ResourceFlaw();
                //find an production event that can be moved after i
                /*LinkedList<Integer> candidates = new LinkedList<>(b.get(i));
                candidates.retainAll(a.get(i));
                for (Integer j : candidates) {
                    if (events.get(j).value <= totalAfter - min) {
                        //moving this event resolves the flaw
                        f.resolvers.add(Resource.createTemporalConstrainOption(events.get(j).tp, events.get(i).tp, 1, Integer.MAX_VALUE));
                    }
                }*/
                //we can merge this resource with another one
                f.resolvers.addAll(st.resMan.GetResolvingBindings(this, totalMinAfter - min, st));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<SupportOption> ol = st.ResourceBalancingActions(events.get(i).tp, true, totalMinAfter - min);
                f.resolvers.addAll(ol);
                ret.add(f);
            }
            if (totalMaxAfter > max) {//overproduction
                ResourceFlaw f = new ResourceFlaw();
                //find an production event that can be moved after i
                /*LinkedList<Integer> candidates = new LinkedList<>(b.get(i));
                candidates.retainAll(a.get(i));
                for (Integer j : candidates) {
                    if (events.get(j).value >= totalAfter - max) {
                        //moving this event resolves the flaw
                        f.resolvers.add(Resource.createTemporalConstrainOption(events.get(j).tp, events.get(i).tp, 1, Integer.MAX_VALUE));
                    }
                }*/
                //we can merge this resource with another one
                f.resolvers.addAll(st.resMan.GetResolvingBindings(this, totalMaxAfter - max, st));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<SupportOption> ol = st.ResourceBalancingActions(events.get(i).tp, true, totalMaxAfter - max);
                f.resolvers.addAll(ol);
                ret.add(f);
            }
        }

        return ret;
    }

    @Override
    public List<ResourceFlaw> GatherFlaws(State st) {
        switch (strategy) {
            case BALANCE_CONSTRAINT:
                return GatherFlawsThroughBalanceConstraint(st);
            case MCS_REFORMULATION:
                return GatherFlawsThroughMCSReformulation(st);
            default:
                throw new FAPEException("Unkown strategy.");
        }
    }

    @Override
    public void addConsumption(State st, TPRef start, TPRef end, float value) {
        events.add(new Event(start, -value));
    }

    @Override
    public void addProduction(State st, TPRef start, TPRef end, float value) {
        events.add(new Event(start, value));
    }

    @Override
    public void addUsage(State st, TPRef start, TPRef end, float value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * we assume that this is the event that sets up the maximal capacity,
     * overriding the function definition, hence we treat it as an production
     * event
     *
     * different interpretation would have to be set up under a different
     * resource
     *
     * @param st
     * @param start
     * @param end
     * @param value
     */
    @Override
    public void addAssignement(State st, TPRef start, TPRef end, float value) {
        this.addProduction(st, start, end, value - this.min);
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
