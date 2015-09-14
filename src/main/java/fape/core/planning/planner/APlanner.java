package fape.core.planning.planner;

import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.core.planning.heuristics.Preprocessor;
import fape.core.planning.heuristics.reachability.ReachabilityGraphs;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.LiftedDTG;
import fape.core.planning.search.flaws.finders.FlawFinder;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnsupportedTaskCond;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.search.strategies.flaws.FlawCompFactory;
import fape.core.planning.search.strategies.plans.PlanCompFactory;
import fape.core.planning.search.strategies.plans.SeqPlanComparator;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.drawing.gui.ChartWindow;
import fape.gui.SearchView;
import fape.util.TinyLogger;
import fape.util.Utils;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.Task;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.constraints.stnu.Controllability;

import java.util.*;

/**
 * Base for any planner in FAPE.
 * It is responsible for detecting and solving flaws and search procedures.
 *
 * Classes that inherit from it only have to implement the abstract methods to
 * provide a search policy. Overriding methods can also be done to change the
 * default behaviour.
 */
public abstract class APlanner {

    public APlanner(State initialState, PlanningOptions options) {
        this.options = options;
        this.pb = initialState.pb;
        this.controllability = initialState.controllability;
        this.dtg = new LiftedDTG(this.pb);
        queue = new PriorityQueue<>(100, this.stateComparator());
        queue.add(initialState);
        preprocessor = new Preprocessor(this, initialState);
        if(options.usePlanningGraphReachability) {
            initialState.pgr = preprocessor.getFeasibilityReasoner();
        }

        if(options.displaySearch) {
            searchView = new SearchView(this);
            searchView.addNode(initialState, null);
        }
    }

    @Deprecated // we should always build from a state (maybe add a constructor from a problem)
    public APlanner(Controllability controllability, PlanningOptions options) {
        this.options = options;
        this.controllability = controllability;
        this.pb = new AnmlProblem(useActionConditions());
        this.dtg = new LiftedDTG(this.pb);
        this.queue = new PriorityQueue<>(100, this.stateComparator());
        queue.add(new State(pb, controllability));

        this.preprocessor = new Preprocessor(this, queue.peek());
    }

    public final PlanningOptions options;
    public final Preprocessor preprocessor;

    @Deprecated //might not work in a general scheme were multiple planner instances are instantiated
    public static APlanner currentPlanner = null;

    public static boolean debugging = false;
    public static boolean logging = false;
    public final static boolean actionResolvers = true; // do we add actions to resolve flaws?

    public int GeneratedStates = 1; //count the initial state
    public int expandedStates = 0;
    public int numFastForwarded = 0;

    public final Controllability controllability;

    public final AnmlProblem pb;
    LiftedDTG dtg = null;

    SearchView searchView = null;

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
     * @param st        State in which the causal link was added.
     * @param supporter Left side of the causal link.
     * @param consumer  Right side of the causal link.
     */
    public void causalLinkAdded(State st, LogStatement supporter, LogStatement consumer) {
    }

    /**
     * All possible state of the planner.
     */
    public enum EPlanState {
        TIMEOUT, CONSISTENT, INCONSISTENT, INFEASIBLE
    }

    /**
     * what is the current state of the plan
     */
    public EPlanState planState = EPlanState.INCONSISTENT;

