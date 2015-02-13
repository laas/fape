package fape.core.planning.planner;

import fape.core.execution.model.AtomicAction;
import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.LiftedDTG;
import fape.core.planning.search.flaws.finders.*;
import fape.core.planning.search.flaws.flaws.*;
import fape.core.planning.search.flaws.resolvers.*;
import fape.core.planning.search.strategies.flaws.FlawCompFactory;
import fape.core.planning.search.strategies.plans.LMC;
import fape.core.planning.search.strategies.plans.PlanCompFactory;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.exceptions.FAPEException;
import fape.util.ActionsChart;
import fape.util.Pair;
import fape.util.TinyLogger;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.parser.ParseResult;
import planstack.constraints.stnu.Controllability;

import java.util.*;

/**
 * Base for any planner in FAPE. It defines all basic operations useful for
 * planning such as alterations of search states, inclusions of ANML blocks ...
 *
 * Classes that inherit from it only have to implement the abstract methods to
 * provide a search policy. Overriding methods can also be done to change the
 * default behaviour.
 */
public abstract class APlanner {

    public APlanner(State initialState, String[] planSelStrategies, String[] flawSelStrategies, Map<ActRef, ActionExecution> actionsExecutions) {
        this.pb = initialState.pb;
        assert pb.usesActionConditions() == this.useActionConditions() :
                "Difference between problem and planner in the handling action conditions";
        this.planSelStrategies = planSelStrategies;
        this.flawSelStrategies = flawSelStrategies;
        this.executedAction = actionsExecutions;
        this.controllability = initialState.controllability;
        this.dtg = new LiftedDTG(this.pb);
        queue = new PriorityQueue<>(100, this.stateComparator());
        queue.add(initialState);
        best = queue.peek();
    }

    public APlanner(Controllability controllability, String[] planSelStrategies, String[] flawSelStrategies) {
        this.planSelStrategies = planSelStrategies;
        this.flawSelStrategies = flawSelStrategies;
        this.controllability = controllability;
        this.executedAction = new HashMap<>();
        this.pb = new AnmlProblem(useActionConditions());
        this.dtg = new LiftedDTG(this.pb);
        this.queue = new PriorityQueue<>(100, this.stateComparator());
        queue.add(new State(pb, controllability));
        best = queue.peek();
    }



    public final Map<ActRef, ActionExecution> executedAction;
    public int currentTime = 0;

    @Deprecated //might not work in a general scheme were pultiple planner instances are intantiated
    public static APlanner currentPlanner = null;

    public static boolean debugging = true;
    public static boolean logging = true;
    public static boolean actionResolvers = true; // do we add actions to resolve flaws?

    public int GeneratedStates = 1; //count the initial state
    public int OpenedStates = 0;

    public final Controllability controllability;

    public final AnmlProblem pb;
    LiftedDTG dtg = null;

    /**
     * Those are used to extract all flaws from a state.
     * The GetFlaws method will typically use all of those
     * to generate the flaws that need to be solved in a given state.
     */
    protected final FlawFinder[] flawFinders = {
            new OpenGoalFinder(),
            new UndecomposedActionFinder(),
            new UnsupportedTaskConditionFinder(),
            new UnmotivatedActionFinder()
    };

    /**
     * Used to build comparators for flaws. Default to a least commiting first.
     */
    public final String[] flawSelStrategies;

    /**
     * Used to build comparators for partial plans.
     */
    public final String[] planSelStrategies;

    /**
     * A short identifier for the planner.
     *
     * @return THe planner ID.
     */
    public abstract String shortName();

    public abstract ActionSupporterFinder getActionSupporterFinder();

    /**
     * If this returns true, decomposition will yield ActionConditions instead of Actions.
     * ActionConditions results in flaws that can be solved by unifying the action condition with
     * an action.
     */
    public boolean useActionConditions() {
        return false;
    }

    /**
     *
     */
    protected final PriorityQueue<State> queue;

    /**
     * This method is invoked whenever a causal link is added and offers a way
     * to react to it for planner extending this class.
     *
     * @param st State in which the causal link was added.
     * @param supporter Left side of the causal link.
     * @param consumer Right side of the causal link.
     */
    public void causalLinkAdded(State st, LogStatement supporter, LogStatement consumer) {
    }

