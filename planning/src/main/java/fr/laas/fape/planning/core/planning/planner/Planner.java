package fr.laas.fape.planning.core.planning.planner;


import fr.laas.fape.anml.model.AnmlProblem;
import fr.laas.fape.exceptions.InconsistencyException;
import fr.laas.fape.gui.ChartWindow;
import fr.laas.fape.planning.core.planning.preprocessing.ActionSupporterFinder;
import fr.laas.fape.planning.core.planning.preprocessing.LiftedDTG;
import fr.laas.fape.planning.core.planning.preprocessing.Preprocessor;
import fr.laas.fape.planning.core.planning.search.Handler;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.Resolver;
import fr.laas.fape.planning.core.planning.search.strategies.flaws.FlawCompFactory;
import fr.laas.fape.planning.core.planning.search.strategies.plans.PlanCompFactory;
import fr.laas.fape.planning.core.planning.search.strategies.plans.SeqPlanComparator;
import fr.laas.fape.planning.core.planning.states.Printer;
import fr.laas.fape.planning.core.planning.states.SearchNode;
import fr.laas.fape.planning.core.planning.states.PartialPlan;
import fr.laas.fape.planning.exceptions.FlawOrderingAnomaly;
import fr.laas.fape.planning.exceptions.FlawWithNoResolver;
import fr.laas.fape.planning.exceptions.PlanningInterruptedException;
import fr.laas.fape.planning.exceptions.ResolverResultedInInconsistency;
import fr.laas.fape.planning.gui.SearchView;
import fr.laas.fape.constraints.stnu.Controllability;
import fr.laas.fape.planning.util.TinyLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base for any planner in FAPE.
 * It is responsible for detecting and solving flaws and search procedures.
 *
 * Classes that inherit from it only have to implement the abstract methods to
 * provide a search policy. Overriding methods can also be done to change the
 * default behaviour.
 */
public class Planner {

    public boolean stopPlanning = false;

    public Planner(PartialPlan initialPartialPlan, PlanningOptions options) {
        this.options = options;
        this.pb = initialPartialPlan.pb;
        this.controllability = initialPartialPlan.controllability;
        this.dtg = new LiftedDTG(this.pb);
        queue = new PriorityQueue<>(100, this.heuristicComputer().comparator(options));
        SearchNode root = new SearchNode(initialPartialPlan, this);

        root.addOperation(s -> {
            s.setPlanner(this);
            s.notify(Handler.StateLifeTime.PRE_QUEUE_INSERTION);
        });
        queue.add(root);

        if(options.displaySearch) {
            searchView = new SearchView(this);
            searchView.addNode(root);
        }
    }

    public final PlanningOptions options;
    public Preprocessor preprocessor;

    public static boolean debugging = false;

    public int numGeneratedPartialPlans = 1; // count the initial partial plan
    public int numExpandedPartialPlans = 0;
    public int numFastForwardedPartialPlans = 0;

    public final Controllability controllability;

    public final AnmlProblem pb;
    LiftedDTG dtg = null;

    SearchView searchView = null;
    public ActionSupporterFinder getActionSupporterFinder() {
        return dtg;
    }

    private final PriorityQueue<SearchNode> queue;

    /** References to all SearchNodes currently allocated by the planner. (the values of the map are not used).
     * THis is used to be able to release the memory retained by those nodes. */
    private final WeakHashMap<SearchNode, Object> allStates = new WeakHashMap<>();

    private final int _bestNodesToKeep = GlobalOptions.getIntOption("best-nodes-to-keep");
    private final int _recentNodesToKeep = GlobalOptions.getIntOption("recent-nodes-to-keep");

    public void recordSearchNode(SearchNode n) {
        allStates.put(n, null);
    }

