package fr.laas.fape.planning.core.planning.planner;


import fr.laas.fape.anml.model.AnmlProblem;
import fr.laas.fape.exceptions.InconsistencyException;
import fr.laas.fape.gui.ChartWindow;
import fr.laas.fape.planning.core.planning.preprocessing.ActionSupporterFinder;
import fr.laas.fape.planning.core.planning.preprocessing.LiftedDTG;
import fr.laas.fape.planning.core.planning.preprocessing.Preprocessor;
import fr.laas.fape.planning.core.planning.search.Handler;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaws;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.Resolver;
import fr.laas.fape.planning.core.planning.search.strategies.flaws.FlawCompFactory;
import fr.laas.fape.planning.core.planning.search.strategies.plans.PlanCompFactory;
import fr.laas.fape.planning.core.planning.search.strategies.plans.SeqPlanComparator;
import fr.laas.fape.planning.core.planning.states.Printer;
import fr.laas.fape.planning.core.planning.states.SearchNode;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.exceptions.FlawOrderingAnomaly;
import fr.laas.fape.planning.exceptions.FlawWithNoResolver;
import fr.laas.fape.planning.exceptions.ResolverResultedInInconsistency;
import fr.laas.fape.planning.gui.SearchView;
import fr.laas.fape.constraints.stnu.Controllability;
import fr.laas.fape.planning.util.TinyLogger;
import fr.laas.fape.planning.util.Utils;

import java.util.*;

/**
 * Base for any planner in FAPE.
 * It is responsible for detecting and solving flaws and search procedures.
 *
 * Classes that inherit from it only have to implement the abstract methods to
 * provide a search policy. Overriding methods can also be done to change the
 * default behaviour.
 */
public class Planner {

