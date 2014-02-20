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

import fape.core.execution.Executor;
import fape.core.execution.model.*;
import fape.core.planning.model.TypeManager;
import fape.core.planning.dtgs.ADTG;
import fape.core.execution.model.statements.Statement;
//import fape.core.execution.model.types.Type;

import fape.core.planning.constraints.UnificationConstraintSchema;
import fape.core.planning.model.*;
import fape.core.planning.model.Action;
import fape.core.planning.search.*;
import fape.core.planning.search.abstractions.AbstractionHierarchy;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.core.transitions.TransitionIO2Planning;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.TimeAmount;
import fape.util.TinyLogger;
import java.util.*;

/**
 *
 * @author FD
 */
public class Planner {

    public static boolean debugging = true;
    public static boolean logging = true;
    public static boolean actionResolvers = true; // do we add actions to resolve flaws?

    public int GeneratedStates = 1; //count the initial state
    public int OpenedStates = 0;

    public Problem pb = new Problem();

    private int nextVarID = 0;

    /**
     * @return a new unused for an unbinded variable.
     */
    public String NewUnbindedVarName() {
        return "a" + nextVarID++ + "_";
    }

    /**
     *
     */
    public PriorityQueue<State> queue = new PriorityQueue<State>(100, new StateComparator());

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
            assert consumer != null : "Consumer was not passed as an argument";
            // this is database merge of one persistence into another
            assert consumer.chain.size() == 1 && !consumer.chain.get(0).change
                    : "This is restricted to databases containing single persistence only";

