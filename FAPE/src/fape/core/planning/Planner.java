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
import java.math.BigDecimal;
import java.util.ArrayList;
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

    /**
     *
     */
    public static int searchWidth = 5;

    //public State init;
    // a list of types keyed by its name
    /**
     *
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
        //now we can happily apply all the options
        if (o.tdb != null && o.precedingComponent != null) {
            // this is database merge of one persistence into another
            int index = o.tdb.chain.indexOf(o.precedingComponent);
            if (o.tdb.chain.size() - 1 != index && !o.tdb.chain.get(index + 1).change) {
                //we add into an existing partially ordered set of perstistance events
                TemporalDatabase.ChainComponent comp = o.tdb.chain.get(index + 1);
                comp.Add(consumer.chain.get(0));
                //propagate time constraints
                TemporalDatabase.PropagatePrecedence(o.precedingComponent, comp, next);
                if (o.tdb.chain.size() > index + 2) {
                    TemporalDatabase.ChainComponent comp2 = o.tdb.chain.get(index + 2);
                    TemporalDatabase.PropagatePrecedence(comp, comp2, next);
                }
                //merge databases
                next.tdb.Merge(o.tdb, consumer);
            } else {
                //add a new persistence just after the chosen element
                o.tdb.chain.add(index + 1, consumer.chain.get(0));
                TemporalDatabase.PropagatePrecedence(o.precedingComponent, o.tdb.chain.get(index + 1), next);
                if (o.tdb.chain.size() > index + 2) {
                    TemporalDatabase.ChainComponent comp2 = o.tdb.chain.get(index + 2);
                    TemporalDatabase.PropagatePrecedence(o.tdb.chain.get(index + 1), comp2, next);
                }
                next.tdb.Merge(o.tdb, consumer);
            }
        } else if (o.tdb != null) {
            //this is a database concatenation
            if (!o.tdb.chain.getLast().change && !consumer.chain.getFirst().change) {
                //merge the changes
                TemporalDatabase.ChainComponent second = o.tdb.chain.getLast(),
                        third = consumer.chain.get(1),
                        first = o.tdb.chain.get(o.tdb.chain.size() - 2);
                for (TemporalEvent e : consumer.chain.getFirst().contents) {
                    o.tdb.chain.getLast().contents.add(e);
                }
                consumer.chain.removeFirst();
                for (TemporalDatabase.ChainComponent c : consumer.chain) {
                    o.tdb.chain.add(c);
                }
                TemporalDatabase.PropagatePrecedence(first, second, next);
                TemporalDatabase.PropagatePrecedence(second, third, next);
                next.tdb.Merge(o.tdb, consumer);
            } else {
                //concatenate
                TemporalDatabase.ChainComponent second = o.tdb.chain.getLast(),
                        first = o.tdb.chain.getFirst();
                for (TemporalDatabase.ChainComponent c : consumer.chain) {
                    o.tdb.chain.add(c);
                }
                TemporalDatabase.PropagatePrecedence(first, second, next);
                next.tdb.Merge(o.tdb, consumer);
            }
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
            AddAction(ref, next);
        } else if (o.actionToDecompose != null) {
            // this is a task decomposition
            // we need to decompose all the actions of the chosen decomposition
            // which represents adding all of them into the plan through AddAction and
            // setting up precedence constraints for them
            List<ActionRef> actionsForDecomposition = null;
            List<TemporalConstraint> tempCons = null;
            int ct = 0;
            for (Pair<List<ActionRef>, List<TemporalConstraint>> p : o.actionToDecompose.refinementOptions) {
                if (ct++ == o.decompositionID) {
                    actionsForDecomposition = p.value1;
                    tempCons = p.value2;
                }
            }
            //put the actions into the plan
            ArrayList<Action> l = new ArrayList<>();
            for (ActionRef ref : actionsForDecomposition) {
                l.add(AddAction(ref, next));
            }

            //now we need to introduce binding constraints between the events through the parameter binding
            //add the constraints parent-child temporal constraints
            for (Action a : l) {
                next.tempoNet.EnforceBefore(o.actionToDecompose.start, a.start);
                next.tempoNet.EnforceBefore(a.end, o.actionToDecompose.end);
                List<UnificationConstraintSchema> lt = unificationConstraintPropagationSchema.get(actions.get(o.actionToDecompose.name)).get(o.decompositionID);
                for (UnificationConstraintSchema s : lt) {
                    next.conNet.AddUnificationConstraint(o.actionToDecompose.events.get(s.mEventID).mDatabase, a.events.get(s.actionEventID).mDatabase);
                }
            }
            //add temporal constraints between actions
            for (TemporalConstraint c : tempCons) {
                next.tempoNet.EnforceBefore(l.get(c.earlier).end, l.get(c.later).start);
            }
        } else {
            throw new FAPEException("Unknown option.");
        }

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

    /**
     * starts plan repair, records the best plan, produces the best plan after
     * <b>forHowLong</b> miliseconds or null, if no plan was found
     *
     * @param forHowLong
     */
    public void Repair(TimeAmount forHowLong) {
        // first start by checking all the consistencies and propagating necessary constraints
        // those are irreversible operations, we do not make any decisions on them
        //State st = GetCurrentState();
        //
        //st.bindings.PropagateNecessary(st);
        //st.tdb.Propagate(st);

        /**
         * search
         */
        boolean end = false;
        while (!end) {
            if (queue.Empty()) {
                this.planState = EPlanState.INFESSIBLE;
                end = true;
            }
            State st = queue.Pop();
            if (st.consumers.isEmpty()) {
                this.planState = EPlanState.CONSISTENT;
                end = true;
            }
            for (TemporalDatabase db : st.consumers) {
                List<SupportOption> supporters = GetSupporters(db, st);
                for (SupportOption o : supporters) {
                    State next = new State(st);
                    if (ApplyOption(next, o, db)) {
                        queue.Add(next);
                    } else {
                        //inconsistent state, doing nothing
                    }
                }
            }
        }
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
                for (TemporalDatabase.ChainComponent comp : b.chain) {
                    if (comp.change && comp.GetSupportValue().Unifiable(db.GetGlobalConsumeValue())
                            && st.tempoNet.CanBeBefore(comp.GetSupportTimePoint(), db.GetConsumeTimePoint())) {
                        SupportOption o = new SupportOption();
                        o.precedingComponent = comp;
                        o.tdb = b;
                        ret.add(o);
                    }
                }
            } else {
                if (b.GetGlobalSupportValue().Unifiable(db.GetGlobalConsumeValue())
                        && st.tempoNet.CanBeBefore(b.GetSupportTimePoint(), db.GetConsumeTimePoint())) {
                    SupportOption o = new SupportOption();
                    o.tdb = b;
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
        HashSet<String> abs = dtg.GetActionSupporters(db.GetGlobalConsumeValue());
        //now we need to gather the decompositions that provide the intended actions
        List<SupportOption> options = st.taskNet.GetDecompositionCandidates(abs, actions);
        ret.addAll(options);

        //now we can look for adding the actions ad-hoc ...
        for (String s : abs) {
            SupportOption o = new SupportOption();
            o.supportingAction = actions.get(s);
            ret.add(o);
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
    public List<Pair<AtomicAction, TimePoint>> Progress(TimeAmount howFarToProgress, TimeAmount forHowLong) {
        throw new UnsupportedOperationException("Not yet implemented");
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
            //STNManager.Init();
        }

        //convert types
        for (fape.core.execution.model.types.Type t : pl.types) {
            types.put(t.name, TransitionIO2Planning.transformType(t, types));
        }

        //convert instances and create state variables from them
        for (Instance i : pl.instances) {
            types.get(i.type).AddInstance(i.name); //this is all of them!
            List<StateVariable> l = TransitionIO2Planning.decomposeInstance("", i.name, i.type, types, i.type);
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
            AbstractAction act = TransitionIO2Planning.TransformAction(a, vars);
            actions.put(act.name, act);
        }

        //process seeds
        for (ActionRef ref : pl.actionsForTaskNetwork) {
            AddAction(ref, st);
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
                int decCnt = 0;
                for (Pair<List<ActionRef>, List<TemporalConstraint>> p : a.strongDecompositions) {
                    List<UnificationConstraintSchema> cons = new LinkedList<>();
                    int refCnt = 0;
                    for (ActionRef rf : p.value1) {
                        //now check for each pair of events if they have the same variable used for them
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
                                                cons.add(new UnificationConstraintSchema(decCnt, mainEventCount, refCnt, subEventCount));
                                            }
                                        }
                                    }

                                }
                                subActionParameterCounter++;
                            }
                        }
                        refCnt++;
                    }
                    schemas.put(decCnt, cons);
                    decCnt++;
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
     * @return
     */
    public Action AddAction(ActionRef ref, State st) {
        AbstractAction abs = actions.get(ref.name);
        if (abs == null) {
            throw new FAPEException("Seeding unknown action: " + ref.name);
        }

        Action act = new Action();
        // set the same name
        act.name = abs.name;
        //prepare the time points
        //add the refinements 
        act.refinementOptions = abs.strongDecompositions;

        act.start = st.tempoNet.getNewTemporalVariable();
        act.end = st.tempoNet.getNewTemporalVariable();
        act.duration = abs.GetDuration();
        boolean success = st.tempoNet.EnforceConstraint(act.start, act.end, (int) act.duration, (int) act.duration);
        if (!success) {
            throw new FAPEException("The temporal network is inconsistent.");
        }

        ArrayList<TemporalDatabase> dbList = new ArrayList<>();
        //set up the events
        for (AbstractTemporalEvent ev : abs.events) {

            TemporalEvent event = ev.event.cc();

            // sets the temporal interval into the context of the temporal context of the parent action
            ev.interval.AssignTemporalContext(event, act.start, act.end);

            String stateVariableReferenceSuffix = ev.stateVariableReference.toString();
            stateVariableReferenceSuffix = stateVariableReferenceSuffix.substring(stateVariableReferenceSuffix.indexOf("."), stateVariableReferenceSuffix.length());

            TemporalDatabase db = st.tdb.GetNewDatabase();
            event.mDatabase = db;
            dbList.add(db);
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
                }
            }

            //we add the event into the database and the action
            act.events.add(event);
            db.AddEvent(event);
        }//event transformation end

        //now we need to propagate the binding constraints
        List<Pair<Integer, Integer>> binds = abs.GetLocalBindings();
        for (Pair<Integer, Integer> p : binds) {
            st.conNet.AddUnificationConstraint(dbList.get(p.value1), dbList.get(p.value2));
        }

        //lets add the action into the task network
        st.taskNet.AddSeed(act);
        //add 

        return act;
    }

}