    /**
     * Removes the action results from the system.
     *
     * @param actionRef The action that failed.
     */
    public void FailAction(ActRef actionRef, int endTime) {
        KeepBestStateOnly();
        assert best != null;
        State st = GetCurrentState();
        st.setActionFailed(actionRef, endTime);
        executedAction.get(actionRef).setFailed();
    }

    /**
     * Set the action ending to the its real end time. It removes the duration
     * constraints between the starts and the end of the action (as given by the
     * duration anml variable). Adds a new constraint [realEndTime, realEndtime]
     * between the global start and the end of action time points.
     *
     * @param actionID Id of the action that finished.
     * @param realEndTime Observed end time.
     */
    public void AddActionEnding(ActRef actionID, int realEndTime) {
        KeepBestStateOnly();
        State st = GetCurrentState();
        Action a = st.getAction(actionID);
        st.setActionSuccess(a, realEndTime);
        executedAction.get(actionID).setSuccess(realEndTime);
    }

    public void setActionExecuting(AtomicAction aa) {
        State st = GetCurrentState();
        Action a = st.getAction(aa.id);
        st.setActionExecuting(a, (int) aa.mStartTime);
        executedAction.put(aa.id, new ActionExecution(a, aa.params, aa.mStartTime));
    }

    /**
     * Pushes the earliest execution time point forward. Causes all pending
     * actions to be delayed
     */
    public void SetEarliestExecution(int earliestExecution) {
        if(Plan.showChart)
            ActionsChart.setCurrentTime(earliestExecution);
        this.currentTime = earliestExecution;
        KeepBestStateOnly();
        State s = GetCurrentState();
        if(s != null) {
            s.enforceDelay(pb.start(), pb.earliestExecution(), earliestExecution);
            // If the STN is not consistent after this addition, the the current plan is not feasible.
            // Full replan is necessary
            if (!s.isConsistent()) {
                this.best = null;
                this.queue.clear();
            }
        }
    }

    /**
     * All possible state of the planner.
     */
    public enum EPlanState {
        TIMEOUT, CONSISTENT, INCONSISTENT, INFEASIBLE, UNINITIALIZED
    }
    /**
     * what is the current state of the plan
     */
    public EPlanState planState = EPlanState.UNINITIALIZED;

    //current best state
    private State best = null;

    /**
     * @return The current best state. Null if no consistent state was found.
     */
    @Deprecated
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

    /**
     * Finds all flaws of a given state. Currently, threats and unbound
     * variables are considered only if no other flaws are present.
     *
     * @return A list of flaws present in the system. The list of flaws might not
     * be exhaustive.
     */
    public List<Flaw> GetFlaws(State st) {
        List<Flaw> flaws = new LinkedList<>();
        for(FlawFinder fd : flawFinders)
            flaws.addAll(fd.getFlaws(st, this));

        // TODO: move the following to the new FlawFinder interface

        //find the resource flaws
        flaws.addAll(st.resourceFlaws());

        if (flaws.isEmpty()) {
            List<TemporalDatabase> dbs = st.getDatabases();
            for (int i = 0; i < dbs.size(); i++) {
                TemporalDatabase db1 = dbs.get(i);
                for (int j = i + 1; j < dbs.size(); j++) {
                    TemporalDatabase db2 = dbs.get(j);
                    if (isThreatening(st, db1, db2)) {
                        flaws.add(new Threat(db1, db2));
                    }
                }
            }
        }

        if (flaws.isEmpty()) {
            for (VarRef v : st.getUnboundVariables()) {
                assert !st.typeOf(v).equals("integer");
                flaws.add(new UnboundVariable(v));
            }
        }

        return flaws;
    }

    /**
     * Implementation of search. An easy thing to do to forward this call to the
     * depthBoundedAStar method.
     *
     * @param deadline Absolute time (in ms) at which the planner must stop.
     * @return A solution state if the planner found one. null otherwise.
     */
    public State search(final long deadline) {
        return search(deadline, Integer.MAX_VALUE, false);
    }

