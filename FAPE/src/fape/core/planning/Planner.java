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
package fape.core.planning;

import fape.core.planning.dtgs.ADTG;
import fape.core.execution.model.ANMLBlock;
import fape.core.execution.model.ActionRef;
import fape.core.execution.model.AtomicAction;
import fape.core.execution.model.Instance;
import fape.core.execution.model.Reference;
import fape.core.execution.model.TemporalConstraint;
import fape.core.execution.model.statements.Statement;
//import fape.core.execution.model.types.Type;

import fape.core.planning.constraints.UnificationConstraintSchema;
import fape.core.planning.model.AbstractAction;
import fape.core.planning.model.AbstractTemporalEvent;
import fape.core.planning.model.Action;
import fape.core.planning.model.StateVariable;
import fape.core.planning.model.Type;
import fape.core.planning.search.Queue;
import fape.core.planning.search.SupportOption;
import fape.core.planning.states.State;
import fape.core.planning.stn.STNManager;
import fape.core.planning.temporaldatabases.IUnifiable;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.core.planning.temporaldatabases.events.resources.ConsumeEvent;
import fape.core.transitions.TransitionIO2Planning;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.TimeAmount;
import fape.util.TimePoint;
import fape.util.TinyLogger;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import java.util.List;
import javax.swing.SwingWorker;

/**
 *
 * @author FD
 */
public class Planner {

    public static boolean debugging = true;
    public static boolean logging = true;
    public static boolean actionResolvers = true; // do we add actions to resolve flaws?

    /**
     *
     */
    //public static int searchWidth = 5;
    //public State init;
    /**
     * a list of types keyed by its name
     */
    public HashMap<String, fape.core.planning.model.Type> types = new HashMap<>();

    /**
     *
     */
    public HashMap<String, StateVariable> vars = new HashMap<>();

    /**
     *
     */
    public HashMap<String, AbstractAction> actions = new HashMap<>();

    /**
     *
     */
    public HashMap<String, ADTG> dtgs = new HashMap<>();

    /**
     *
     */
    public Queue queue = new Queue();

    /**
     *
     */
    public HashMap<AbstractAction, HashMap<Integer, List<UnificationConstraintSchema>>> unificationConstraintPropagationSchema = new HashMap<>();