    /**
     * Finds all flaws of a given state. Currently, threats and unbound
     * variables are considered only if no other flaws are present.
     *
     * @return A list of flaws present in the system. The list of flaws might not
     * be exhaustive.
     */
    public List<Flaw> getFlaws(State st) {
        List<Flaw> flaws = new LinkedList<>();

        if(!useActionConditions() && !st.getOpenTasks().isEmpty()) {
            // we are not using action condition (htn planner),
            // hence every opened task must be solved with an action insertion which is done first
            for(Task ac : st.getOpenTasks()) {
                flaws.add(new UnsupportedTaskCond(ac));
            }
        } else {
            for (FlawFinder fd : options.flawFinders)
                flaws.addAll(fd.getFlaws(st, this));

            //find the resource flaws
            flaws.addAll(st.resourceFlaws());
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
     * @param deadline             Absolute time (in ms) at which the planner must stop.
     * @param maxDepth             Will discard any partial plan which depth is greater than that.
     *                             Note that the depth of a partial plan is computed with respect to
     *                             initial state (i.e. repairing a state starts with a depth > 0)
     * @param incrementalDeepening If set to true, the planner will increase the maximum
     *                             allowed depth from 1 until maxDepth until a plan is found
     *                             or the planner times out.
     * @return A solution plan if the planner founds one. null otherwise.
     * Also check the "planState" field for more detailed information.
     */
    public State search(final long deadline, final int maxDepth, final boolean incrementalDeepening) {
        if (options.useAEpsilon) {
            return aEpsilonSearch(deadline, maxDepth, incrementalDeepening);
        } else {
            return bestFirstSearch(deadline, maxDepth, incrementalDeepening);
        }
    }

    public State bestFirstSearch(final long deadline, final int maxDepth, final boolean incrementalDeepening){

        List<State> toRestore = new LinkedList<>(queue);

        int currentMaxDepth;
        if(incrementalDeepening)
            currentMaxDepth = 1;
        else
            currentMaxDepth = maxDepth;

        State solution = null;
        while(currentMaxDepth <= maxDepth && solution == null && planState != EPlanState.TIMEOUT)
        {
            queue.clear();
            queue.addAll(toRestore);
            solution = depthBoundedAStar(deadline, currentMaxDepth);

            if (solution != null) {
                // here we check that the plan is indeed a solution
                // it might not be the case if are looking for DC plans (Plan.makeDispatchable)
                // and the STNU does not check dynamic controllability
                Plan plan = new Plan(solution);
                if (!plan.isConsistent()) {
                    System.err.println("Returned state is not consistent or not DC. We keep searching.");
                    planState = EPlanState.INCONSISTENT;
                    solution = null;
                }
            }

            if (currentMaxDepth == Integer.MAX_VALUE) // make sure we don't overflow
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
        return FlawCompFactory.get(st, this, options.flawSelStrategies);
    }

    SeqPlanComparator stateComparator = null;

    /**
     * The comparator used to order the queue. THe first state in the queue
     * (according to this comparator, will be selected for expansion.
     *
     * @return The comparator to use for ordering the queue.
     */
    public final SeqPlanComparator stateComparator() {
        if(stateComparator == null)
            stateComparator = PlanCompFactory.get(this, options.planSelStrategies);
        return stateComparator;
    }

    /**
     * Checks if two temporal databases are threatening each others. It is the
     * case if: - both are not consuming databases (start with an assignment).
     * Otherwise, threat is naturally handled by looking for supporters. - their
     * state variables are unifiable. - they can overlap
     *
     * @return True there is a threat.
     */

    protected State depthBoundedAStar(final long deadLine, final int maxDepth) {
        while (true) {
            if (System.currentTimeMillis() > deadLine) {
                TinyLogger.LogInfo("Timeout.");
                this.planState = EPlanState.TIMEOUT;
                return null;
            }
            if (queue.isEmpty()) {
                this.planState = EPlanState.INFEASIBLE;
                return null;
            }

            //get the best state and continue the search
            State st = queue.remove();

            if(!st.isConsistent()) {
                if(options.displaySearch)
                    searchView.setDeadEnd(st);
                continue;
            }

            List<Flaw> flaws = getFlaws(st);
            if (flaws.isEmpty()) {
                // this is a solution state
                if(Planner.debugging)
                    st.assertConstraintNetworkGroundAndConsistent();
                if(options.displaySearch)
                    searchView.setSolution(st);

                this.planState = EPlanState.CONSISTENT;
                TinyLogger.LogInfo("Plan found:");
                TinyLogger.LogInfo(st);
                return st;
            } else if(st.depth < maxDepth) {
                List<State> children = expand(st, flaws);
                for(State child : children) {
                    queue.add(child);
                }
            }
        }
    }

    /**
     * Expand a partial plan by selecting a flaw and generating resolvers for this flaw.
     * @param st    Partial plan to expand
     * @param flaws List of flaws in the partial plan. Those are asked to avoid duplication of work.
     * @return      All consistent children as a result of the expansion.
     */
    public List<State> expand(State st, List<Flaw> flaws) {
        for(Flaw f : flaws)
            f.getNumResolvers(st, this);

        List<State> children = new LinkedList<>();

        if(options.displaySearch)
            searchView.setCurrentFocus(st);

        assert st.isConsistent() : "Expand was given an inconsistent state.";

        expandedStates++;

        if(options.usePlanningGraphReachability) {
            if (!preprocessor.getFeasibilityReasoner().checkFeasibility(st)) {
                TinyLogger.LogInfo(st, "\nDead End State: [%s]", st.mID);
                if (options.displaySearch)
                    searchView.setDeadEnd(st);
                return children;
            }
        }

        assert !flaws.isEmpty() : "Cannot expand a flaw free state. It is already a solution.";

        TinyLogger.LogInfo(st, "\nCurrent state: [%s]", st.mID);

        // sort the flaws, higher priority come first
        try {
            Collections.sort(flaws, this.flawComparator(st));
        } catch (java.lang.IllegalArgumentException e) {
            // problem with the sort function, try to find an problematic example and exit
            System.err.println("The flaw comparison function is not legal (for instance, it might not be transitive).");
            Utils.showExampleProblemWithFlawComparator(flaws, this.flawComparator(st), st, this);
            System.exit(1);
        }

        //we just take the first flaw and its resolvers
        Flaw f;
        if(options.chooseFlawManually) {
            System.out.print("STATE :" + st.mID + "\n");
            for(int i=0 ; i<flaws.size() ; i++)
                System.out.println("["+i+"] "+Printer.p(st, flaws.get(i)));
            int choosen = Utils.readInt();
            f = flaws.get(choosen);
        } else {
            f = flaws.get(0);
        }
        List<Resolver> resolvers = f.getResolvers(st, this);
        // put resolvers are always in the same order (for reproducibility)
        Collections.sort(resolvers);

        if(options.displaySearch)
            searchView.setProperty(st, SearchView.SELECTED_FLAW, Printer.p(st, f));

        if (resolvers.isEmpty()) {
            // dead end, keep going
            TinyLogger.LogInfo(st, "  Dead-end, flaw without resolvers: %s", flaws.get(0));
            if(options.displaySearch)
                searchView.setDeadEnd(st);
            return children;
        }

        TinyLogger.LogInfo(st, " Flaw: %s", f);

        // Append the possibles fixed state to the queue
        for (Resolver res : resolvers) {
            TinyLogger.LogInfo(st, "   Res: %s", res);

            State next = st.cc();
            TinyLogger.LogInfo(st, "     [%s] Adding %s", next.mID, res);
            boolean success = applyResolver(next, res);

            if (success) {
                if(options.useFastForward) {
                    if(fastForward(next, 10)) {
                        children.add(next);
                        GeneratedStates++;
                    } else {
                        TinyLogger.LogInfo(st, "     ff: Dead-end reached for state: %s", next.mID);
                    }
                } else {
                    children.add(next);
                    GeneratedStates++;
                }
                if(options.displaySearch) {
                    searchView.addNode(next, st);
                    searchView.setProperty(next, SearchView.LAST_APPLIED_RESOLVER, Printer.p(st, res));
                }
            } else {
                TinyLogger.LogInfo(st, "     Dead-end reached for state: %s", next.mID);
                //inconsistent state, doing nothing
            }
        }
        return children;
    }

    /**
     * This function looks at flaws and resolvers in the state and fixes flaws with a single resolver.
     * It does that at most "maxForwardState"
     */
    public boolean fastForward(State st, int maxForwardStates) {
        List<Flaw> flaws = getFlaws(st);

        if(maxForwardStates == 0)
            return true;

        if (flaws.isEmpty()) {
            return true;
        }

        //do some sorting here - min domain
        //Collections.sort(opts, optionsComparatorMinDomain);
        Collections.sort(flaws, this.flawComparator(st));

        //we just take the first flaw and its resolvers
        Flaw f = flaws.get(0);
        List<Resolver> resolvers = f.getResolvers(st, this);
        if (resolvers.isEmpty()) {
            // dead end, keep going
            TinyLogger.LogInfo(st, "  Dead-end, flaw without resolvers: %s", flaws.get(0));
            return false;
        }

        if(resolvers.size() == 1) {
            Resolver res = resolvers.get(0);
            TinyLogger.LogInfo(st, "     [%s] ff: Adding %s", st.mID, res);
            if(applyResolver(st, res)) {
                numFastForwarded++;
                return fastForward(st, maxForwardStates-1);
            } else {
                return false;
            }
        } else {
            // nothing was done
            return true;
        }
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
        return resolver.apply(st, this) && st.csp.propagateMixedConstraints() && st.isConsistent();
    }

    /**
     * Main parameter of the greediness of A-Epsilon. TODO: this should be a command line parameter.
     */
    protected final float epsilon = 0.3f;

    protected State aEpsilonSearch(final long deadLine, final int maxDepth, final boolean incrementalDeepening) {
        assert !incrementalDeepening : "Incremental Deepening is not supported in A-Epsilon search.";

        // stores the admissible children of the last expanded node.
        PriorityQueue<State> AX = new PriorityQueue<State>(10, stateComparator());


        if (queue.isEmpty()) {
            this.planState = EPlanState.INFEASIBLE;
            return null;
        }
        float fThreshold = (1f + epsilon) * f(queue.peek());

        while (true) {
            if (System.currentTimeMillis() > deadLine) {
                TinyLogger.LogInfo("Timeout.");
                this.planState = EPlanState.TIMEOUT;
                return null;
            }

            if (queue.isEmpty()) {
                this.planState = EPlanState.INFEASIBLE;
                return null;
            }

            State current;
            if(AX.isEmpty()) {
                // get best in open
                current = queue.poll();
            } else {
                // still interesting states (below threshold) in the successors of the previously expanded state
                current = AX.poll();
                queue.remove(current);
            }

            if(!current.isConsistent()) {
                if(options.displaySearch)
                    searchView.setDeadEnd(current);
                continue;
            }

            List<Flaw> flaws = getFlaws(current);
            if (flaws.isEmpty()) {
                // this is a solution state
                if(Planner.debugging)
                    current.assertConstraintNetworkGroundAndConsistent();
                if(options.displaySearch)
                    searchView.setSolution(current);

                this.planState = EPlanState.CONSISTENT;
                TinyLogger.LogInfo("Plan found:");
                TinyLogger.LogInfo(current);
                return current;
            } else if(current.depth < maxDepth) {
                // expand the state
                List<State> children = expand(current, flaws);
                AX.clear();
                for(State child : children) {
                    queue.add(child);
                    // add admissible children to AX for next iteration
                    if(f(child) < fThreshold) {
                        AX.add(child);
                    }
                }
            }
            // update the threshold, since our heuristic is not admissible, we do not take
            // the max of fthreshold and (1+e)*f(best in open)
            if(!queue.isEmpty())
                fThreshold = (1f + epsilon) * f(queue.peek());
        }
    }

    public float h(State st){
        return stateComparator().h(st);
    }

    public float g(State st){
        return stateComparator().g(st);
    }
    public float f(State st) { return g(st) + h(st); }
    public boolean definesHeuristicsValues() { return stateComparator().definesHeuristicsValues(); }

    private ChartWindow chartWindow = null;
    public void drawState(State st) {
        if(chartWindow == null)
            chartWindow = new ChartWindow("Actions");

        chartWindow.draw(st.getCanvasOfActions());
    }
}