    public Planner(State initialState, PlanningOptions options) {
        this.options = options;
        this.pb = initialState.pb;
        this.controllability = initialState.controllability;
        this.dtg = new LiftedDTG(this.pb);
        queue = new PriorityQueue<>(100, this.heuristicComputer().comparator(options));
        SearchNode root = new SearchNode(initialState);

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

    public int numGeneratedStates = 1; //count the initial state
    public int numExpandedStates = 0;
    public int numFastForwardedStates = 0;

    public final Controllability controllability;

    public final AnmlProblem pb;
    LiftedDTG dtg = null;

    SearchView searchView = null;
    public ActionSupporterFinder getActionSupporterFinder() {
        return dtg;
    }

    private final PriorityQueue<SearchNode> queue;

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

    public List<Flaw> getFlaws(SearchNode st) {
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

        List<SearchNode> toRestore = new LinkedList<>(queue);

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
            if (debugging && incrementalDeepening)
                System.out.println("Current max depth: "+currentMaxDepth+". Expanded nodes: "+ numExpandedStates);
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
    private Comparator<Flaw> flawComparator(State st) {
        return FlawCompFactory.get(st, this, options.flawSelStrategies);
    }

    private SeqPlanComparator heuristic = null;

    /**
     * The comparator used to order the queue. THe first state in the queue
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
    private State depthBoundedAStar(final long deadLine, final int maxDepth) {
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
            SearchNode st = queue.remove();

            try {
                // let all handlers know that this state was selected for expansion
                for (Handler h : options.handlers)
                    h.addOperation(st, Handler.StateLifeTime.SELECTION, this);

                if (!st.getState().isConsistent()) {
                    if (options.displaySearch)
                        searchView.setDeadEnd(st);
                    continue;
                }

                List<Flaw> flaws = getFlaws(st);
                if (flaws.isEmpty()) {
                    // this is a solution state
                    if (options.displaySearch)
                        searchView.setSolution(st);

                    this.planState = EPlanState.CONSISTENT;
                    TinyLogger.LogInfo("Plan found:");
                    TinyLogger.LogInfo(st.getState());
                    return st.getState();
                } else if (st.getDepth() < maxDepth) {
                    List<SearchNode> children = expand(st);
                    for (SearchNode child : children) {
                        queue.add(child);
                    }
                }
            } catch (InconsistencyException e) {
                if(options.displaySearch) {
                    searchView.setDeadEnd(st);
                }
            }
        }
    }

    /**
     * Expand a partial plan by selecting a flaw and generating resolvers for this flaw.
     * @param st    Partial plan to expand
     * @return      All consistent children as a result of the expansion.
     */
    private List<SearchNode> expand(SearchNode st) {
        try {
            if (options.displaySearch)
                searchView.setCurrentFocus(st);

            if(TinyLogger.logging) {
                TinyLogger.LogInfo(Printer.timelines(st.getState()));
            }

            assert st.getState().isConsistent() : "Expand was given an inconsistent state.";

            if (st.getDepth() == 0 && st.getState().addableActions != null) {
                assert st.getState().getAllActions().isEmpty();
                preprocessor.restrictPossibleActions(st.getState().addableActions);
            }

            numExpandedStates++;

            TinyLogger.LogInfo(st.getState(), "\nCurrent state: [%s]", st.getID());

            List<Flaw> flaws = getFlaws(st);
            assert !flaws.isEmpty() : "Cannot expand a flaw free state. It is already a solution.";

            // just take the first flaw and its resolvers (unless the flaw is chosen on command line)
            Flaw f;
            if (options.chooseFlawManually) {
                System.out.print("STATE :" + st.getID() + "\n");
                System.out.println(Printer.timelines(st.getState()));
                for (int i = 0; i < flaws.size(); i++)
                    System.out.println("[" + i + "] " + Printer.p(st.getState(), flaws.get(i)));
                int choosen = Utils.readInt();
                f = flaws.get(choosen);
            } else {
                f = flaws.get(0);
            }
            List<Resolver> resolvers = f.getResolvers(st.getState(), this);
            // make sure resolvers are always in the same order (for reproducibility)
            Collections.sort(resolvers);


            if (options.displaySearch)
                searchView.setProperty(st, SearchView.SELECTED_FLAW, Printer.p(st.getState(), f));

            if (resolvers.isEmpty()) {
                // dead end, keep going
                TinyLogger.LogInfo(st.getState(), "  Dead-end, flaw without resolvers: %s", flaws.get(0));
                if (options.displaySearch) {
                    searchView.setProperty(st, SearchView.COMMENT, "  Dead-end, flaw without resolvers: " + flaws.get(0));
                    searchView.setDeadEnd(st);
                }
                st.setExpanded();
                return Collections.emptyList();
            }

            TinyLogger.LogInfo(st.getState(), " Flaw: %s", f);

            List<SearchNode> children = new LinkedList<>();

            final Object flawsHash = Flaws.hash(flaws);

            // compute all valid children
            for (int resolverID = 0; resolverID < resolvers.size(); resolverID++) {
                SearchNode next = new SearchNode(st);
                final int currentResolver = resolverID;
                try {
                    next.addOperation(s -> {
                        List<Flaw> fs = s.getFlaws(options.flawFinders, flawComparator(s));
//                        assert Flaws.hash(fs).equals(flawsHash) : "There is a problem with the generated flaws.";

                        Flaw selectedFlaw = fs.get(0);
                        List<Resolver> possibleResolvers = selectedFlaw.getResolvers(s, this);
                        Collections.sort(possibleResolvers);
                        Resolver res;
                        try {
                            res = possibleResolvers.get(currentResolver);
                        } catch (IndexOutOfBoundsException e) {
                            // apparently we tried to access a resolver that did not exists, either a
                            // resolver disapeared or the flaw is not the one we expected
                            throw new FlawOrderingAnomaly(flaws, 0, currentResolver);
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
                        children.add(next);
                        numGeneratedStates++;
                    } else {
                        TinyLogger.LogInfo(st.getState(), "     Dead-end reached for state: %s", next.getID());
                        //inconsistent state, doing nothing
                    }
                    if (options.displaySearch) {
                        searchView.addNode(next);
                        if (!success)
                            searchView.setDeadEnd(next);
                        searchView.setProperty(next, SearchView.LAST_APPLIED_RESOLVER, Printer.p(st.getState(), resolvers.get(currentResolver)));
                        searchView.setProperty(next, SearchView.COMMENT, hrComment);
                    }
                } catch (InconsistencyException e) {
                    if(options.displaySearch) {
                        searchView.addNode(next);
                        searchView.setDeadEnd(next);
                        searchView.setProperty(next, SearchView.LAST_APPLIED_RESOLVER, Printer.p(st.getState(), resolvers.get(currentResolver)));
                        searchView.setProperty(next, SearchView.COMMENT, e.toString());
                    }
                }
            }
            st.setExpanded();
            return children;
        } catch (InconsistencyException e) {
            if(options.displaySearch) {
                searchView.setDeadEnd(st);
                searchView.setProperty(st, SearchView.COMMENT, e.toString());
            }
            return Collections.emptyList();
        }
    }

    /**
     * This function looks at flaws and resolvers in the state and fixes flaws with a single resolver.
     * It does that at most "maxForwardState"
     */
    private boolean fastForward(State st, int maxForwardStates) {
        if(maxForwardStates == 0)
            return true;

        List<Flaw> flaws = st.getFlaws(options.flawFinders, flawComparator(st));

        if (flaws.isEmpty()) {
            return true;
        }

        //we just take the first flaw and its resolvers
        Flaw flaw = flaws.get(0);
        List<Resolver> resolvers = flaw.getResolvers(st, this);

        if (resolvers.isEmpty()) {
            throw new FlawWithNoResolver(flaw);
        }

        if(resolvers.size() == 1) {
            Resolver res = resolvers.get(0);
            if(!applyResolver(st, res, true))
                throw new ResolverResultedInInconsistency(flaw, res);
            else
                st.checkConsistency();
            TinyLogger.LogInfo(st, "     [%s] ff: Adding %s", st.mID, res);
            if(st.isConsistent()) {
                numFastForwardedStates++;
                return fastForward(st, maxForwardStates-1);
            } else {
                throw new ResolverResultedInInconsistency(flaw, res);
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
    private boolean applyResolver(State st, Resolver resolver, boolean isFastForwarding) {
        boolean result = st.apply(resolver.asStateModification(st), isFastForwarding) && st.csp.propagateMixedConstraints() && st.checkConsistency();
        return result;
    }

    private State aEpsilonSearch(final long deadLine, final int maxDepth, final boolean incrementalDeepening) {
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
                    // still interesting states (below threshold) in the successors of the previously expanded state
                    current = AX.poll();
                    queue.remove(current);
                }

                try {
                    // let all handlers know that this state was selected for expansion
                    for (Handler h : options.handlers)
                        h.addOperation(current, Handler.StateLifeTime.SELECTION, this);

                    if (!current.getState().isConsistent())
                        throw new InconsistencyException();

                    if (current.getState().isSolution(options.flawFinders)) {
                        // this is a solution state
                        if (options.displaySearch)
                            searchView.setSolution(current);

                        this.planState = EPlanState.CONSISTENT;
                        TinyLogger.LogInfo("Plan found:");
                        TinyLogger.LogInfo(current.getState());
                        return current.getState();
                    } else if (current.getDepth() < maxDepth) {
                        // expand the state
                        List<SearchNode> children = expand(current);
                        AX.clear();
                        for (SearchNode child : children) {
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
                    TinyLogger.LogInfo("\nCurrent state: [" + current.getID() + "]");
                    TinyLogger.LogInfo("  Non consistent");
                }
            }
        } catch (InconsistencyException e) {
            // should only occur in the first state of the queue
            assert queue.size() == 1 : "An inconsistency exception leaked from search";
            this.planState = EPlanState.INFEASIBLE;
            TinyLogger.LogInfo("Problem not solvable from initial state");
            return null;
        }
    }

    public double h(SearchNode st){ return options.heuristicWeight * heuristic.h(st); }
    public double g(SearchNode st){ return heuristic.g(st); }
    public double f(SearchNode st) { return g(st) + h(st); }

    private ChartWindow chartWindow = null;
    public void drawState(State st) {
        if(chartWindow == null)
            chartWindow = new ChartWindow("Actions");

        chartWindow.draw(st.getCanvasOfActions());
    }
}