    private boolean ApplyOption(State next, SupportOption o, TemporalDatabase consumer) {
        TemporalDatabase supporter = null;
        TemporalDatabase.ChainComponent precedingComponent = null;
        if (o.temporalDatabase != -1) {
            supporter = next.GetDatabase(o.temporalDatabase);
            if (o.precedingChainComponent != -1) {
                precedingComponent = supporter.GetChainComponent(o.precedingChainComponent);
            }
        }
        //now we can happily apply all the options
        if (supporter != null && precedingComponent != null) {
            // this is database merge of one persistence into another
            int index = supporter.chain.indexOf(precedingComponent);
            if (supporter.chain.size() - 1 != index && !supporter.chain.get(index + 1).change) {
                //we add into an existing partially ordered set of perstistance events
                TemporalDatabase.ChainComponent comp = supporter.chain.get(index + 1);
                //constraint the values
                next.conNet.AddUnificationConstraint(comp.GetSupportValue(), consumer.GetGlobalConsumeValue());
                //add it
                comp.Add(consumer.chain.get(0));
                //propagate time constraints
                TemporalDatabase.PropagatePrecedence(precedingComponent, comp, next);
                if (supporter.chain.size() > index + 2) {
                    TemporalDatabase.ChainComponent comp2 = supporter.chain.get(index + 2);
                    TemporalDatabase.PropagatePrecedence(comp, comp2, next);
                }
            } else {
                //add a new persistence just after the chosen element

                supporter.chain.add(index + 1, consumer.chain.get(0));
                TemporalDatabase.PropagatePrecedence(precedingComponent, supporter.chain.get(index + 1), next);
                //constraint the values
                next.conNet.AddUnificationConstraint(precedingComponent.GetSupportValue(), consumer.GetGlobalConsumeValue());
                if (supporter.chain.size() > index + 2) {
                    TemporalDatabase.ChainComponent comp2 = supporter.chain.get(index + 2);
                    TemporalDatabase.PropagatePrecedence(supporter.chain.get(index + 1), comp2, next);
                }

            }
            //merge databases
            next.tdb.Merge(next, supporter, consumer);
            //remove consumer
            TemporalDatabase dbRemove = null;
            for (TemporalDatabase db : next.consumers) {
                if (consumer.mID == db.mID) {
                    dbRemove = db;
                }
            }
            next.consumers.remove(dbRemove);
        } else if (supporter != null) {
            //this is a database concatenation
            if (!supporter.chain.getLast().change && !consumer.chain.getFirst().change) {
                //merge the changes
                TemporalDatabase.ChainComponent second = supporter.chain.getLast(),
                        third = consumer.chain.get(1),
                        first = supporter.chain.get(supporter.chain.size() - 2);
                for (TemporalEvent e : consumer.chain.getFirst().contents) {
                    supporter.chain.getLast().contents.add(e);
                }
                consumer.chain.removeFirst();
                for (TemporalDatabase.ChainComponent c : consumer.chain) {
                    supporter.chain.add(c);
                }
                TemporalDatabase.PropagatePrecedence(first, second, next);
                TemporalDatabase.PropagatePrecedence(second, third, next);
                next.conNet.AddUnificationConstraint(first.GetSupportValue(), third.GetConsumeValue());//adding just the constraint between the border values, the middle should be constrained with both                
                next.tdb.Merge(next, supporter, consumer);
            } else {
                //concatenate
                TemporalDatabase.ChainComponent second = consumer.chain.getFirst(),
                        first = supporter.chain.getLast();
                for (TemporalDatabase.ChainComponent c : consumer.chain) {
                    supporter.chain.add(c);
                }
                next.conNet.AddUnificationConstraint(first.GetSupportValue(), second.GetConsumeValue());//adding just the constraint between the border values
                TemporalDatabase.PropagatePrecedence(first, second, next);
                next.tdb.Merge(next, supporter, consumer);
            }
            //remove consumer
            TemporalDatabase dbRemove = null;
            for (TemporalDatabase db : next.consumers) {
                if (consumer.mID == db.mID) {
                    dbRemove = db;
                }
            }
            next.consumers.remove(dbRemove);
        } else if (o.supportingAction != null) {
            //this is a simple applciation of an action
            ActionRef ref = new ActionRef();
            ref.name = o.supportingAction.name;
            int ct = 0;
            for (Instance i : o.supportingAction.params) {
                Reference rf = new Reference();
                rf.refs.add("a" + (ct++) + "_"); //random unbinded parameters
                ref.args.add(rf);
            }
            boolean enforceDuration = true;
            if (!this.actions.get(ref.name).strongDecompositions.isEmpty()) {
                enforceDuration = false;
            }
            Action addedAction = AddAction(ref, next, null, enforceDuration);
            // create the binding between consumer and the new statement in the action that supports it
            TemporalDatabase supportingDatabase = null;
            for (TemporalEvent e : addedAction.events) {
                if (e instanceof TransitionEvent) {
                    TransitionEvent ev = (TransitionEvent) e;
                    if (ev.to.Unifiable(consumer.GetGlobalConsumeValue())) {
                        supportingDatabase = ev.mDatabase;
                    }
                }
            }
            if (supportingDatabase == null) {
                return false;
            } else {
                SupportOption opt = new SupportOption();
                opt.temporalDatabase = supportingDatabase.mID;
                return ApplyOption(next, opt, consumer);
            }
        } else if (o.actionToDecompose != -1) {
            Action decomposedAction = next.taskNet.GetAction(o.actionToDecompose);
            // this is a task decomposition
            // we need to decompose all the actions of the chosen decomposition
            // which represents adding all of them into the plan through AddAction and
            // setting up precedence constraints for them
            List<ActionRef> actionsForDecomposition = null;
            List<TemporalConstraint> tempCons = null;
            int ct = 0;
            for (Pair<List<ActionRef>, List<TemporalConstraint>> p : decomposedAction.refinementOptions) {
                if (ct++ == o.decompositionID) {
                    actionsForDecomposition = p.value1;
                    tempCons = p.value2;
                }
            }
            //put the actions into the plan
            decomposedAction.decomposition = new LinkedList<>();
            ArrayList<Action> l = new ArrayList<>();
            for (ActionRef ref : actionsForDecomposition) {
                l.add(AddAction(ref, next, decomposedAction, true));
            }

            //now we need to introduce binding constraints between the events through the parameter binding
            //add the constraints parent-child temporal constraints
            for (Action a : l) {
                next.tempoNet.EnforceBefore(decomposedAction.start, a.start);
                next.tempoNet.EnforceBefore(a.end, decomposedAction.end);
                List<UnificationConstraintSchema> lt = unificationConstraintPropagationSchema.get(actions.get(decomposedAction.name)).get(o.decompositionID);
                for (UnificationConstraintSchema s : lt) {
                    //now we are adding a constraint between a pair of temporal databases and/or state variable values
                    TemporalEvent e = decomposedAction.events.get(s.mEventID);
                    TemporalEvent e2 = a.events.get(s.actionEventID);
                    IUnifiable aa = null, bb = null;
                    switch (s.typeLeft) {
                        case EVENT:
                            aa = e.mDatabase;
                            break;
                        case FIRST_VALUE:
                            if (e instanceof TransitionEvent) {
                                aa = ((TransitionEvent) e).from;
                            } else if (e instanceof PersistenceEvent) {
                                aa = ((PersistenceEvent) e).value;
                            } else {
                                throw new FAPEException("Unknown event type.");
                            }
                            break;
                        case SECOND_VALUE:
                            aa = ((TransitionEvent) e).to;
                            break;
                    }

                    switch (s.typeRight) {
                        case EVENT:
                            bb = e2.mDatabase;
                            break;
                        case FIRST_VALUE:
                            if (e2 instanceof TransitionEvent) {
                                bb = ((TransitionEvent) e2).from;
                            } else if (e2 instanceof PersistenceEvent) {
                                bb = ((PersistenceEvent) e2).value;
                            } else {
                                throw new FAPEException("Unknown event type.");
                            }
                            break;
                        case SECOND_VALUE:
                            bb = ((TransitionEvent) e2).to;
                            break;
                    }
                    next.conNet.AddUnificationConstraint(aa, bb);
                }
            }
            //add temporal constraints between actions
            for (TemporalConstraint c : tempCons) {
                next.tempoNet.EnforceBefore(l.get(c.earlier).end, l.get(c.later).start);
            }
        } else {
            throw new FAPEException("Unknown option.");
        }
        next.conNet.CheckConsistency();
        return next.conNet.PropagateAndCheckConsistency(next); //if the propagation failed and we have achieved an inconsistent state
    }

