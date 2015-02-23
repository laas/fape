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

import fape.core.planning.preprocessing.ActionDecompositions;
import fape.core.planning.search.flaws.flaws.ResourceFlaw;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.search.flaws.resolvers.ResourceSupportingAction;
import fape.core.planning.search.flaws.resolvers.ResourceSupportingDecomposition;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.statements.AbstractConsumeResource;
import planstack.anml.model.abs.statements.AbstractProduceResource;
import planstack.anml.model.abs.statements.AbstractResourceStatement;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.TPRef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

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

    /**
     * Finds actions that contain a resource modification event of the given
     * level
     *
     * @param when
     * @param after
     * @param var
     * @param amount
     * @param st
     * @return Actions containing at least one statement inducing a change on
     * the function.
     */
    public List<Resolver> ResourceBalancingActions(TPRef when, boolean after, ParameterizedStateVariable var, float amount, State st) {
        List<Resolver> ret = new LinkedList<>();
        //check whether we can add an action to resolve a conflict
        HashSet<AbstractAction> candidates = new HashSet<>();
        for (AbstractAction act : st.pb.abstractActions()) {
            for (AbstractResourceStatement resStatement : act.jResStatements()) {
                if (resStatement.sv().func() == var.func() && ((amount < 0 && resStatement instanceof AbstractProduceResource) || //we need a production event to satisfy our overconsumption
                        (amount > 0 && resStatement instanceof AbstractConsumeResource))) { //vice-versa
                    candidates.add(act);
                    ResourceSupportingAction o = new ResourceSupportingAction();
                    o.absAction = act;
                    o.when = when;
                    o.before = !after;
                    o.unifyingResourceVariable = var;
                    ret.add(o);
                }
            }
        }
        //we may also achieve the action through decomposition
        ActionDecompositions decompositions = new ActionDecompositions(st.pb);
        for (Action leaf : st.getOpenLeaves()) {
            for (Integer decID : decompositions.possibleDecompositions(leaf, candidates)) {
                ResourceSupportingDecomposition opt = new ResourceSupportingDecomposition();
                opt.resourceMotivatedActionToDecompose = leaf;
                opt.decompositionID = decID;
                opt.when = when;
                opt.before = !after;
                ret.add(opt);
            }
        }

        return ret;
    }

    /**
     * determines whether the resource has any conflicts to resolve
     *
     * @param st
     * @return
     */
    @Override
    public boolean isTriviallyConsistent(State st) {
        ArrayList<HashSet<Integer>> b = new ArrayList<>(), pbs = new ArrayList<>(), bs = new ArrayList<>(), pb = new ArrayList<>();
        for (Event event : events) {
            b.add(new HashSet<Integer>());
            pbs.add(new HashSet<Integer>());
            bs.add(new HashSet<Integer>());
            pb.add(new HashSet<Integer>());
        }
        for (int i = 0; i < events.size(); i++) {
            for (int j = 0; j < events.size(); j++) {
                if (i == j) {
                    continue;
                }
                if (st.canBeStrictlyBefore(events.get(j).tp, events.get(i).tp)) {
                    // i in pb(j)
                    pb.get(j).add(i);
                } else {
                    // j in bs(i)
                    bs.get(i).add(j);
                }

                if (st.canBeBefore(events.get(j).tp, events.get(i).tp)) {
                    //j in pbs(i)
                    pbs.get(i).add(j);
                } else {
                    //i in b(j)
                    b.get(j).add(i);
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
            for (Integer j : bs.get(i)) {//all necessary consumptions
                if (events.get(j).value < 0) {
                    totalMinBefore += events.get(j).value;
                }
            }

            for (Integer j : b.get(i)) {//all possible consumptions
                if (events.get(j).value < 0) {
                    totalMaxBefore += events.get(j).value;
                }
            }
            for (Integer j : bs.get(i)) {//all necessary productions
                if (events.get(j).value > 0) {
                    totalMaxBefore += events.get(j).value;
                }
            }

            for (Integer j : pbs.get(i)) {//all possible productions
                if (events.get(j).value > 0) {
                    totalMinAfter += events.get(j).value;
                }
            }
            for (Integer j : pb.get(i)) {//all necessary consumptions
                if (events.get(j).value < 0) {
                    totalMinAfter += events.get(j).value;
                }
            }

            for (Integer j : pbs.get(i)) {//all possible consumptions
                if (events.get(j).value < 0) {
                    totalMaxAfter += events.get(j).value;
                }
            }
            for (Integer j : pb.get(i)) {//all necessary productions
                if (events.get(j).value > 0) {
                    totalMaxAfter += events.get(j).value;
                }
            }

            if (totalMinBefore < min) {//overconsumption
                return false;
            }
            if (totalMaxBefore > max) {//overproduction
                return false;
            }
            if (totalMinAfter < min) {//overconsumption
                return false;
            }
            if (totalMaxAfter > max) {//overproduction
                return false;
            }
        }
        return true;
    }

    /**
     * determines necessary conditions for consistency of this resource
     *
     * @param st
     * @return
     */
    @Override
    public boolean isConsistent(State st) {
        return true; //the reservoir resource is always considered consistent, since any conflict can be resolved by adding an appropriete action
    }

    private List<ResourceFlaw> GatherFlawsThroughBalanceConstraint(State st) {
        List<ResourceFlaw> ret = new LinkedList<>();

        //create precedence sets
        // B - strictly before, BS - before or same, PB - possibly before, PBS - possibly before or same
        ArrayList<HashSet<Integer>> bs = new ArrayList<>(), b = new ArrayList<>(), pbs = new ArrayList<>(), pb = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            bs.add(new HashSet<Integer>());
            pb.add(new HashSet<Integer>());
            b.add(new HashSet<Integer>());
            pbs.add(new HashSet<Integer>());
        }
        for (int i = 0; i < events.size(); i++) {
            for (int j = 0; j < events.size(); j++) {
                if (i == j) {
                    continue;
                }
                if (st.canBeStrictlyBefore(events.get(i).tp, events.get(j).tp)) {
                    // i in pb(j)
                    pb.get(j).add(i);
                } else {
                    // j in bs(i)
                    bs.get(i).add(j);
                }

                if (st.canBeBefore(events.get(j).tp, events.get(i).tp)) {
                    //j in pbs(i)
                    pbs.get(i).add(j);
                } else {
                    //i in b(j)
                    b.get(j).add(i);
                }
            }
        }

        //L^<_, L^>
        for (int i = 0; i < events.size(); i++) {
            float totalMinBefore = 0, totalMaxBefore = 0, totalMinAfter = events.get(i).value, totalMaxAfter = events.get(i).value; //after includes itself

            //totalMaxBefore
            for (Integer j : pb.get(i)) {//all possible consumptions
                if (events.get(j).value < 0) {
                    totalMaxBefore += events.get(j).value;
                }
            }
            for (Integer j : b.get(i)) {//all necessary productions
                if (events.get(j).value > 0) {
                    totalMaxBefore += events.get(j).value;
                }
            }

            //totalMinBefore
            for (Integer j : pb.get(i)) {//all possible productions
                if (events.get(j).value > 0) {
                    totalMinBefore += events.get(j).value;
                }
            }
            for (Integer j : b.get(i)) {//all necessary consumptions
                if (events.get(j).value < 0) {
                    totalMinBefore += events.get(j).value;
                }
            }

            //totalMaxAfter
            for (Integer j : pbs.get(i)) {//all possible consumptions
                if (events.get(j).value < 0) {
                    totalMaxAfter += events.get(j).value;
                }
            }
            for (Integer j : bs.get(i)) {//all necessary productions
                if (events.get(j).value > 0) {
                    totalMaxAfter += events.get(j).value;
                }
            }

            //totalMinAfter
            for (Integer j : pbs.get(i)) {//all possible productions
                if (events.get(j).value > 0) {
                    totalMinAfter += events.get(j).value;
                }
            }
            for (Integer j : bs.get(i)) {//all necessary consumptions
                if (events.get(j).value < 0) {
                    totalMinAfter += events.get(j).value;
                }
            }

            if (totalMinBefore < min) {//overconsumption                
                ResourceFlaw f = new ResourceFlaw();
                //we can merge this resource with another one
                f.resolvers.addAll(st.getResolvingBindings(this, totalMinBefore - min));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<Resolver> ol = ResourceBalancingActions(events.get(i).tp, false, this.stateVariable, totalMinBefore - min, st);
                f.resolvers.addAll(ol);
                ret.add(f);
            }
            
            if (totalMaxBefore > max) {//overconsumption
                ResourceFlaw f = new ResourceFlaw();
                //we can merge this resource with another one
                f.resolvers.addAll(st.getResolvingBindings(this, totalMaxBefore - max));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<Resolver> ol = ResourceBalancingActions(events.get(i).tp, false, this.stateVariable, totalMaxBefore - min, st);
                f.resolvers.addAll(ol);
                ret.add(f);
            }
            
            if (totalMinAfter < min) {//overconsumption
                ResourceFlaw f = new ResourceFlaw();
                //find an production event that can be moved before i
                /*LinkedList<Integer> candidates = new LinkedList<>(pbs.get(i));
                candidates.retainAll(pbs.get(i));
                for (Integer j : candidates) {
                    if (events.get(j).value <= totalMinAfter - min) {
                        //moving this event resolves the flaw
                        f.resolvers.add(Resource.createTemporalConstrainOption(events.get(j).tp, events.get(i).tp, 1, Integer.MAX_VALUE));
                    }
                }*/
                //we can merge this resource with another one
                f.resolvers.addAll(st.getResolvingBindings(this, totalMinAfter - min));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<Resolver> ol = ResourceBalancingActions(events.get(i).tp, true, this.stateVariable, totalMinAfter - min, st);
                f.resolvers.addAll(ol);
                ret.add(f);
            }
            if (totalMaxAfter > max) {//overproduction
                ResourceFlaw f = new ResourceFlaw();
                //find an production event that can be moved before i
                /*LinkedList<Integer> candidates = new LinkedList<>(b.get(i));
                candidates.retainAll(pbs.get(i));
                for (Integer j : candidates) {
                    if (events.get(j).value >= totalMaxAfter - max) {
                        //moving this event resolves the flaw
                        f.resolvers.add(Resource.createTemporalConstrainOption(events.get(j).tp, events.get(i).tp, 1, Integer.MAX_VALUE));
                    }
                }*/
                //we can merge this resource with another one
                f.resolvers.addAll(st.getResolvingBindings(this, totalMaxAfter - max));
                //we can also add a production action, or decompose an action that shall lead to a production action
                List<Resolver> ol = ResourceBalancingActions(events.get(i).tp, true, this.stateVariable, totalMaxAfter - max, st);
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
        this.max = value;
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