            next.tdb.InsertDatabaseAfter(next, supporter, consumer, precedingComponent);
        } else if (supporter != null) {
            assert consumer != null : "Consumer was not passed as an argument";
            // database concatenation
            next.tdb.InsertDatabaseAfter(next, supporter, consumer, supporter.chain.getLast());
        } else if (o.supportingAction != null) {
            assert consumer != null : "Consumer was not passed as an argument";
            //this is a simple applciation of an action
            ActionRef ref = new ActionRef();
            ref.name = o.supportingAction.name;
            int ct = 0;
            for (Instance i : o.supportingAction.params) {
                Reference rf = new Reference();
                rf.refs.add(NewUnbindedVarName()); //random unbinded parameters
                ref.args.add(rf);
            }
            boolean enforceDuration = true;
            if (!pb.actions.get(ref.name).strongDecompositions.isEmpty()) {
                enforceDuration = false;
            }
            Action addedAction = AddAction(ref, next, null, enforceDuration);
            // create the binding between consumer and the new statement in the action that supports it
            int supportingDatabase = -1;
            for (TemporalEvent e : addedAction.events) {
                if (e instanceof TransitionEvent) {
                    TransitionEvent ev = (TransitionEvent) e;
                    TemporalDatabase potSupporter = next.GetDatabase(ev.tdbID);
                    if(next.Unifiable(ev.to, consumer.GetGlobalConsumeValue()) && next.Unifiable(potSupporter.stateVariable, consumer.stateVariable)) {
                        supportingDatabase = ev.tdbID;
                    }
                }
            }
            if (supportingDatabase == -1) {
                return false;
            } else {
                SupportOption opt = new SupportOption();
                opt.temporalDatabase = supportingDatabase;
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
                l.add(AddAction(ref.cc(), next, decomposedAction, true));
            }

            //add the constraints parent-child temporal constraints
            for (Action a : l) {
                next.tempoNet.EnforceBefore(decomposedAction.start, a.start);
                next.tempoNet.EnforceBefore(a.end, decomposedAction.end);
            }
            //add temporal constraints between actions
            for (TemporalConstraint c : tempCons) {
                next.tempoNet.EnforceBefore(l.get(c.earlier).end, l.get(c.later).start);
            }
        } else {
            throw new FAPEException("Unknown option.");
        }

        // if the propagation failed and we have achieved an inconsistent state
        return next.tempoNet.IsConsistent() && next.conNet.PropagateAndCheckConsistency(next);
    }

    /**
     * we remove the action results from the system
     *
     * @param pop
     */
    public void FailAction(Integer pop) {
        KeepBestStateOnly();
        if (best == null) {
            throw new FAPEException("No current state.");
        } else {
            State bestState = GetCurrentState();
            Action remove = bestState.taskNet.GetAction(pop);
            if (remove == null) {
                throw new FAPEException("Unknown action.");
            }
            //
            for (TemporalEvent t : remove.events) {
                bestState.SplitDatabase(t);
            }
            remove.events = new LinkedList<>();
            bestState.FailAction(pop);
        }
    }

    /**
     * Set the action ending to the its real end time. It removes the duration
     * constraints between the starts and the end of the action (as given by the
     * duration anml variable). Adds a new constraint [realEndTime, realEndtime]
     * between the global start and the end of action time points.
     *
     * @param actionID
     * @param realEndTime
     */
    public void AddActionEnding(int actionID, int realEndTime) {
        KeepBestStateOnly();
        State bestState = GetCurrentState();
        bestState.taskNet.SetActionSuccess(actionID);
        Action a = bestState.taskNet.GetAction(actionID);
        // remove the duration constraints of the action
        bestState.tempoNet.RemoveConstraints(new Pair(a.start, a.end), new Pair(a.end, a.start));
        // insert new constraint specifying the end time of the action
        bestState.tempoNet.EnforceConstraint(bestState.tempoNet.GetGlobalStart(), a.end, realEndTime, realEndTime);
        TinyLogger.LogInfo("Overriding constraint.");
    }

    /**
     * Pushes the earliest execution time point forward.
     * Causes all pending actions to be delayed
     * @param earliestExecution
     */
    public void SetEarliestExecution(int earliestExecution) {
        KeepBestStateOnly();
        State s = GetCurrentState();
        s.tempoNet.EnforceDelay(s.tempoNet.GetGlobalStart(), s.tempoNet.GetEarliestExecution(), earliestExecution);

        // If the STN is not consistent after this addition, the the current plan is not feasible.
        // Full replan is necessary
        if(!s.tempoNet.IsConsistent()) {
            this.best = null;
            this.queue.clear();
        }
    }

    /**
     *
     */
    public enum EPlanState {

        TIMEOUT,
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
        queue.add(new State(pb));
        best = queue.peek();
    }

    /**
     *
     * @return
     */
    public State GetCurrentState() {
        return best;
    }

    /**
     * Remove all states in the queues except for the best one (which is stored
     * in best). This is to be used when updating the problem to make sure we
     * don't keep any outdated states.
     */
    public void KeepBestStateOnly() {
        if (best == null) {
            throw new FAPEException("No known best state.");
        }
        queue.clear();
        queue.add(best);
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
    Comparator<Pair<Flaw, List<SupportOption>>> optionsComparatorMinDomain = new Comparator<Pair<Flaw, List<SupportOption>>>() {
        @Override
        public int compare(Pair<Flaw, List<SupportOption>> o1, Pair<Flaw, List<SupportOption>> o2) {
            return o1.value2.size() - o2.value2.size();
        }
    };

    public class FlawSelector implements Comparator<Pair<Flaw, List<SupportOption>>> {

        private final State state;

        public FlawSelector(State st) {
            this.state = st;
        }

        private int priority(Pair<Flaw, List<SupportOption>> flawAndResolvers) {
            Flaw flaw = flawAndResolvers.value1;
            List<SupportOption> options = flawAndResolvers.value2;
            int base;
            if(options.size() <= 1) {
                base = 0;
            } else if(flaw instanceof UnsupportedDatabase) {
                TemporalDatabase consumer = ((UnsupportedDatabase) flaw).consumer;
                String predicate = consumer.stateVariable.predicateName;
                String argType = state.GetType(consumer.stateVariable.variable.GetReference());
                String valueType = state.GetType(consumer.GetGlobalConsumeValue().GetReference());
                int level = state.pb.hierarchy.getLevel(predicate, argType, valueType);
                base = (level +1) * 500;
            } else {
                base = 99999;
            }

            return base + options.size();
        }


        @Override
        public int compare(Pair<Flaw, List<SupportOption>> o1, Pair<Flaw, List<SupportOption>> o2) {
            return priority(o1) - priority(o2);
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

    public List<SupportOption> GetResolvers(State st, Flaw f) {
        if(f instanceof UnsupportedDatabase) {
            return GetSupporters(((UnsupportedDatabase) f).consumer, st);
        } else if(f instanceof UndecomposedAction) {
            UndecomposedAction ua = (UndecomposedAction) f;
            List<SupportOption> resolvers = new LinkedList<>();
            for(int decompositionID=0 ; decompositionID < ua.action.refinementOptions.size() ; decompositionID++) {
                SupportOption res = new SupportOption();
                res.actionToDecompose = ua.action.mID;
                res.decompositionID = decompositionID;
                resolvers.add(res);
            }
            return resolvers;
        } else {
            assert false : "Unknown flaw type: " + f;
            return new LinkedList<>();
        }
    }

    public List<Flaw> GetFlaws(State st) {
        List<Flaw> flaws = new LinkedList<>();
        for(TemporalDatabase consumer : st.consumers) {
            flaws.add(new UnsupportedDatabase(consumer));
        }
        for(Action refinable : st.taskNet.GetOpenLeaves()) {
            flaws.add(new UndecomposedAction(refinable));
        }
        return flaws;
    }


    private State aStar(TimeAmount forHowLong) {
        // first start by checking all the consistencies and propagating necessary constraints
        // those are irreversible operations, we do not make any decisions on them
        //State st = GetCurrentState();
        //
        //st.bindings.PropagateNecessary(st);
        //st.tdb.Propagate(st);
        long deadLine = System.currentTimeMillis() + forHowLong.val;
        /**
         * search
         */
        while (true) {
            if (System.currentTimeMillis() > deadLine) {
                TinyLogger.LogInfo("Timeout.");
                this.planState = EPlanState.INCONSISTENT;
                break;
            }
            if (queue.isEmpty()) {
                TinyLogger.LogInfo("No plan found.");
                this.planState = EPlanState.INFESSIBLE;
                break;
            }
            //get the best state and continue the search
            State st = queue.remove();
            OpenedStates++;

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
            LinkedList<Pair<Flaw, List<SupportOption>>> opts = new LinkedList<>();
            for(Flaw flaw : GetFlaws(st)) {
                opts.add(new Pair(flaw, GetResolvers(st, flaw)));
            }

            //do some sorting here - min domain
            //Collections.sort(opts, optionsComparatorMinDomain);
            Collections.sort(opts, new FlawSelector(st));

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

            //we just take the first option here as a tie breaker by min-domain
            Pair<Flaw, List<SupportOption>> opt = opts.getFirst();

            for (SupportOption o : opt.value2) {
                State next = new State(st);
                boolean success = false;
                if(opt.value1 instanceof UndecomposedAction) {
                    success = ApplyOption(next, o, null);
                } else {
                    success = ApplyOption(next, o, next.GetDatabase(((UnsupportedDatabase) opt.value1).consumer.mID));
                }
                //TinyLogger.LogInfo(next.Report());
                if (success) {
                    queue.add(next);
                    GeneratedStates++;
                } else {
                    TinyLogger.LogInfo("Dead-end reached for state: " + next.mID);
                    //inconsistent state, doing nothing
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
    public boolean Repair(TimeAmount forHowLong) {
        KeepBestStateOnly();
        best = aStar(forHowLong);
        if (best == null) {
            return false;
        }
        //dfs(forHowLong);

        //we empty the queue now and leave only the best state there
        KeepBestStateOnly();
        return true;
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
            if (db == b || !st.Unifiable(db, b)) {
                continue;
            }
            if (db.HasSinglePersistence()) {
                //we are looking for chain integration too
                int ct = 0;
                for (TemporalDatabase.ChainComponent comp : b.chain) {
                    if (comp.change && st.Unifiable(comp.GetSupportValue(), db.GetGlobalConsumeValue())
                            && st.tempoNet.CanBeBefore(comp.GetSupportTimePoint(), db.GetConsumeTimePoint())) {
                        SupportOption o = new SupportOption();
                        o.precedingChainComponent = ct;
                        o.temporalDatabase = b.mID;
                        ret.add(o);
                    }
                    ct++;
                }
            } else {
                if (st.Unifiable(b.GetGlobalSupportValue(), db.GetGlobalConsumeValue())
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


        ADTG dtg = pb.dtgs.get(db.stateVariable.type);
        HashSet<String> abs = dtg.GetActionSupporters(this, st, db);
        //now we need to gather the decompositions that provide the intended actions
        List<SupportOption> options = st.taskNet.GetDecompositionCandidates(abs, pb.actions);
        ret.addAll(options);

        //now we can look for adding the actions ad-hoc ...
        if (Planner.actionResolvers) {
            for (String s : abs) {
                SupportOption o = new SupportOption();
                o.supportingAction = pb.actions.get(s);
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
     * TODO: This is wrong we should only send actions whose dependencies are met (all provider tasks already executed)
     * progresses in the plan up for howFarToProgress, returns either
     * AtomicActions that were instantiated with corresponding start times, or
     * null, if not solution was found in the given time
     *
     * @param howFarToProgress
     * @param forHowLong
     * @return
     */
    public List<AtomicAction> Progress(TimeAmount howFarToProgress) {
        State myState = best;

        List<AtomicAction> ret = new LinkedList<>();
        List<Action> l = myState.taskNet.GetAllActions();
        for (Action a : l) {
            long startTime = myState.tempoNet.GetEarliestStartTime(a.start);
            if (startTime > howFarToProgress.val) {
                continue;
            }
            if(a.status != Action.Status.PENDING) {
                continue;
            }
            AtomicAction aa = new AtomicAction();
            aa.mStartTime = (int) startTime;
            aa.mID = a.mID;
            aa.duration = (int) a.maxDuration;
            aa.name = a.name;
            aa.params = a.ProduceParameters(myState);
            ret.add(aa);
        }

        Collections.sort(ret, new Comparator<AtomicAction>() {
            @Override
            public int compare(AtomicAction o1, AtomicAction o2) {
                return (int) (o1.mStartTime - o2.mStartTime);
            }
        });

        //TODO awful hack to only return the first task, we need to keep track of causal links to make it cleaner
        if(ret.size() > 0)
            ret = ret.subList(0,1);
        else
            ret = ret.subList(0,0);

        // for all selecting actions, we set them as being executed and we bind their start time point
        // to the one we requested.
        for(AtomicAction aa : ret) {
            Action a = myState.taskNet.GetAction(aa.mID);
            myState.taskNet.SetActionExecuting(a.mID);
            myState.tempoNet.RemoveConstraints(new Pair(myState.tempoNet.GetEarliestExecution(), a.start),
                    new Pair(a.start, myState.tempoNet.GetEarliestExecution()));
            myState.tempoNet.EnforceConstraint(myState.tempoNet.GetGlobalStart(), a.start, aa.mStartTime, aa.mStartTime);
        }

        return ret;
    }

    public boolean hasPendingActions() {
        for(Action a : best.taskNet.GetAllActions()) {
            if(a.status == Action.Status.PENDING)
                return true;
        }
        return false;
    }

    /**
     * restarts the planning problem into its initial state
     */
    public void Restart() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Catch up with all problem updates that were not forced into the state.
     * @param st
     * @return
     */
    public boolean updateState(State st) {
        // apply all pending revisions
        while(st.problemRevision < pb.currentRevision) {
            st.problemRevision++;

            // for every update in this revision, apply it to state.
            for(Problem.ProblemRevision update : pb.revisions.get(st.problemRevision)) {
                if(update.isActionAddition()) {
                    AddAction(update.addAction, st, null, false);
                } else if(update.isStatementAddition()) {
                    Statement s = update.statement;
                    TransitionIO2Planning.InsertStatementIntoState(st, s, pb.vars.get(s.GetVariableName()));
                } else if(update.isObjectAddition()) {
                    Instance i = update.object;
                    ObjectVariableValues binding = new ObjectVariableValues(i.name, i.type);
                    assert !st.parameterBindings.containsKey(i.name);
                    st.parameterBindings.put(i.name, binding);
                } else {
                    throw new FAPEException("Unrecognized problem revision: " + update);
                }
            }
        }

        // true if state is consistent
        return st.tempoNet.IsConsistent() && st.conNet.PropagateAndCheckConsistency(st);
    }

    /**
     * enforces given facts into the plan (possibly breaking it) this is an
     * incremental step, if there was something already defined, the name
     * collisions are considered to be intentional
     *
     * @param anml
     */
    public void ForceFact(ANMLBlock anml) {
        //read everything that is contained in the ANML block
        if (logging) {
            TinyLogger.LogInfo("Forcing new fact into best state.");
        }

        KeepBestStateOnly();

        //TODO: apply ANML to more states and choose the best after the applciation


        pb.ForceFact(anml);

        // apply revisions to best state and check if it is consistent
        State st = GetCurrentState();

        boolean consistent = updateState(st);
        if(!consistent) {
            this.planState = EPlanState.INFESSIBLE;
        }
    }

    /**
     *
     * @param ref
     * @param st
     * @param parent
     * @return
     */
    public Action AddAction(ActionRef ref, State st, Action parent, boolean enforceDuration) {

        AbstractAction abs = pb.actions.get(ref.name);
        if (abs == null) {
            throw new FAPEException("Seeding unknown action: " + ref.name);
        }

        Action act = new Action();
        act.params = abs.params;
        act.constantParams = ref.args;

        List<Pair<Reference,Reference>> hardBindings = new LinkedList<>();

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

            // this look for parameters that where given a stateVariable value from the parent action
            // (typically r.right), add a pair of references with the stateVariable and the parameter name
            for(refCt=0 ; refCt<act.constantParams.size() ; refCt++) {
                Reference r = act.constantParams.get(refCt);
                if(r.refs.size() > 1) {
                    String var = NewUnbindedVarName();
                    Reference varRef = new Reference(var);
                    hardBindings.add(new Pair(r, varRef));
                    act.constantParams.remove(refCt);
                    act.constantParams.add(refCt, varRef);
                }
            }
        }

        // for all parameters, create a new variable and add it to the state
        for(int i=0 ; i<act.params.size() ; i++) {
            Instance param = act.params.get(i);
            Reference paramRef = act.constantParams.get(i);
            String varName = paramRef.GetConstantReference();

            assert pb.types.containsType(param.type) : "Unknown type";

            if(!st.parameterBindings.containsKey(varName)) {
                // variable does not exists yet, create one
                if(varName.endsWith("_")) {
                    List<String> values = pb.types.instances(param.type);
                    ObjectVariableValues binding = new ObjectVariableValues(values, param.type);
                    st.parameterBindings.put(varName, binding);
                } else {
                    throw new FAPEException("This is a literal, should be already in variables: " + paramRef);
                }
            }
        }

        // set the same name
        act.name = abs.name;

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
        // enforce that the current action must be executed after a known time in the future
        st.tempoNet.EnforceBefore(st.tempoNet.GetEarliestExecution(), act.start);

        // This creates persistence event for every parameter of the form "r.right -> g"
        for(Pair<Reference,Reference> binding : hardBindings) {
            Reference svRef = act.BindedReference(binding.value1);

            // state variable is the reference passed (ex: r.right)
            String predType = st.GetType(svRef);
            VariableRef svParam = act.GetBindedVariableRef(svRef.variable(), act.GetType(st, svRef.variable()));
            ParameterizedStateVariable sv = new ParameterizedStateVariable(svRef.predicate(), svParam, predType);

            // create persistence on the value (ex: g)
            PersistenceEvent ev = new PersistenceEvent(sv, new VariableRef(binding.value2, st.GetType(binding.value2)));
            TemporalDatabase db = st.tdb.GetNewDatabase(st.conNet);
            ev.tdbID = db.mID;
            db.stateVariable = sv;

            // Event is forced during the whole action
            TemporalInterval all = new TemporalInterval("TStart", "TEnd");
            all.AssignTemporalContext(ev, act.start, act.end);

            act.events.add(ev);
            db.AddEvent(ev);
            if(db.isConsumer())
                st.consumers.add(db);
        }

        //set up the events
        for (AbstractTemporalEvent ev : abs.events) {
            // get the event binded into the current action
            TemporalEvent event = ev.GetEventInAction(st, act);

            // create new databse and
            TemporalDatabase db = st.tdb.GetNewDatabase(st.conNet);
            event.tdbID = db.mID;

            // Create a stateVariable for the database
            Reference finalRef = act.BindedReference(ev.stateVariableReference);
            String type = st.GetType(finalRef);
            db.stateVariable = event.stateVariable;

            //we add the event into the database and the action and the consumers
            act.events.add(event);
            db.AddEvent(event);
            if(db.isConsumer())
                st.consumers.add(db);

        }//event transformation end

        //lets add the action into the task network
        if (parent == null) {
            st.taskNet.AddSeed(act);
        } else {
            parent.decomposition.add(act);
        }

        return act;
    }

    public static String DomainTableReportFormat() {
        return String.format("%s\t%s\t",
                "Num state variables",
                "Num actions");
    }

    public String DomainTableReport() {
        return String.format("%s\t%s\t",
                pb.vars.size(),
                pb.actions.size());
    }

    public static String PlanTableReportFormat() {
        return String.format("%s\t%s\t%s\t%s\t",
                "Status",
                "Plan length",
                "Opened States",
                "Generated States");
    }

    public String PlanTableReport() {
        String status = "INCONS";
        String planLength = "--";

        if(planState == EPlanState.INFESSIBLE) {
           status = "INFESS";
        }
        else if(planState == EPlanState.INCONSISTENT) {
            status = "INCONS";
            planLength = "--";
        } else {
            assert best != null;
            status = "SOLVED";
            planLength = ""+ best.taskNet.GetAllActions().size();
        }
        return String.format("%s\t%s\t%s\t%s\t",
                status,
                planLength,
                OpenedStates,
                GeneratedStates);
    }

    /**
     * the goal is to solve a single problem for the given anml input and
     * produce the plan on the standard output
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        String anml = "problems/DreamDecomposition.anml";
        if(args.length > 0)
            anml = args[0];
        Planner p = new Planner();
        Planner.actionResolvers = true;
        p.Init();
        p.ForceFact(Executor.ProcessANMLfromFile(anml));
        boolean timeOut = false;
        try {
            timeOut = p.Repair(new TimeAmount(1000 * 6000));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Planning finished for " + anml + " with failure.");
            //throw new FAPEException("Repair failure.");
        }
        long end = System.currentTimeMillis();
        float total = (end - start) / 1000f;
        if (!timeOut) {
            System.out.println("Planning finished for " + anml + " timed out.");
        } else {
            System.out.println("Planning finished for " + anml + " in " + total + "s");
        }
    }
}
