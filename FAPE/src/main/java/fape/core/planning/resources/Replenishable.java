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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import planstack.anml.model.concrete.TPRef;

/**
 * aka Reservoir
 *
 * @author FD
 */
public class Replenishable extends Resource {

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

    @Override
    public List<ResourceFlaw> GatherFlaws(State st) {
        List<ResourceFlaw> ret = new LinkedList<>();

        //create precedence sets
        // BS - before or at the same time, AS - after or the same time, U - undefined to each other
        ArrayList<LinkedList<Integer>> bs = new ArrayList<>(), as = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            bs.add(new LinkedList<Integer>());
            as.add(new LinkedList<Integer>());
        }
        for (int i = 0; i < events.size(); i++) {
            for (int j = i + 1; j < events.size(); j++) {
                if (st.tempoNet.CanBeBefore(events.get(i).tp, events.get(j).tp)) {
                    bs.get(j).add(i);
                    as.get(i).add(j);
                }
            }
        }
        //L^<_, L^>
        for (int i = 0; i < events.size(); i++) {
            float totalBefore = 0, totalAfter = events.get(i).value;
            for (Integer j : bs.get(i)) {
                totalBefore += events.get(j).value;
            }
            for (Integer j : as.get(i)) {
                totalAfter += events.get(j).value;
            }
            if (totalBefore < min) {//overconsumption
                ResourceFlaw f = new ResourceFlaw();
                //find an consumption event that can be moved after i
                LinkedList<Integer> candidates = new LinkedList<>(as.get(i));
                candidates.retainAll(bs.get(i));
                for (Integer j : candidates) {
                    if (events.get(j).value <= totalBefore - min) {
                        //moving this event resolves the flaw
                        f.resolvers.add(Resource.createTemporalConstrainOption(events.get(i).tp, events.get(j).tp, 1, Integer.MAX_VALUE));
                    }
                }
                //we can merge this resource with another one
                f.resolvers.addAll(st.resMan.GetResolvingBindings(this, totalBefore - min, st));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<SupportOption> ol = st.ResourceBalancingActions(events.get(i).tp, false, totalBefore - min);
                f.resolvers.addAll(ol);
                ret.add(f);
            }
            if (totalBefore > max) {//overproduction
                ResourceFlaw f = new ResourceFlaw();
                //find an production event that can be moved after i
                LinkedList<Integer> candidates = new LinkedList<>(as.get(i));
                candidates.retainAll(bs.get(i));
                for (Integer j : candidates) {
                    if (events.get(j).value >= totalBefore - max) {
                        //moving this event resolves the flaw
                        f.resolvers.add(Resource.createTemporalConstrainOption(events.get(i).tp, events.get(j).tp, 1, Integer.MAX_VALUE));
                    }
                }
                //we can merge this resource with another one
                f.resolvers.addAll(st.resMan.GetResolvingBindings(this, totalBefore - max, st));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<SupportOption> ol = st.ResourceBalancingActions(events.get(i).tp, false, totalBefore - max);
                f.resolvers.addAll(ol);
                ret.add(f);
            }
            if (totalAfter < min) {//overconsumption
                ResourceFlaw f = new ResourceFlaw();
                //find an production event that can be moved after i
                LinkedList<Integer> candidates = new LinkedList<>(bs.get(i));
                candidates.retainAll(as.get(i));
                for (Integer j : candidates) {
                    if (events.get(j).value <= totalAfter - min) {
                        //moving this event resolves the flaw
                        f.resolvers.add(Resource.createTemporalConstrainOption(events.get(j).tp, events.get(i).tp, 1, Integer.MAX_VALUE));
                    }
                }
                //we can merge this resource with another one
                f.resolvers.addAll(st.resMan.GetResolvingBindings(this, totalAfter - min, st));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<SupportOption> ol = st.ResourceBalancingActions(events.get(i).tp, true, totalAfter - min);
                f.resolvers.addAll(ol);
                ret.add(f);
            }
            if (totalAfter > max) {//overproduction
                ResourceFlaw f = new ResourceFlaw();
                //find an production event that can be moved after i
                LinkedList<Integer> candidates = new LinkedList<>(bs.get(i));
                candidates.retainAll(as.get(i));
                for (Integer j : candidates) {
                    if (events.get(j).value >= totalAfter - max) {
                        //moving this event resolves the flaw
                        f.resolvers.add(Resource.createTemporalConstrainOption(events.get(j).tp, events.get(i).tp, 1, Integer.MAX_VALUE));
                    }
                }
                //we can merge this resource with another one
                f.resolvers.addAll(st.resMan.GetResolvingBindings(this, totalAfter - max, st));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<SupportOption> ol = st.ResourceBalancingActions(events.get(i).tp, true, totalAfter - max);
                f.resolvers.addAll(ol);
                ret.add(f);
            }
        }

        return ret;
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
     * overriding the function definition, hence we treat it as an production event
     * 
     * different interpretation would have to be set up under a different resource
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