    /**
     *
     * @param deadline Absolute time (in ms) at which the planner must stop.
     * @param maxDepth Will discard any partial plan which depth is greater than that.
     *                 Note that the depth of a partial plan is computed with respect to
     *                 initial state (i.e. repairing a state starts with a depth > 0)
     * @param incrementalDeepening If set to true, the planner will increase the maximum
     *                             allowed depth from 1 until maxDepth until a plan is found
     *                             or the planner times out.
     * @return A solution plan if the planner founds one. null otherwise.
     *         Also check the "planState" field for more detailed information.
     */
    public State search(final long deadline, final int maxDepth, final boolean incrementalDeepening) {
        List<State> toRestore = new LinkedList<>(queue);

        int currentMaxDepth;
        if(incrementalDeepening)
            currentMaxDepth = 1;
        else
            currentMaxDepth = maxDepth;

        State solution = null;
        while(currentMaxDepth <= maxDepth && solution == null && planState != EPlanState.TIMEOUT) {
            queue.clear();
            queue.addAll(toRestore);
            solution = depthBoundedAStar(deadline, currentMaxDepth);
            if(currentMaxDepth == Integer.MAX_VALUE) // make sure we don't overflow
                break;
            currentMaxDepth += 1;
        }
        return solution;
    }

    /**
     * Provides a comparator that is used to sort flaws. THe first flaw will be
     * selected to be resolved.
     *
     * @param st State in which the flaws appear.
     * @return The comparator to use for ordering.
     */
    public final Comparator<Flaw> flawComparator(State st) {
        return FlawCompFactory.get(st, this, flawSelStrategies);
    }

    /**
     * The comparator used to order the queue. THe first state in the queue
     * (according to this comparator, will be selected for expansion.
     *
     * @return The comparator to use for ordering the queue.
     */
    public final Comparator<State> stateComparator() {
        return PlanCompFactory.get(planSelStrategies);
    }

    /**
     * Checks if two temporal databases are threatening each others. It is the
     * case if: - both are not consuming databases (start with an assignment).
     * Otherwise, threat is naturally handled by looking for supporters. - their
     * state variables are unifiable. - they can overlap
     *
     * @return True there is a threat.
     */
    protected boolean isThreatening(State st, TemporalDatabase db1, TemporalDatabase db2) {
        // if they are not both consumers, it is dealt by open goal reasoning
        if (db1.isConsumer() || db2.isConsumer())
            return false;

        // if their state variables are not unifiable
        if(!st.Unifiable(db1, db2))
            return false;

        // if db1 cannot start before db2 ends
        boolean db1AfterDB2 = true;
        for(TPRef start1 : db1.getFirstTimePoints()) {
            for (TPRef end2 : db2.getLastTimePoints()) {
                if (st.canBeBefore(start1, end2)) {
                    db1AfterDB2 = false;
                    break;
                }
            }
        }
        // if db2 cannot start before db1 ends
        boolean db2AfterDB1 = true;
        for(TPRef end1 : db1.getLastTimePoints())
            for(TPRef start2 : db2.getFirstTimePoints())
                if(st.canBeBefore(start2, end1)) {
                    db2AfterDB1 = false;
                    break;
                }

        // true if they can overlap
        return !(db1AfterDB2 || db2AfterDB1);

    }

    public static boolean optimal = false;