    /**
     *
     */
    public enum EPlanState {

        /**
         *
         */
        CONSISTENT,
        /**
         *
         */
        INCONSISTENT,
        /**
         *
         */
        INFESSIBLE,
        /**
         *
         */
        UNINITIALIZED
    }
    /**
     * what is the current state of the plan
     */
    public EPlanState planState = EPlanState.UNINITIALIZED;

    //current best state
    private State best = null;

    /**
     * initializes the data structures of the planning problem
     *
     */
    public void Init() {
        queue.Add(new State());
    }

    /**
     *
     * @return
     */
    public State GetCurrentState() {
        return queue.Peek();
    }
    /*
     private boolean dfsRec(State st) {
     if (st.consumers.isEmpty()) {
     this.planState = EPlanState.CONSISTENT;
     TinyLogger.LogInfo("Plan found:");
     st.taskNet.Report();
     return true;
     } else {
     for (TemporalDatabase db : st.consumers) {
     List<SupportOption> supporters = GetSupporters(db, st);
     if (supporters.isEmpty()) {
     return false; //dead end
     }
     for (SupportOption o : supporters) {
     State next = new State(st);
     boolean suc = ApplyOption(next, o, db);
     TinyLogger.LogInfo(next.Report());
     if (suc) {
     if (dfsRec(next)) {
     return true;
     }
     } else {
     TinyLogger.LogInfo("Dead-end reached for state: " + next.mID);
     //inconsistent state, doing nothing
     }
     }
     }
     return false;
     }
     }

     private void dfs(TimeAmount forHowLong) {
     dfsRec(queue.Pop());
     }
     */
    Comparator<Pair<TemporalDatabase, List<SupportOption>>> optionsComparatorMinDomain = new Comparator<Pair<TemporalDatabase, List<SupportOption>>>() {
        @Override
        public int compare(Pair<TemporalDatabase, List<SupportOption>> o1, Pair<TemporalDatabase, List<SupportOption>> o2) {
            return o1.value2.size() - o2.value2.size();
        }
    };

