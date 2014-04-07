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
import fape.core.execution.model.AtomicAction;
import fape.core.planning.preprocessing.ActionDecompositions;
import fape.core.planning.preprocessing.ActionSupporters;
import fape.core.planning.printers.Printer;
import fape.core.planning.search.*;
import fape.core.planning.search.abstractions.AbstractionHierarchy;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.TimeAmount;
import fape.util.TinyLogger;
import planstack.anml.model.LVarRef;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Statement;
import planstack.anml.parser.ParseResult;

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

    public final AnmlProblem pb = new AnmlProblem();

    /**
     *
     */
    public PriorityQueue<State> queue = new PriorityQueue<State>(100, new StateComparator());

    private boolean ApplyOption(State next, SupportOption o, TemporalDatabase consumer) {
        TemporalDatabase supporter = null;
        ChainComponent precedingComponent = null;
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

            Action action = Factory.getStandaloneAction(pb, o.supportingAction);
            next.insert(action);

            // create the binding between consumer and the new statement in the action that supports it
            TemporalDatabase supportingDatabase = null;
            for (Statement s : action.statements()) {
                if(s instanceof LogStatement && next.canBeEnabler((LogStatement) s, consumer)) {
                    assert supportingDatabase == null : "Error: several statements might support the database";
                    supportingDatabase = next.tdb.getDBContaining((LogStatement) s);
                }
            }
            if (supportingDatabase == null) {
                return false;
            } else {
                SupportOption opt = new SupportOption();
                opt.temporalDatabase = supportingDatabase.mID;
                return ApplyOption(next, opt, consumer);
            }
        } else if (o.actionToDecompose != null) {
            // Apply the i^th decomposition of o.actionToDecompose, where i is given by
            // o.decompositionID

            // Action to decomposed
            Action decomposedAction = o.actionToDecompose;

            // Abstract version of the decomposition
            AbstractDecomposition absDec = decomposedAction.decompositions().get(o.decompositionID);

            // Decomposition (ie implementing StateModifier) containing all changes to be made to a search state.
            Decomposition dec = Factory.getDecomposition(pb, decomposedAction, absDec);
            next.applyDecomposition(dec);
        } else if(o.actionWithBindings != null) {
            Action act = Factory.getStandaloneAction(pb, o.actionWithBindings.act);
            next.insert(act);

            // restrict domain of given variables to the given set of variables.
            for(LVarRef lvar : o.actionWithBindings.values.keySet()) {
                next.conNet.restrictDomain(act.context().getGlobalVar(lvar), o.actionWithBindings.values.get(lvar));
            }
            // create the binding between consumer and the new statement in the action that supports it
            TemporalDatabase supportingDatabase = null;
            for (Statement s : act.statements()) {
                if(s instanceof LogStatement && next.canBeEnabler((LogStatement) s, consumer)) {
                    assert supportingDatabase == null : "Error: several statements might support the database";
                    supportingDatabase = next.tdb.getDBContaining((LogStatement) s);
                }
            }
            if (supportingDatabase == null) {
                return false;
            } else {
                SupportOption opt = new SupportOption();
                opt.temporalDatabase = supportingDatabase.mID;
                return ApplyOption(next, opt, consumer);
            }

        } else {
            throw new FAPEException("Unknown option.");
        }

        // if the propagation failed and we have achieved an inconsistent state
        return next.isConsistent();
    }

    /**
     * we remove the action results from the system
     *
     * @param
     */
    public void FailAction(ActRef actionRef) {
        KeepBestStateOnly();
        if (best == null) {
            throw new FAPEException("No current state.");
        } else {
            State bestState = GetCurrentState();

            bestState.setActionFailed(actionRef);
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
    public void AddActionEnding(ActRef actionID, int realEndTime) {
        KeepBestStateOnly();
        State bestState = GetCurrentState();
        bestState.setActionSuccess(actionID);
        Action a = bestState.taskNet.GetAction(actionID);
        // remove the duration constraints of the action
        bestState.tempoNet.RemoveConstraints(new Pair(a.start(), a.end()), new Pair(a.end(), a.start()));
        // insert new constraint specifying the end time of the action
        bestState.tempoNet.EnforceConstraint(pb.start(), a.end(), realEndTime, realEndTime);
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
        s.tempoNet.EnforceDelay(pb.start(), pb.earliestExecution(), earliestExecution);
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
        queue.clear();

        if (best == null) {
            TinyLogger.LogInfo("No known best state.");
        } else {
            queue.add(best);
        }

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
            for(int decompositionID=0 ; decompositionID < ua.action.decompositions().size() ; decompositionID++) {
                SupportOption res = new SupportOption();
                res.actionToDecompose = ua.action;
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
        AbstractionHierarchy hierarchy = new AbstractionHierarchy(pb);
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

            TinyLogger.LogInfo(st);
            if (st.consumers.isEmpty() && st.taskNet.GetOpenLeaves().isEmpty()) {
                this.planState = EPlanState.CONSISTENT;
                TinyLogger.LogInfo("Plan found:");
                TinyLogger.LogInfo(st.taskNet);
                TinyLogger.LogInfo(st.tdb);
                TinyLogger.LogInfo(st.tempoNet);
                return st;
            }
            //continue the search
            LinkedList<Pair<Flaw, List<SupportOption>>> opts = new LinkedList<>();
            for(Flaw flaw : GetFlaws(st)) {
                opts.add(new Pair(flaw, GetResolvers(st, flaw)));
            }

            //do some sorting here - min domain
            //Collections.sort(opts, optionsComparatorMinDomain);
            Collections.sort(opts, new FlawSelector(hierarchy, st));

            if (opts.isEmpty()) {
                throw new FAPEException("Error: no flaws but state was not found to be a solution.");
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
            // if the database has a single persistence we try to integrate it with other persistences.
            // except if the state variable is constant, in which case looking only for the assignments saves search effort.
            if (db.HasSinglePersistence() && !db.stateVariable.func().isConstant()) {
                //we are looking for chain integration too
                int ct = 0;
                for (ChainComponent comp : b.chain) {
                    if (comp.change && st.Unifiable(comp.GetSupportValue(), db.GetGlobalConsumeValue())
                            && st.tempoNet.CanBeBefore(comp.getSupportTimePoint(), db.getConsumeTimePoint())) {
                        SupportOption o = new SupportOption();
                        o.precedingChainComponent = ct;
                        o.temporalDatabase = b.mID;
                        ret.add(o);
                    }
                    ct++;
                }
            } else {
                if (st.Unifiable(b.GetGlobalSupportValue(), db.GetGlobalConsumeValue())
                        && st.tempoNet.CanBeBefore(b.getSupportTimePoint(), db.getConsumeTimePoint())) {
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

        ActionSupporters supporters = new ActionSupporters(pb);
        ActionDecompositions decompositions = new ActionDecompositions(pb);
        Collection<AbstractAction> potentialSupporters = supporters.getActionsSupporting(db);

        for(Action leaf : st.taskNet.GetOpenLeaves()) {
            for(Integer decID : decompositions.possibleDecompositions(leaf, potentialSupporters)) {
                SupportOption opt = new SupportOption();
                opt.actionToDecompose = leaf;
                opt.decompositionID = decID;
                ret.add(opt);
            }
        }


        //now we can look for adding the actions ad-hoc ...
        if (Planner.actionResolvers) {
            for (AbstractAction aa : potentialSupporters) {
                SupportOption o = new SupportOption();
                o.supportingAction = aa;
                ret.add(o);
            }
        }

        return ret;
    }

    /**
     * progresses in the plan up for howFarToProgress, returns either
     * AtomicActions that were instantiated with corresponding start times, or
     * null, if not solution was found in the given time
     *
     * @param howFarToProgress
     * @return
     */
    public List<AtomicAction> Progress(TimeAmount howFarToProgress) {
        State myState = best;
        Plan plan = new Plan(myState);

        List<AtomicAction> ret = new LinkedList<>();
        for (Action a : plan.GetNextActions()) {
            long startTime = myState.tempoNet.GetEarliestStartTime(a.start());
            if (startTime > howFarToProgress.val) {
                continue;
            }
            if(a.status() != ActionStatus.PENDING) {
                continue;
            }
            AtomicAction aa = new AtomicAction(a, startTime, a.maxDuration(), best);
            ret.add(aa);
        }

        Collections.sort(ret, new Comparator<AtomicAction>() {
            @Override
            public int compare(AtomicAction o1, AtomicAction o2) {
                return (int) (o1.mStartTime - o2.mStartTime);
            }
        });

        // for all selecting actions, we set them as being executed and we bind their start time point
        // to the one we requested.
        for(AtomicAction aa : ret) {
            Action a = myState.taskNet.GetAction(aa.id);
            myState.setActionExecuting(a.id());
            myState.tempoNet.RemoveConstraints(new Pair(pb.earliestExecution(), a.start()),
                    new Pair(a.start(), pb.earliestExecution()));
            myState.tempoNet.EnforceConstraint(pb.start(), a.start(), (int) aa.mStartTime, (int) aa.mStartTime);
        }

        return ret;
    }

    public boolean hasPendingActions() {
        for(Action a : best.taskNet.GetAllActions()) {
            if(a.status() == ActionStatus.PENDING)
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
     * enforces given facts into the plan (possibly breaking it) this is an
     * incremental step, if there was something already defined, the name
     * collisions are considered to be intentional
     *
     * @param anml
     */
    public void ForceFact(ParseResult anml) {
        //read everything that is contained in the ANML block
        if (logging) {
            TinyLogger.LogInfo("Forcing new fact into best state.");
        }

        KeepBestStateOnly();

        //TODO: apply ANML to more states and choose the best after the applciation
        pb.addAnml(anml);

        // apply revisions to best state and check if it is consistent
        State st = GetCurrentState();

        boolean consistent = st.update();
        if(!consistent) {
            this.planState = EPlanState.INFESSIBLE;
        }
    }

//    public static String DomainTableReportFormat() {
//        return String.format("%s\t%s\t%s\t",
//                "Num state variables",
//                "Num objects",
//                "Num actions");
//    }
//
//    public String DomainTableReport() {
//        return String.format("%s\t%s\t%s\t",
//                pb.vars.size(),
//                pb.types.instances().size(),
//                pb.actions.size());
//    }
//
//    public static String PlanTableReportFormat() {
//        return String.format("%s\t%s\t%s\t%s\t",
//                "Status",
//                "Plan length",
//                "Opened States",
//                "Generated States");
//    }
//
//    public String PlanTableReport() {
//        String status = "INCONS";
//        String planLength = "--";
//
//        if(planState == EPlanState.INFESSIBLE) {
//           status = "INFESS";
//        }
//        else if(planState == EPlanState.INCONSISTENT) {
//            status = "INCONS";
//            planLength = "--";
//        } else {
//            assert best != null;
//            status = "SOLVED";
//            planLength = ""+ best.taskNet.GetAllActions().size();
//        }
//        return String.format("%s\t%s\t%s\t%s\t",
//                status,
//                planLength,
//                OpenedStates,
//                GeneratedStates);
//    }

    /**
     * the goal is to solve a single problem for the given anml input and
     * produce the plan on the standard output
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        String anml = "../fape/FAPE/problems/handover.anml";

        if(args.length > 0)
            anml = args[0];
        Planner p = new Planner();
        Planner.actionResolvers = true;
        p.Init();
        p.ForceFact(Executor.ProcessANMLfromFile(anml));
        boolean timeOut = false;
        try {
            timeOut = !p.Repair(new TimeAmount(1000 * 6000));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Planning finished for " + anml + " with failure.");
            //throw new FAPEException("Repair failure.");
        }
        long end = System.currentTimeMillis();
        float total = (end - start) / 1000f;
        if (timeOut) {
            System.out.println("Planning finished for " + anml + " timed out.");
        } else {
            System.out.println("Planning finished for " + anml + " in " + total + "s");
            State sol = p.GetCurrentState();

            System.out.println("=== Temporal databases === \n"+ Printer.temporalDatabaseManager(sol, sol.tdb));

            Plan plan = new Plan(sol);
            plan.exportToDot("plan.dot");
            System.out.println("Look at plan.dot for a complete plan.");
        }
    }
}