    /** Release memory from most SearchNodes.
     *  This is done by transforming a Soft or Strong reference in nodes to weak references. */
    private void cleanNodes(int mostRecentExpandedToKeep, int mostRecentPendingToKeep, int bestToKeep, Comparator<? super SearchNode> heuristic) {
        Set<SearchNode> recentsExpanded =
                allStates.keySet().stream()
                        .filter(s -> s.status == SearchNode.Status.EXPANDED)
                        .filter(SearchNode::holdsMemory)
                        .sorted(Comparator.comparingLong(s -> s.lastRecord))
                        .limit(mostRecentExpandedToKeep)
                        .collect(Collectors.toSet());

        Set<SearchNode> recentsPending =
                allStates.keySet().stream()
                        .filter(s -> s.status == SearchNode.Status.PENDING)
                        .filter(SearchNode::holdsMemory)
                        .sorted(Comparator.comparingLong(s -> s.lastRecord))
                        .limit(mostRecentPendingToKeep)
                        .collect(Collectors.toSet());

        Set<SearchNode> bests =
                allStates.keySet().stream()
                        .filter(s -> s.status == SearchNode.Status.PENDING)
                        .filter(SearchNode::holdsMemory)
                        .sorted(heuristic)
                        .limit(bestToKeep)
                        .collect(Collectors.toSet());

        allStates.keySet().stream()
                .filter(s -> s.status != SearchNode.Status.STABLE)
                .filter(SearchNode::holdsMemory)
                .filter(s -> !recentsExpanded.contains(s))
                .filter(s -> !recentsPending.contains(s))
                .filter(s -> !bests.contains(s))
                .forEach(SearchNode::releaseMemory);
    }

    /**
     * All possible states of the planner.
     */
    public enum EPlanState {
        TIMEOUT, CONSISTENT, INCONSISTENT, INFEASIBLE
    }

    /**
     * what is the current state of the plan
     */
    public EPlanState planState = EPlanState.INCONSISTENT;

    public List<Handler> getHandlers() { return options.handlers; }

    public Optional<Flaw> getFlaws(SearchNode plan) {
        return plan.getState().getFlaws(options.flawFinders, flawComparator(plan.getState()));
    }