    Comparator<Pair<TemporalDatabase, List<SupportOption>>> optionsActionsPreffered = new Comparator<Pair<TemporalDatabase, List<SupportOption>>>() {
        @Override
        public int compare(Pair<TemporalDatabase, List<SupportOption>> o1, Pair<TemporalDatabase, List<SupportOption>> o2) {
            int sum1 = 0, sum2 = 0;
            for (SupportOption op : o1.value2) {
                if (op.supportingAction != null) {
                    sum1 += 1;
                } else {
                    sum1 += 100;
                }
            }
            for (SupportOption op : o2.value2) {
                if (op.supportingAction != null) {
                    sum2 += 1;
                } else {
                    sum2 += 100;
                }
            }
            return sum1 - sum2;
        }
    };

    private State aStar(TimeAmount forHowLong) {
        // first start by checking all the consistencies and propagating necessary constraints
        // those are irreversible operations, we do not make any decisions on them
        //State st = GetCurrentState();
        //
        //st.bindings.PropagateNecessary(st);
        //st.tdb.Propagate(st);

        /**
         * search
         */
        while (true) {
            if (queue.Empty()) {
                TinyLogger.LogInfo("No plan found.");
                this.planState = EPlanState.INFESSIBLE;
                break;
            }
            //get the best state and continue the search
            State st = queue.Pop();
            TinyLogger.LogInfo(st.Report());
            if (st.consumers.isEmpty() && st.taskNet.GetOpenLeaves().isEmpty()) {
                this.planState = EPlanState.CONSISTENT;
                TinyLogger.LogInfo("Plan found:");
                TinyLogger.LogInfo(st.taskNet.Report());
                TinyLogger.LogInfo(st.tdb.Report());
                TinyLogger.LogInfo(st.tempoNet.Report());
                return st;
            }
            //continue the search
            LinkedList<Pair<TemporalDatabase, List<SupportOption>>> opts = new LinkedList<>();
            for (TemporalDatabase db : st.consumers) {
                List<SupportOption> supporters = GetSupporters(db, st);
                opts.add(new Pair(db, supporters));
            }
            //do some sorting here - min domain
            //Collections.sort(opts, optionsComparatorMinDomain);
            Collections.sort(opts, optionsComparatorMinDomain);

            if (opts.isEmpty()) {
                TinyLogger.LogInfo("Dead-end, no options: " + st.mID);
                //dead end
                continue;
            }

            if (opts.getFirst().value2.isEmpty()) {
                TinyLogger.LogInfo("Dead-end, consumer without resolvers: " + st.mID);
                //dead end
                continue;
            }

            for (Pair<TemporalDatabase, List<SupportOption>> opt : opts) {
                for (SupportOption o : opt.value2) {
                    State next = new State(st);
                    boolean suc = ApplyOption(next, o, next.GetConsumer(opt.value1));
                    //TinyLogger.LogInfo(next.Report());
                    if (suc) {
                        queue.Add(next);
                    } else {
                        TinyLogger.LogInfo("Dead-end reached for state: " + next.mID);
                        //inconsistent state, doing nothing
                    }
                }
            }
        }
        return null;
    }