    protected State depthBoundedAStar(final long deadLine, final int maxDepth) {
        while (true) {
            if (System.currentTimeMillis() > deadLine) {
                TinyLogger.LogInfo("Timeout.");
                this.planState = EPlanState.TIMEOUT;
                return null;
            }
            if (queue.isEmpty()) {
                if (!APlanner.optimal) {
                    TinyLogger.LogInfo("No plan found.");
                    this.planState = EPlanState.INFEASIBLE;
                    return null;
                } else if(best.isConsistent() && GetFlaws(best).isEmpty()) {
                    this.planState = EPlanState.CONSISTENT;
                    return best;
                } else {
                    this.planState = EPlanState.INFEASIBLE;
                    return null;
                }
            }
            //get the best state and continue the search
            State st = queue.remove();

            if(!st.isConsistent())
                break;
            OpenedStates++;

            if (APlanner.debugging) {
                st.exportTemporalNetwork("current-stn.dot");
            }

            List<Flaw> flaws = GetFlaws(st);

            TinyLogger.LogInfo(st, "\nCurrent state: [%s]", st.mID);
            if (flaws.isEmpty()) {
                if(Planner.debugging)
                    st.assertConstraintNetworkGroundAndConsistent();
                if (!APlanner.optimal) {
                    this.planState = EPlanState.CONSISTENT;
                    TinyLogger.LogInfo("Plan found:");
                    TinyLogger.LogInfo(st);
                    return st;
                }else{
                    best = st;
                    //remove all suboptimal candidates from the queue
                    List<State> remove = new LinkedList<>();
                    for(State s:queue){
                        if(LMC.singleton.cost(s) <= LMC.singleton.cost(best)){
                            remove.add(s);
                        }
                    }
                    queue.removeAll(remove);
                    continue;
                }
            }

            if(st.depth == maxDepth) //we are not interested in its children
                continue;

            //do some sorting here - min domain
            //Collections.sort(opts, optionsComparatorMinDomain);
            Collections.sort(flaws, this.flawComparator(st));

            if (flaws.isEmpty()) {
                throw new FAPEException("Error: no flaws but state was not found to be a solution.");
            }

            //we just take the first flaw and its resolvers
            Flaw f = flaws.get(0);
            List<Resolver> resolvers = f.getResolvers(st, this);

            if (resolvers.isEmpty()) {
                // dead end, keep going
                TinyLogger.LogInfo(st, "  Dead-end, flaw without resolvers: %s", flaws.get(0));
                continue;
            }

            TinyLogger.LogInfo(st, " Flaw: %s", f);

            // Append the possibles fixed state to the queue
            for (Resolver res : resolvers) {
                TinyLogger.LogInfo(st, "   Res: %s", res);

                State next = st.cc();
                TinyLogger.LogInfo(st, "     [%s] Adding %s", next.mID, res);
                boolean success = applyResolver(next, res);

                if (success) {
                    queue.add(next);
                    GeneratedStates++;
                } else {
                    TinyLogger.LogInfo(st, "     Dead-end reached for state: %s", next.mID);
                    //inconsistent state, doing nothing
                }
            }

        }
        return null;
    }

    /**
     * Applies a resolver to the given state. This state will be modified to integrate the resolver.
     *
     * @param st State to modify
     * @param resolver Resolver to apply
     * @return True if the resolver was successfully applied and the resulting state is consistent.
     *         False otherwise.
     */
    public boolean applyResolver(State st, Resolver resolver) {
        return resolver.apply(st, this) &&
                st.isConsistent();
    }

    /**
     * Keeps the current best plan and tries to repair it until the absolute deadline.
     * @param deadline Absolute deadline in ms (to compare with currentTimeMillis())
     * @return True if a consistent state was found, false otherwise.
     */
    @Deprecated // we should build a planner with one state and search from that
    public boolean Repair(long deadline) {
        return Repair(deadline, Integer.MAX_VALUE, false);
    }

    /**
     * Cleans up the queue of everything except the best state and starts searching from this one.
     *
     * @param deadline Absolute deadline in ms (to compare with currentTimeMillis())
     * @param maxDepth Maximum depth of a solution any state beyond that will be discarded.
     *                 Note that the current best state might have a depth > 0.
     * @param incrementalDeepening Gradually increments the maximum depth until maxDepth is reached.
     * @return True if a solution is found, False otherwise.
     */
    @Deprecated  // we should build a planner with one state and search from that
    public boolean Repair(final long deadline, final int maxDepth, final boolean incrementalDeepening) {
        KeepBestStateOnly();

        do {
            planState = EPlanState.INCONSISTENT;

            best = search(deadline, maxDepth, incrementalDeepening);
            assert best == null || best.isConsistent() : "Search returned an inconsistent plan.";

            if (planState == EPlanState.TIMEOUT && best == null)
                return false;
            else if (planState == EPlanState.INFEASIBLE) {
                assert best == null;
                return false;
            } else if(planState == EPlanState.CONSISTENT) {
                assert best != null;
                // it seems consistent, check if it is dynamically controllable
                Plan plan = new Plan(best);
                if (!plan.isConsistent()) {
                    System.err.println("Returned state is not consistent or not DC. We keep searching.");
                    planState = EPlanState.INCONSISTENT;
                    search(deadline); //TODO: here incremental deepening is abandoned, this whole part should be in search
                } else {
                    planState = EPlanState.CONSISTENT;
                }
            }
        } while(planState != EPlanState.CONSISTENT);

        //we empty the queue now and leave only the best state there
        KeepBestStateOnly();
        return true;
    }