    /**
     * Implementation of search. An easy thing to do to forward this call to the
     * depthBoundedAStar method.
     *
     * @param deadline Absolute time (in ms) at which the planner must stop.
     * @return A solution state if the planner found one. null otherwise.
     */
    public PartialPlan search(final long deadline) {
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
    public PartialPlan search(final long deadline, final int maxDepth, final boolean incrementalDeepening) {
        if (options.useAEpsilon) {
            return aEpsilonSearch(deadline, maxDepth, incrementalDeepening);
        } else {
            return bestFirstSearch(deadline, maxDepth, incrementalDeepening);
        }
    }

    public PartialPlan bestFirstSearch(final long deadline, final int maxDepth, final boolean incrementalDeepening){

        List<SearchNode> toRestore = new LinkedList<>(queue);

        int currentMaxDepth;
        if(incrementalDeepening)
            currentMaxDepth = 1;
        else
            currentMaxDepth = maxDepth;

        PartialPlan solution = null;
        while(currentMaxDepth <= maxDepth && solution == null && planState != EPlanState.TIMEOUT)
        {
            queue.clear();
            queue.addAll(toRestore);
            solution = depthBoundedAStar(deadline, currentMaxDepth);

            if (currentMaxDepth == Integer.MAX_VALUE) // make sure we don't overflow
                break;
            currentMaxDepth += 1;
            if (debugging && incrementalDeepening)
                System.out.println("Current max depth: "+currentMaxDepth+". Expanded nodes: "+ numExpandedPartialPlans);
        }
        return solution;
    }

    /**
     * Provides a comparator that is used to sort flaws. THe first flaw will be
     * selected to be resolved.
     *
     * @param plan PartialPlan in which the flaws appear.
     * @return The comparator to use for ordering.
     */
    private Comparator<Flaw> flawComparator(PartialPlan plan) {
        return FlawCompFactory.get(plan, this, options.flawSelStrategies);
    }

    private SeqPlanComparator heuristic = null;

    /**
     * The comparator used to order the queue. THe first plan in the queue
     * (according to this comparator, will be selected for expansion.
     *
     * @return The comparator to use for ordering the queue.
     */
    public final SeqPlanComparator heuristicComputer() {
        if(heuristic == null) {
            heuristic = PlanCompFactory.get(this, options.planSelStrategies);
        }
        return heuristic;
    }

    /**
     * Checks if two temporal databases are threatening each others. It is the
     * case if: - both are not consuming databases (start with an assignment).
     * Otherwise, threat is naturally handled by looking for supporters. - their
     * state variables are unifiable. - they can overlap
     *
     * @return True there is a threat.
     */
    private PartialPlan depthBoundedAStar(final long deadLine, final int maxDepth) {
        while (true) {
            if(stopPlanning)
                throw new PlanningInterruptedException();

            if (System.currentTimeMillis() > deadLine) {
                TinyLogger.LogInfo("Timeout.");
                this.planState = EPlanState.TIMEOUT;
                return null;
            }
            if (queue.isEmpty()) {
                this.planState = EPlanState.INFEASIBLE;
                return null;
            }

            //get the best plan and continue the search
            SearchNode node = queue.remove();
            PartialPlan plan = node.getState();
            cleanNodes(_recentNodesToKeep, 0, _bestNodesToKeep, queue.comparator());

            try {
                // let all handlers know that this plan was selected for expansion
                for (Handler h : options.handlers)
                    h.addOperation(node, Handler.StateLifeTime.SELECTION, this);

                if (!plan.isConsistent()) {
                    if (options.displaySearch)
                        searchView.setDeadEnd(node);
                    continue;
                }

                Optional<Flaw> flaw = getFlaws(node);
                if (!flaw.isPresent()) {
                    // this is a solution plan
                    if (options.displaySearch)
                        searchView.setSolution(node);

                    this.planState = EPlanState.CONSISTENT;
                    TinyLogger.LogInfo("Plan found:");
                    TinyLogger.LogInfo(plan);
                    return plan;
                } else if (node.getDepth() < maxDepth) {
                    List<SearchNode> children = expand(node, flaw.get());
                    queue.addAll(children);
                }
            } catch (InconsistencyException e) {
                if(options.displaySearch) {
                    searchView.setDeadEnd(node);
                }
            }
        }
    }

    /**
     * Expand a partial plan by selecting a flaw and generating resolvers for this flaw.
     * @param plan    Partial plan to expand
     * @return      All consistent children as a result of the expansion.
     */
    private List<SearchNode> expand(SearchNode plan, Flaw f) {
        try {
            if (options.displaySearch)
                searchView.setCurrentFocus(plan);

            if(TinyLogger.logging) {
                TinyLogger.LogInfo(Printer.timelines(plan.getState()));
            }

            assert plan.getState().isConsistent() : "Expand was given an inconsistent state.";

            if (plan.getDepth() == 0 && plan.getState().addableActions != null) {
                assert plan.getState().getAllActions().isEmpty();
                preprocessor.restrictPossibleActions(plan.getState().addableActions);
            }

            numExpandedPartialPlans++;

            TinyLogger.LogInfo(plan.getState(), "\nCurrent plan: [%s]", plan.getID());

            List<Resolver> resolvers = f.getResolvers(plan.getState(), this);
            // make sure resolvers are always in the same order (for reproducibility)
            Collections.sort(resolvers);


            if (options.displaySearch)
                searchView.setProperty(plan, SearchView.SELECTED_FLAW, Printer.p(plan.getState(), f));

            if (resolvers.isEmpty()) {
                // dead end, keep going
                TinyLogger.LogInfo(plan.getState(), "  Dead-end, flaw without resolvers: %s", f);
                if (options.displaySearch) {
                    searchView.setProperty(plan, SearchView.COMMENT, "  Dead-end, flaw without resolvers: " + f);
                    searchView.setDeadEnd(plan);
                }
                plan.setExpanded();
                return Collections.emptyList();
            }

            TinyLogger.LogInfo(plan.getState(), " Flaw: %s", f);

            List<SearchNode> children = new LinkedList<>();

            // compute all valid children
            for (int resolverID = 0; resolverID < resolvers.size(); resolverID++) {
                SearchNode next = new SearchNode(plan, this);
                final int currentResolver = resolverID;
                try {
                    next.addOperation(s -> {
                        Optional<Flaw> fs = s.getFlaws(options.flawFinders, flawComparator(s));

                        assert fs.isPresent();
                        Flaw selectedFlaw = fs.get();
                        List<Resolver> possibleResolvers = selectedFlaw.getResolvers(s, this);
                        Collections.sort(possibleResolvers);
                        Resolver res;
                        try {
                            res = possibleResolvers.get(currentResolver);
                        } catch (IndexOutOfBoundsException e) {
                            // apparently we tried to access a resolver that did not exists, either a
                            // resolver disapeared or the flaw is not the one we expected
                            throw new FlawOrderingAnomaly(null, 0, currentResolver);
                        }
                        if (!applyResolver(s, res, false))
                            s.setDeadEnd();
                        else {
                            s.checkConsistency();
                            if (s.isConsistent() && options.useFastForward)
                                fastForward(s, 10);
                        }
                        s.checkConsistency();
                        s.notify(Handler.StateLifeTime.PRE_QUEUE_INSERTION);
                    });

                    boolean success = next.getState().isConsistent();
                    String hrComment = "";

                    if (!success)
                        hrComment = "Non consistent resolver application or error while fast-forwarding.";

                    if (success) {
                        assert next != null;
                        children.add(next);
                        numGeneratedPartialPlans++;
                    } else {
                        TinyLogger.LogInfo(plan.getState(), "     Dead-end reached for plan: %s", next.getID());
                        //inconsistent plan, doing nothing
                    }
                    if (options.displaySearch) {
                        searchView.addNode(next);
                        if (!success)
                            searchView.setDeadEnd(next);
                        searchView.setProperty(next, SearchView.LAST_APPLIED_RESOLVER, Printer.p(plan.getState(), resolvers.get(currentResolver)));
                        searchView.setProperty(next, SearchView.COMMENT, hrComment);
                    }
                } catch (InconsistencyException e) {
                    if(options.displaySearch) {
                        searchView.addNode(next);
                        searchView.setDeadEnd(next);
                        searchView.setProperty(next, SearchView.LAST_APPLIED_RESOLVER, Printer.p(plan.getState(), resolvers.get(currentResolver)));
                        searchView.setProperty(next, SearchView.COMMENT, e.toString());
                    }
                }
            }
            plan.setExpanded();
            return children;
        } catch (InconsistencyException e) {
            if(options.displaySearch) {
                searchView.setDeadEnd(plan);
                searchView.setProperty(plan, SearchView.COMMENT, e.toString());
            }
            return Collections.emptyList();
        }
    }

    /**
     * This function looks at flaws and resolvers in the plan and fixes a flaw with a single resolver.
     * It does that at most "maxForwardState"
     */
    private boolean fastForward(PartialPlan plan, int maxForwardStates) {
        while(maxForwardStates > 0) {
            maxForwardStates--;

            Optional<Flaw> flaws = plan.getFlaws(options.flawFinders, flawComparator(plan));

            if (!flaws.isPresent()) {
                return true;
            }

            //we just take the first flaw and its resolvers
            Flaw flaw = flaws.get();
            List<Resolver> resolvers = flaw.getResolvers(plan, this);

            if (resolvers.isEmpty()) {
                throw new FlawWithNoResolver(flaw);
            }

            if (resolvers.size() == 1) {
                Resolver res = resolvers.get(0);
                if (!applyResolver(plan, res, true))
                    throw new ResolverResultedInInconsistency(flaw, res);
                else
                    plan.checkConsistency();
                TinyLogger.LogInfo(plan, "     [%s] ff: Adding %s", plan.mID, res);
                if (plan.isConsistent()) {
                    numFastForwardedPartialPlans++;
                    // proceed to next loop
                } else {
                    throw new ResolverResultedInInconsistency(flaw, res);
                }
            } else {
                // nothing was done
                return true;
            }
        }
        return true;
    }

    /**
     * Applies a resolver to the given plan. This plan will be modified to integrate the resolver.
     *
     * @param plan PartialPlan to modify
     * @param resolver Resolver to apply
     * @return True if the resolver was successfully applied and the resulting plan is consistent.
     *         False otherwise.
     */
    private boolean applyResolver(PartialPlan plan, Resolver resolver, boolean isFastForwarding) {
        boolean result = plan.apply(resolver.asStateModification(plan), isFastForwarding) && plan.csp.propagateMixedConstraints() && plan.checkConsistency();
        return result;
    }

    private PartialPlan aEpsilonSearch(final long deadLine, final int maxDepth, final boolean incrementalDeepening) {
        assert !incrementalDeepening : "Incremental Deepening is not supported in A-Epsilon search.";

        // stores the admissible children of the last expanded node.
        PriorityQueue<SearchNode> AX = new PriorityQueue<SearchNode>(10, heuristicComputer().comparator(options));


        if (queue.isEmpty()) {
            this.planState = EPlanState.INFEASIBLE;
            TinyLogger.LogInfo("Initially empty queue.");
            return null;
        }
        try {
            double fThreshold = (1f + options.epsilon) * f(queue.peek());

            int numStatesExploredInDepth = 0;
            int numStatesToExploreInBest = 0;
            while (true) {
                if(stopPlanning)
                    throw new PlanningInterruptedException();

                if (System.currentTimeMillis() > deadLine) {
                    TinyLogger.LogInfo("Timeout.");
                    this.planState = EPlanState.TIMEOUT;
                    return null;
                }

                if (queue.isEmpty()) {
                    this.planState = EPlanState.INFEASIBLE;
                    TinyLogger.LogInfo("Empty queue.");
                    return null;
                }
                cleanNodes(_recentNodesToKeep, AX.size(), _bestNodesToKeep, queue.comparator());

                SearchNode current;
                if (AX.isEmpty() || numStatesToExploreInBest > 0) {
                    if (numStatesExploredInDepth > 0) {
                        numStatesToExploreInBest = Math.round(numStatesExploredInDepth / options.depthShallowRatio);
                        // we should keep expanding least-cost nodes for
                        numStatesExploredInDepth = 0;
                    }
                    // get best in open
                    current = queue.poll();
                    numStatesToExploreInBest--;
                } else {
                    numStatesExploredInDepth++;
                    // still interesting plans (below threshold) in the successors of the previously expanded plan
                    current = AX.poll();
                    queue.remove(current);
                }

                try {
                    // let all handlers know that this plan was selected for expansion
                    for (Handler h : options.handlers)
                        h.addOperation(current, Handler.StateLifeTime.SELECTION, this);

                    if (!current.getState().isConsistent())
                        throw new InconsistencyException();

                    if (current.getState().isSolution(options.flawFinders)) {
                        // this is a solution plan
                        if (options.displaySearch)
                            searchView.setSolution(current);

                        this.planState = EPlanState.CONSISTENT;
                        TinyLogger.LogInfo("Plan found:");
                        TinyLogger.LogInfo(current.getState());
                        return current.getState();
                    } else if (current.getDepth() < maxDepth) {
                        Optional<Flaw> flaw = getFlaws(current);
                        assert flaw.isPresent();
                        // expand the plan
                        List<SearchNode> children = expand(current, flaw.get());
                        AX.clear();
                        for (SearchNode child : children) {
                            assert child != null;
                            queue.add(child);
                            // add admissible children to AX for next iteration
                            if (f(child) < fThreshold) {
                                AX.add(child);
                            }
                        }
                    }
                    // update the threshold, since our heuristic is not admissible, we do not take
                    // the max of fthreshold and (1+e)*f(best in open)
                    if (!queue.isEmpty())
                        fThreshold = (1f + options.epsilon) * f(queue.peek());
                } catch (InconsistencyException e) {
                    if (options.displaySearch)
                        searchView.setDeadEnd(current);
                    TinyLogger.LogInfo("\nCurrent plan: [" + current.getID() + "]");
                    TinyLogger.LogInfo("  Non consistent");
                }
            }
        } catch (InconsistencyException e) {
            // should only occur in the first state of the queue
            assert queue.size() == 1 : "An inconsistency exception leaked from search";
            this.planState = EPlanState.INFEASIBLE;
            TinyLogger.LogInfo("Problem not solvable from initial plan");
            return null;
        }
    }

    public double h(SearchNode plan){ return options.heuristicWeight * heuristic.h(plan); }
    public double g(SearchNode plan){ return heuristic.g(plan); }
    public double f(SearchNode plan) { return g(plan) + h(plan); }

    private ChartWindow chartWindow = null;
    public void drawState(PartialPlan plan) {
        if(chartWindow == null)
            chartWindow = new ChartWindow("Actions");

        chartWindow.draw(plan.getCanvasOfActions());
    }
}