    /**
     * starts plan repair, records the best plan, produces the best plan after
     * <b>forHowLong</b> miliseconds or null, if no plan was found
     *
     * @param forHowLong
     */
    public void Repair(TimeAmount forHowLong) {
        best = aStar(forHowLong);
        //dfs(forHowLong);
    }

    /**
     *
     * @param db
     * @param st
     * @return
     */
    public List<SupportOption> GetSupporters(TemporalDatabase db, State st) {
        //here we need to find several types of supporters
        //1) chain parts that provide the value we need
        //2) actions that provide the value we need and can be added
        //3) tasks that can decompose into an action we need
        List<SupportOption> ret = new LinkedList<>();

        //get chain connections
        for (TemporalDatabase b : st.tdb.vars) {
            if (db == b || !TemporalDatabase.Unifiable(db, b)) {
                continue;
            }
            if (db.HasSinglePersistence()) {
                //we are looking for chain integration too
                int ct = 0;
                for (TemporalDatabase.ChainComponent comp : b.chain) {
                    if (comp.change && comp.GetSupportValue().Unifiable(db.GetGlobalConsumeValue())
                            && st.tempoNet.CanBeBefore(comp.GetSupportTimePoint(), db.GetConsumeTimePoint())) {
                        SupportOption o = new SupportOption();
                        o.precedingChainComponent = ct;
                        o.temporalDatabase = b.mID;
                        ret.add(o);
                    }
                    ct++;
                }
            } else {
                if (b.GetGlobalSupportValue().Unifiable(db.GetGlobalConsumeValue())
                        && st.tempoNet.CanBeBefore(b.GetSupportTimePoint(), db.GetConsumeTimePoint())) {
                    SupportOption o = new SupportOption();
                    o.temporalDatabase = b.mID;
                    ret.add(o);
                }
            }
        }

        // adding actions
        // ... the idea is to decompose actions as long as they provide some support that I need, if they cant, I start adding actions
        //find actions that help me with achieving my value through some decomposition in the task network
        //they are those that I can find in the virtual decomposition tree
        //first get the action names from the abstract dtgs
        //StateVariable[] varis = null;
        //varis = db.domain.values().toArray(varis);
        ADTG dtg = dtgs.get(db.domain.getFirst().type);
        HashSet<String> abs = dtg.GetActionSupporters(db);
        //now we need to gather the decompositions that provide the intended actions
        List<SupportOption> options = st.taskNet.GetDecompositionCandidates(abs, actions);
        ret.addAll(options);

        //now we can look for adding the actions ad-hoc ...
        if (Planner.actionResolvers) {
            for (String s : abs) {
                SupportOption o = new SupportOption();
                o.supportingAction = actions.get(s);
                ret.add(o);
            }
        }

        return ret;
    }

    /*public void DFS(State st) {
     st.tdb.Propagate(st);
     for (TemporalDatabase db : st.consumers) {
     List<TemporalDatabase> supporters = st.tdb.GetSupporters(db);

     }
     }*/
    /**
     * progresses in the plan up for howFarToProgress, returns either
     * AtomicActions that were instantiated with corresponding start times, or
     * null, if not solution was found in the given time
     *
     * @param howFarToProgress
     * @param forHowLong
     * @return
     */
    public List<Pair<AtomicAction, Long>> Progress(TimeAmount howFarToProgress, TimeAmount forHowLong) {
        State myState = best;

        List<Pair<AtomicAction, Long>> ret = new LinkedList<>();
        List<Action> l = myState.taskNet.GetAllActions();
        for (Action a : l) {
            long startTime = myState.tempoNet.GetEarliestStartTime(a.start);
            AtomicAction aa = new AtomicAction();
            aa.mID = a.mID;
            aa.duration = (int) a.maxDuration;
            aa.name = a.name;
            aa.params = a.ProduceParameters(myState);
            ret.add(new Pair(aa, startTime));
        }
        return ret;
    }