    /**
     * Progresses in the plan up for howFarToProgress.
     * Returns either AtomicActions that were instantiated with corresponding start times, or
     * null, if not solution was found in the given time
     */
    public List<AtomicAction> Progress(long currentTime) {
        this.SetEarliestExecution((int) currentTime);
        this.Repair(System.currentTimeMillis() + 500);
        State myState = best;
        if(best == null)
            return new LinkedList<>();

        Plan plan = new Plan(myState);

        if(Plan.showChart)
            ActionsChart.setCurrentTime((int) currentTime);


        List<AtomicAction> ret = new LinkedList<>();
        for (Action a : plan.getExecutableActions((int) currentTime)) {
            long startTime = myState.getEarliestStartTime(a.start());
            assert a.status() == ActionStatus.PENDING : "Action "+a+" is not pending but "+a.status();
            assert startTime >= currentTime : "Cannot start an action at a time "+startTime+" lower that "+
                    "current time: "+currentTime;
            long latestEnd = myState.getLatestStartTime(a.end());
            AtomicAction aa = new AtomicAction(a, startTime, plan.getMinDuration(a), plan.getMaxDuration(a), best);
            ret.add(aa);
        }

        // for all selecting actions, we set them as being executed and we bind their start time point
        // to the one we requested.

        for(AtomicAction aa : ret) {
            setActionExecuting(aa);
        }

        return ret;
    }

    public int numUnfinishedActions() {
        int cnt = 0;
        for (Action a : best.getAllActions()) {
            if (!a.decomposable() && (a.status() == ActionStatus.PENDING || a.status() == ActionStatus.EXECUTING)) {
                cnt++;
            }
        }
        return cnt;
    }

    public boolean hasPendingActions() {
        for (Action a : best.getAllActions()) {
            if (a.status() == ActionStatus.PENDING) {
                return true;
            }
        }
        return false;
    }

    /**
     * restarts the planning problem into its initial state
     *
    public boolean Replan(long deadline) {
        State st = new State(pb, controllability);

        for(ActionExecution ae : executedAction.values()) {
            Action a = ae.createNewGroundAction(pb);
            if(ae.status == ActionStatus.EXECUTING || ae.status == ActionStatus.EXECUTED) {
                st.insert(a);
                st.setActionExecuting(a, (int) ae.startTime);
            }
            if(ae.status == ActionStatus.EXECUTED) {
                st.setActionSuccess(a, (int) ae.endTime);
            }
        }
        st.enforceDelay(pb.start(), pb.earliestExecution(), currentTime);
        st.isConsistent();
        best = st;
        queue.clear();
        queue.add(st);
        return Repair(deadline);
    }
     */

    /**
     * Enforces given facts into the plan (possibly breaking it) this is an
     * incremental step, if there was something already defined, the name
     * collisions are considered to be intentional
     *
     * @param anml An ANML AST to be integrated in the planner.
     * @param propagate If true, a propagation will be done in the constraint networks.
     *                  This should be avoided if the domain is not completely described yet.
     * @return True if the planner is applicable to resulting anml problem.
     *
    public boolean ForceFact(ParseResult anml, boolean propagate) {
        //read everything that is contained in the ANML block
        if (logging) {
            TinyLogger.LogInfo("Forcing new fact into best state.");
        }

        KeepBestStateOnly();

        //TODO: apply ANML to more states and choose the best after the application
        pb.addAnml(anml);
        this.dtg = new LiftedDTG(this.pb);

        // apply revisions to best state and check if it is consistent
        State st = GetCurrentState();

        st.update();
        if (propagate && !st.isConsistent()) {
            this.planState = EPlanState.INFEASIBLE;
        }

        return true;
    }
    */
}
