package fape.core.planning.planner;

import fape.core.planning.heuristics.reachability.ReachabilityGraphs;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.LiftedDTG;
import fape.core.planning.preprocessing.Preprocessor;
import fape.core.planning.search.Handler;
import fape.core.planning.search.flaws.finders.FlawFinder;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.search.strategies.flaws.FlawCompFactory;
import fape.core.planning.search.strategies.plans.PlanCompFactory;
import fape.core.planning.search.strategies.plans.SeqPlanComparator;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.states.StateWrapper;
import fape.drawing.gui.ChartWindow;
import fape.gui.SearchView;
import fape.util.TinyLogger;
import fape.util.Utils;
import planstack.anml.model.AnmlProblem;
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
        initialState.setPlanner(this);
        this.controllability = initialState.controllability;
        this.dtg = new LiftedDTG(this.pb);
        queue = new PriorityQueue<>(100, this.stateComparator());
        queue.add(new StateWrapper(initialState));

        if(options.usePlanningGraphReachability) {
            initialState.pgr = preprocessor.getFeasibilityReasoner();
            initialState.reachabilityGraphs = new ReachabilityGraphs(this, initialState);
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
        this.pb = new AnmlProblem();
        this.dtg = new LiftedDTG(this.pb);
        this.queue = new PriorityQueue<>(100, this.stateComparator());

        State initState = new State(pb, controllability);
        queue.add(new StateWrapper(initState));
        initState.setPlanner(this);

        if(options.usePlanningGraphReachability)
            initState.reachabilityGraphs = new ReachabilityGraphs(this, initState);
    }

    public final PlanningOptions options;
    public Preprocessor preprocessor;

    public static boolean debugging = false;
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

    protected final PriorityQueue<StateWrapper> queue;

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

    public abstract boolean isTopDownOnly();

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

    public List<Handler> getHandlers() { return options.handlers; }

    public List<Flaw> getFlaws(StateWrapper st) {
        return st.getState().getFlaws(options.flawFinders, flawComparator(st.getState()));
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

        List<StateWrapper> toRestore = new LinkedList<>(queue);

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
            StateWrapper st = queue.remove();

            // let all handlers know that this state was selected for expansion
            for(Handler h : options.handlers)
                h.apply(st, Handler.StateLifeTime.SELECTION, this);

            if(!st.getState().isConsistent()) {
                if(options.displaySearch)
                    searchView.setDeadEnd(st.getState());
                continue;
            }

            List<Flaw> flaws = getFlaws(st);
            if (flaws.isEmpty()) {
                // this is a solution state
                if(options.displaySearch)
                    searchView.setSolution(st.getState());

                this.planState = EPlanState.CONSISTENT;
                TinyLogger.LogInfo("Plan found:");
                TinyLogger.LogInfo(st.getState());
                return st.getState();
            } else if(st.getState().depth < maxDepth) {
                List<StateWrapper> children = expand(st, flaws);
                for(StateWrapper child : children) {
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
    public List<StateWrapper> expand(StateWrapper st, List<Flaw> flaws) {
        for(Flaw f : flaws)
            f.getNumResolvers(st.getState(), this);

        if(options.displaySearch)
            searchView.setCurrentFocus(st.getState());

        assert st.getState().isConsistent() : "Expand was given an inconsistent state.";

        expandedStates++;

        assert !flaws.isEmpty() : "Cannot expand a flaw free state. It is already a solution.";

        TinyLogger.LogInfo(st.getState(), "\nCurrent state: [%s]", st.getID());

        // sort the flaws, higher priority come first
        try {
            Collections.sort(flaws, this.flawComparator(st.getState()));
        } catch (java.lang.IllegalArgumentException e) {
            // problem with the sort function, try to find an problematic example and exit
            System.err.println("The flaw comparison function is not legal (for instance, it might not be transitive).");
            Utils.showExampleProblemWithFlawComparator(flaws, this.flawComparator(st.getState()), st.getState(), this);
            System.exit(1);
        }

        // just take the first flaw and its resolvers (unless the flaw is chosen on command line)
        Flaw f;
        if(options.chooseFlawManually) {
            System.out.print("STATE :" + st.getID() + "\n");
            for(int i=0 ; i<flaws.size() ; i++)
                System.out.println("["+i+"] "+Printer.p(st.getState(), flaws.get(i)));
            int choosen = Utils.readInt();
            f = flaws.get(choosen);
        } else {
            f = flaws.get(0);
        }
        List<Resolver> resolvers = f.getResolvers(st.getState(), this);
        // make sure resolvers are always in the same order (for reproducibility)
        Collections.sort(resolvers);

        if(options.displaySearch)
            searchView.setProperty(st.getState(), SearchView.SELECTED_FLAW, Printer.p(st.getState(), f));

        if (resolvers.isEmpty()) {
            // dead end, keep going
            TinyLogger.LogInfo(st.getState(), "  Dead-end, flaw without resolvers: %s", flaws.get(0));
            if(options.displaySearch) {
                searchView.setProperty(st.getState(), SearchView.COMMENT, "Pruned by old reachability");
                searchView.setDeadEnd(st.getState());
            }
            return Collections.emptyList();
        }

        TinyLogger.LogInfo(st.getState(), " Flaw: %s", f);

        List<StateWrapper> children = new LinkedList<>();

        // compute all valid children
//        for (Resolver res : resolvers) {
        for(int resolverID=0 ; resolverID < resolvers.size() ; resolverID++) {
            StateWrapper next = new StateWrapper(st);
            final int currentResolver = resolverID;
            next.addOperation(s -> {
                Resolver res = getFlaws(st).get(0)
                        .getResolvers(st.getState(), this)
                        .get(currentResolver);
                if(!applyResolver(s, res))
                    s.setDeadEnd();
            });
//            TinyLogger.LogInfo(st.getState(), "   Res: %s", res);

//            TinyLogger.LogInfo(st.getState(), "     [%s] Adding %s", next.mID, res);
            boolean success = next.getState().isConsistent();
            String hrComment = "";

            if(!success)
                hrComment = "Non consistent resolver application.";

            // if we use fast forward, solve any flaw with at most one resolver
            if(success && options.useFastForward) {
                success &= fastForward(next, 10);
                if(!success) {
                    hrComment = "Inconsistency or flaw with no resolvers when fast forwarding.";
                }
            }

            // build reachability graphs
            if(success && options.usePlanningGraphReachability) {
                next.addOperation(s -> {
                    s.reachabilityGraphs = new ReachabilityGraphs(this, s);
                    if(!s.reachabilityGraphs.isRefinableToSolution())
                        s.setDeadEnd();
                });
                success &= next.getState().isConsistent();
                if(!success)
                    hrComment = "Pruned by reachability.";
            }

            if(options.displaySearch) {
                searchView.addNode(next.getState(), st.getState());
                searchView.setProperty(next.getState(), SearchView.LAST_APPLIED_RESOLVER, Printer.p(st.getState(), resolvers.get(currentResolver)));
                searchView.setProperty(next.getState(), SearchView.COMMENT, hrComment);
            }
            if (success) {
                children.add(next);
                GeneratedStates++;
            } else {
                TinyLogger.LogInfo(st.getState(), "     Dead-end reached for state: %s", next.getID());
                if (options.displaySearch)
                    searchView.setDeadEnd(next.getState());
                //inconsistent state, doing nothing
            }
        }
        return children;
    }

    /**
     * This function looks at flaws and resolvers in the state and fixes flaws with a single resolver.
     * It does that at most "maxForwardState"
     */
    public boolean fastForward(StateWrapper st, int maxForwardStates) {
        List<Flaw> flaws = getFlaws(st);

        if(maxForwardStates == 0)
            return true;

        if (flaws.isEmpty()) {
            return true;
        }

        //we just take the first flaw and its resolvers
        Flaw f = flaws.get(0);
        List<Resolver> resolvers = f.getResolvers(st.getState(), this);
        if (resolvers.isEmpty()) {
            // dead end, keep going
            TinyLogger.LogInfo(st.getState(), "  Dead-end, flaw without resolvers: %s", flaws.get(0));
            return false;
        }

        if(resolvers.size() == 1) {
            st.addOperation(s -> {
                Resolver res = s.getFlaws(options.flawFinders, flawComparator(s)).get(0).getResolvers(s, this).get(0);
                if(!res.apply(s, this))
                    s.setDeadEnd();
            });
            TinyLogger.LogInfo(st.getState(), "     [%s] ff: Adding %s", st.getID(), resolvers.get(0));
            if(st.getState().isConsistent()) {
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
        boolean result = resolver.apply(st, this) && st.csp.propagateMixedConstraints() && st.isConsistent();
        st.flaws = null; // clear flaws cache
        return result;
    }

    protected State aEpsilonSearch(final long deadLine, final int maxDepth, final boolean incrementalDeepening) {
        assert !incrementalDeepening : "Incremental Deepening is not supported in A-Epsilon search.";

        // stores the admissible children of the last expanded node.
        PriorityQueue<StateWrapper> AX = new PriorityQueue<StateWrapper>(10, stateComparator());


        if (queue.isEmpty()) {
            this.planState = EPlanState.INFEASIBLE;
            return null;
        }
        float fThreshold = (1f + options.epsilon) * f(queue.peek());

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

            StateWrapper current;
            if(AX.isEmpty()) {
                // get best in open
                current = queue.poll();
            } else {
                // still interesting states (below threshold) in the successors of the previously expanded state
                current = AX.poll();
                queue.remove(current);
            }

            // let all handlers know that this state was selected for expansion
            for(Handler h : options.handlers)
                h.apply(current, Handler.StateLifeTime.SELECTION, this);

            if(!current.getState().isConsistent()) {
                if(options.displaySearch)
                    searchView.setDeadEnd(current.getState());
                continue;
            }

            List<Flaw> flaws = getFlaws(current);
            if (flaws.isEmpty()) {
                // this is a solution state
                if(options.displaySearch)
                    searchView.setSolution(current.getState());

                this.planState = EPlanState.CONSISTENT;
                TinyLogger.LogInfo("Plan found:");
                TinyLogger.LogInfo(current.getState());
                return current.getState();
            } else if(current.getState().depth < maxDepth) {
                // expand the state
                List<StateWrapper> children = expand(current, flaws);
                AX.clear();
                for(StateWrapper child : children) {
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
                fThreshold = (1f + options.epsilon) * f(queue.peek());
        }
    }

    public float h(StateWrapper st){ return stateComparator().h(st); }
    public float g(StateWrapper st){ return stateComparator().g(st); }
    public float f(StateWrapper st) { return g(st) + h(st); }

    private ChartWindow chartWindow = null;
    public void drawState(State st) {
        if(chartWindow == null)
            chartWindow = new ChartWindow("Actions");

        chartWindow.draw(st.getCanvasOfActions());
    }
}