    /**
     * restarts the planning problem into its initial state
     */
    public void Restart() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * enforces given facts into the plan (possibly breaking it) this is an
     * incremental step, if there was something already defined, the name
     * collisions are considered to be intentional
     *
     * @param pl
     */
    public void ForceFact(ANMLBlock pl) {
        //read everything that is contained in the ANML block

        //TODO: apply ANML to more states and choose the best after the applciation
        State st = GetCurrentState();

        // this a generic predecesor of all types
        if (st.isInitState) {
            types.put("object", new fape.core.planning.model.Type("object"));
            Type bool = new fape.core.planning.model.Type("boolean");
            bool.AddInstance("true");
            bool.AddInstance("false");
            types.put("boolean", bool);
            //STNManager.Init();
        }

        //convert types
        for (fape.core.execution.model.types.Type t : pl.types) {
            types.put(t.name, TransitionIO2Planning.transformType(t, types));
        }

        //convert instances and create state variables from them
        for (Instance i : pl.instances) {
            types.get(i.type).AddInstance(i.name); //this is all of them!
            fape.core.execution.model.types.Type tp = null;
            for (fape.core.execution.model.types.Type t : pl.types) {
                if (t.name.equals(i.type)) {
                    tp = t;
                }
            }
            if (tp == null) {
                throw new FAPEException("Unknown parent type.");
            }
            if (tp.parent != null) {
                types.get(tp.parent).AddInstance(i.name); //this is all of them!
            }
            List<StateVariable> l = TransitionIO2Planning.decomposeInstance("", i.name, i.type, types, i.type, true);
            for (StateVariable v : l) {
                vars.put(v.name, v);
            }
        }

        //process statements
        for (Statement s : pl.statements) {
            if (!vars.containsKey(s.GetVariableName())) {
                throw new FAPEException("Unknown state variable: " + s.GetVariableName());
            }
            TransitionIO2Planning.InsertStatementIntoState(s, vars.get(s.GetVariableName()), st);
        }

        //process actions
        for (fape.core.execution.model.Action a : pl.actions) {
            if (actions.containsKey(a.name)) {
                throw new FAPEException("Overriding action abstraction: " + a.name);
            }
            AbstractAction act = TransitionIO2Planning.TransformAction(a, vars, st.conNet, types);
            actions.put(act.name, act);
        }

        //process seeds
        for (ActionRef ref : pl.actionsForTaskNetwork) {
            AddAction(ref, st, null, false);
        } //end of seed processing

        //add the values of state variables
        /*for (StateVariable v : vars.values()) {
         Type t = types.get(v.type);
         t.AddInstance(v.name);
            
            
            
         }*/
        /*
         //add the state variabel value indexes into the abstract actions
         for(AbstractAction a:actions.values()){
         for(AbstractTemporalEvent e:a.events){
         if(e.event instanceof TransitionEvent){
         TransitionEvent ev = (TransitionEvent) e.event;    
         ev.from.index = types.get(e.varType).instances.get(ev.from.value);
         ev.to.index = types.get(e.varType).instances.get(ev.to.value);
         }else if(e.event instanceof PersistenceEvent){
         PersistenceEvent ev = (PersistenceEvent) e.event;                    
         ev.value.index = types.get(e.varType).instances.get(ev.value.value);
         }
         }
         }*/
        for (Type t : types.values()) {
            if (Character.isUpperCase(t.name.charAt(0)) || t.name.equals("boolean")) {//this is an enum type
                ADTG dtg = new ADTG(t, actions.values());

                dtg.op_all_paths();
                dtgs.put(t.name, dtg);
            }
        }

        //get values for the state variables
        /*for (AbstractAction a : actions.values()) {
         for (AbstractTemporalEvent e : a.events) {
         if (e.event instanceof TransitionEvent || e.event instanceof PersistenceEvent) {
         String refe = e.stateVariableReference.GetTypeReference();

         int xx = 0;
         /*HashSet<String> hs = values.get(refe);
         if(hs == null){
         hs = new HashSet<>();
         }
         if(e.event instanceof TransitionEvent){
         hs.add(((TransitionEvent)e.event).from.value);
         hs.add(((TransitionEvent)e.event).to.value);
         }else{
         hs.add(((PersistenceEvent)e.event).value.value);
         }*/
        //}
        //}
        // }
        //create propagation schemas
        if (st.isInitState) {

            for (AbstractAction a : actions.values()) {
                HashMap<Integer, List<UnificationConstraintSchema>> schemas = new HashMap<>();
                int decompositionCounter = 0; //indexes which decomposition we are working on
                for (Pair<List<ActionRef>, List<TemporalConstraint>> p : a.strongDecompositions) {
                    List<UnificationConstraintSchema> cons = new LinkedList<>();
                    int referenceActionCounter = 0;
                    for (ActionRef rf : p.value1) {
                        AbstractAction rfAbs = actions.get(rf.name);
                        //now we prepare the bindings
                        //List<Pair<List<AbstractAction.SharedParameterStruct>, List<AbstractAction.SharedParameterStruct>>> pairs = new LinkedList<>();                        
                        int referenceActionParameterCounter = 0;
                        for (Reference ref : rf.args) {
                            if (ref.refs.toString().endsWith("_")) {
                                continue; //this is unbinded reference
                            }
                            int mainActionParameterCounter = 0;
                            for (Instance i : a.params) {
                                if (i.name.equals(ref.GetConstantReference())) {
                                    //now we create the constrain schemas for all the combinations of events and values that share the parameter
                                    for (AbstractAction.SharedParameterStruct u : a.param2Event.get(mainActionParameterCounter)) {
                                        for (AbstractAction.SharedParameterStruct v : rfAbs.param2Event.get(referenceActionParameterCounter)) {
                                            cons.add(new UnificationConstraintSchema(decompositionCounter, u.relativeEventIndex, referenceActionCounter, v.relativeEventIndex, u.type, v.type));
                                        }
                                    }
                                }
                                mainActionParameterCounter++;
                            }
                            referenceActionParameterCounter++;
                        }

                        /*
                         AbstractAction abs = actions.get(rf.name);
                         for (Instance i : a.params) {
                         int subActionParameterCounter = 0;
                         for (Reference r : rf.args) {
                         if (r.refs.get(0).equals(i.name)) {
                         //this parameter is passed by, now we need to find the pairs of events
                         for (int mainEventCount = 0; mainEventCount < a.events.size(); mainEventCount++) {
                         for (int subEventCount = 0; subEventCount < abs.events.size(); subEventCount++) {
                         if (abs.events.get(subEventCount).stateVariableReference.refs.getFirst().equals(abs.params.get(subActionParameterCounter).name)
                         && a.events.get(mainEventCount).stateVariableReference.refs.getFirst().equals(i.name)) {
                         
                         }
                         }
                         }

                         }
                         subActionParameterCounter++;
                         }
                         }*/
                        referenceActionCounter++;
                    }
                    schemas.put(decompositionCounter, cons);
                    decompositionCounter++;
                }
                unificationConstraintPropagationSchema.put(a, schemas);
            }
        }
        int xx = 0;
    }

    /**
     *
     * @param ref
     * @param st
     * @param parent
     * @return
     */
    public Action AddAction(ActionRef ref, State st, Action parent, boolean enforceDuration) {
        AbstractAction abs = actions.get(ref.name);
        if (abs == null) {
            throw new FAPEException("Seeding unknown action: " + ref.name);
        }

        Action act = new Action();
        act.params = abs.params;
        act.constantParams = ref.args;
        if (parent != null) {
            //we need to pair arguments of the method with the parameters of the added action
            int refCt = 0;
            for (Reference r : act.constantParams) {
                String firstObject = r.GetConstantReference();
                int ct = 0;
                for (Instance rr : parent.params) {
                    if (firstObject.equals(rr.name)) {
                        act.constantParams.get(refCt).ReplaceFirstReference(parent.constantParams.get(ct));
                    }
                    ct++;
                }
                refCt++;
            }
        }
        // set the same name
        act.name = abs.name;
        //prepare the time points
        //add the refinements 
        act.refinementOptions = abs.strongDecompositions;

        act.start = st.tempoNet.getNewTemporalVariable();
        act.end = st.tempoNet.getNewTemporalVariable();
        if (enforceDuration) {
            act.maxDuration = abs.GetDuration();
            boolean success = st.tempoNet.EnforceConstraint(act.start, act.end, (int) act.maxDuration, (int) act.maxDuration);
            if (!success) {
                throw new FAPEException("The temporal network is inconsistent.");
            }
        } else {
            st.tempoNet.EnforceBefore(act.start, act.end);
        }

        //ArrayList<TemporalDatabase> dbList = new ArrayList<>();
        //set up the events
        for (AbstractTemporalEvent ev : abs.events) {

            TemporalEvent event = ev.event.cc(st.conNet, true);

            // sets the temporal interval into the context of the temporal context of the parent action
            ev.interval.AssignTemporalContext(event, act.start, act.end);

            String stateVariableReferenceSuffix = ev.stateVariableReference.toString();
            stateVariableReferenceSuffix = stateVariableReferenceSuffix.substring(stateVariableReferenceSuffix.indexOf("."), stateVariableReferenceSuffix.length());

            TemporalDatabase db = st.tdb.GetNewDatabase(st.conNet);
            event.mDatabase = db;
            //dbList.add(db);
            for (int i = 0; i < abs.params.size(); i++) {
                Instance instanceOfTheParameter = abs.params.get(i);
                if (instanceOfTheParameter.name.equals(ev.stateVariableReference.refs.getFirst())) {
                    //this is the right parameter
                    Reference re = ref.args.get(i);

                    if (re.refs.getFirst().endsWith("_")) {
                        // this is an unbinded reference, we need to add all possible state variables     
                        String typeDerivation = instanceOfTheParameter.type + stateVariableReferenceSuffix;
                        for (String str : vars.keySet()) {
                            StateVariable var = vars.get(str);
                            if (var.typeDerivationName.equals(typeDerivation)) {
                                db.domain.add(vars.get(str));
                            }
                        }
                    } else {
                        // this is binded to a constant - one specific state variable
                        StateVariable var = vars.get(re.toString() + stateVariableReferenceSuffix);
                        if (var == null) {
                            throw new FAPEException("Unknown state variable.");
                        }
                        //obj.domain.add(var);
                        db.domain.add(var);
                    }
                    db.AddActionParam(act.mID, instanceOfTheParameter);
                }
            }

            //we add the event into the database and the action and the consumers
            act.events.add(event);
            st.consumers.add(db);
            db.AddEvent(event);
        }//event transformation end

        //now we need to propagate the binding constraints, those are the binding constraints between events in one action
        for (List<AbstractAction.SharedParameterStruct> structs : abs.param2Event) {
            for (int ii = 0; ii < structs.size(); ii++) {
                for (int jj = ii + 1; jj < structs.size(); jj++) {
                    IUnifiable uni1 = act.GetUnifiableComponent(structs.get(ii));
                    IUnifiable uni2 = act.GetUnifiableComponent(structs.get(jj));
                    st.conNet.AddUnificationConstraint(uni1, uni2);
                }
            }
        }

        //lets add the action into the task network
        if (parent == null) {
            st.taskNet.AddSeed(act);
        } else {
            parent.decomposition.add(act);
        }
        //add 

        return act;
    }

}
