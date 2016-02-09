package fape.core.planning.planninggraph;

import fape.core.planning.grounding.*;
import fape.core.planning.heuristics.relaxed.ActionUsageTracker;
import fape.core.planning.heuristics.relaxed.DTGCollection;
import fape.core.planning.heuristics.relaxed.DTGImpl;
import fape.core.planning.heuristics.relaxed.OpenGoalTransitionFinder;
import fape.core.planning.planner.APlanner;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.NoSolutionException;
import planstack.anml.model.concrete.Action;

import java.util.*;

public class RelaxedPlanExtractor {

    final APlanner planner;
    final State st;
    Set<GAction> inPlanActions;

    private boolean displayResolution() { return false; }

    Collection<GAction> alreadyUsed = new HashSet<>();

    Set<GTask> derivPending = new HashSet<>();
    Set<GTask> decompPending = new HashSet<>();
    Set<Fluent> causalPending = new HashSet<>();

    public RelaxedPlanExtractor(APlanner planner, State st) {
        this.planner = planner;
        this.st = st;
        this.dtgs = new DTGCollection(planner, st, displayResolution());
        inPlanActions = new HashSet<>();
        for(Action a : st.getAllActions()) {
            for(GAction ga : st.getGroundActions(a))
                inPlanActions.add(ga);
        }
    }

    private int costOfFluent(Fluent f) {
        if(achievedFluents.contains(f))
            // already achieved
            return 0;
        else if(causalPending.contains(f))
            // we already need to achieve it
            return 0;
        else
            return st.reachabilityGraphs.causalLevelOfFluent(f);
    }

    private int costOfPreconditions(GAction ga) {
        assert ga != null;
        if(alreadyUsed.contains(ga))
            return 0;
        int maxPreconditionCost = 0;
        for(Fluent f : ga.pre) {
            int cost = costOfFluent(f);
            if (cost > maxPreconditionCost)
                maxPreconditionCost = cost;
        }
        assert maxPreconditionCost >= 0;
        return maxPreconditionCost;
    }

    private int[] numOfActionsInstantiationsProducingFluent = null;

    private void setActionInstantiation(Action lifted, GAction ground) {
        counter.addActionInstantiation(lifted, ground);
        if(!instantiated.contains(lifted)) {
            // check if we need to update the possible addition count
            if(numOfActionsInstantiationsProducingFluent != null) {
                for(GAction ga : actionInstantiations.get(lifted)) {
                    for(int fluent : ga.additions)
                        numOfActionsInstantiationsProducingFluent[fluent]--;
                }
            }

            assert actionInstantiations.get(lifted).contains(ground) : "First instantiation is not in the domain.";
            actionInstantiations.get(lifted).clear();
            actionInstantiations.get(lifted).add(ground);
            instantiated.add(lifted);
        } else {
            assert actionInstantiations.get(lifted).size() == 1;
        }
    }

    private boolean achievableByLiftedAction(int fluent) {
        if(numOfActionsInstantiationsProducingFluent == null) {
            numOfActionsInstantiationsProducingFluent = new int[planner.preprocessor.getApproximateNumFluents()];
            for(Action a : actionInstantiations.keySet()) {
                if(instantiated.contains(a))
                    continue;
                for(GAction ga : actionInstantiations.get(a)) {
                    for(int f : ga.additions)
                        numOfActionsInstantiationsProducingFluent[f]++;
                }
            }
        }

        return numOfActionsInstantiationsProducingFluent[fluent] > 0;
    }

    public GAction selectMostInterestingAction(Collection<GAction> actions) throws NoSolutionException {
        GAction bestAction = null;
        int bestActionCost = Integer.MAX_VALUE;
        for(GAction ga : actions) {
            if(ga == null)
                return null; // init fact, we can not do better
            if(st.reachabilityGraphs.causalLevelOf(ga) < 0)
                continue; // action is not feasible

            if(alreadyUsed.contains(ga)) {
                return ga; // no additional cost as the action is already part of the relaxed plan
            }
            int maxCostOfAchieved = 0;
            int sumPreconditionsCosts = 0;

            for(Fluent f : ga.pre) {
                assert st.reachabilityGraphs.causalLevelOfFluent(f) >= 0;
                int base = achievableByLiftedAction(f.getID()) ? 0 : costOfFluent(f);
                sumPreconditionsCosts += base;
            }

            for(Fluent f : ga.add) {
                if(causalPending.contains(f) && !achievedFluents.contains(f)) {
                    int c = st.reachabilityGraphs.causalLevelOfFluent(f);
                    if(c >= 0) // only account for possible ones TODO: should never be not achievable?
                        maxCostOfAchieved = maxCostOfAchieved > c ? maxCostOfAchieved : c;
                }
            }
            int cost = sumPreconditionsCosts - maxCostOfAchieved;
            if(cost < bestActionCost) {
                bestActionCost = cost;
                bestAction = ga;
            }
        }

        if(bestAction != null) {
            assert st.reachabilityGraphs.causalLevelOf(bestAction) >= 0;
            return bestAction;

        } else {
            throw new NoSolutionException("No best action in "+actions);
        }
    }

    Map<Action, Set<GAction>> actionInstantiations;

    private Map<Action, Set<GAction>> getInitialPossibleActionsInstantiations() {
        Map<Action, Set<GAction>> instantiations = new HashMap<>();

        for (Action a : st.getAllActions()) {
            instantiations.put(a, st.getGroundActions(a));
        }
        return instantiations;
    }

    final DTGCollection dtgs;

    /** maps a timeline to its DTG */
    Map<Timeline, Integer> dtgsOfTimelines = new HashMap<>();

    Map<GStateVariable, Integer> dtgsOfStateVariables= new HashMap<>();

    public OpenGoalTransitionFinder.Path getPathToPersistence(Timeline og) throws NoSolutionException {
        assert og.hasSinglePersistence();
        OpenGoalTransitionFinder pathFinder = new OpenGoalTransitionFinder(planner, dtgs, displayResolution());
        Collection<Fluent> ogs = DisjunctiveFluent.fluentsOf(og.stateVariable, og.getGlobalConsumeValue(), st, planner);
        Set<GStateVariable> possibleStateVariables = new HashSet<>();
        for (Fluent f : ogs)
            possibleStateVariables.add(f.sv);

        for (GStateVariable sv : possibleStateVariables) {
            if(!dtgsOfStateVariables.containsKey(sv)) {
                final DTGImpl dtg = planner.preprocessor.getDTG(sv);
                final int dtgID = dtgs.add(dtg);
                dtgsOfStateVariables.put(sv, dtgID);
            }
            pathFinder.setDTGUsable(dtgsOfStateVariables.get(sv));
        }

        Collection<Timeline> potentialIndirectSupporters = new LinkedList<>();

        for (Timeline tl : st.getTimelines()) {
            if (tl == og || tl.hasSinglePersistence() || !st.unifiable(tl, og)
                    || !st.canAllBeBefore(tl.getFirstChange().end(), og.getFirstTimePoints()))
                continue;

            potentialIndirectSupporters.add(tl);
        }

        // filter to keep only those that can be directly before
        List<Timeline> toRemove = new LinkedList<>();
        for (Timeline tl1 : potentialIndirectSupporters) {
            for (Timeline tl2 : potentialIndirectSupporters) {
                if (tl1 == tl2)
                    continue;
                if (st.unified(tl1, tl2)) {
                    // if the last time points of tl2 cannot be before the first of tl1, then tl1 can't be after tl2.
//                        if(!st.canAllBeBefore(tl2.getLastTimePoints(), tl1.getFirstChangeTimePoint()))
                    if (!st.canBeBefore(tl2, tl1))
                        // in addition og can't be before tl2
                        if (!st.canBeBefore(og, tl2)) {
                            // the order is necessarily tl1 -> tl2 -> og hence tl1 can be removed from the set of potential supporters
                            toRemove.add(tl1);
                            if (toRemove.contains(tl2)) {
                                if (!st.canBeBefore(tl1, tl2) && !st.canBeBefore(tl2, tl1) && st.unified(tl1, tl2))
                                    throw new NoSolutionException("Unsolvable threat: "+tl1+"   "+tl2); // unsolvable threat
                            }
                        }
                }
            }
        }
        potentialIndirectSupporters.removeAll(toRemove);

        for (Timeline tl : potentialIndirectSupporters) {
            pathFinder.setDTGUsable(dtgsOfTimelines.get(tl));
        }
        assert og.chain.length == 1;
        pathFinder.addPersistenceTargets(ogs, og.getFirst().getConsumeTimePoint(), og.getFirst().getSupportTimePoint());

        return pathFinder.bestPath(actionCostEvaluator);
    }

    public OpenGoalTransitionFinder.Path getPathToTransition(Timeline og) throws NoSolutionException {
        assert !og.hasSinglePersistence();
        OpenGoalTransitionFinder pathFinder = new OpenGoalTransitionFinder(planner, dtgs, displayResolution());
        Collection<Fluent> ogs = DisjunctiveFluent.fluentsOf(og.stateVariable, og.getGlobalConsumeValue(), st, planner);
        Set<GStateVariable> possibleStateVariables = new HashSet<>();
        for(Fluent f : ogs)
            possibleStateVariables.add(f.sv);

        for(GStateVariable sv : possibleStateVariables) {
            if(!dtgsOfStateVariables.containsKey(sv)) {
                final DTGImpl dtg = planner.preprocessor.getDTG(sv);
                final int dtgID = dtgs.add(dtg);
                dtgsOfStateVariables.put(sv, dtgID);
            }
            pathFinder.setDTGUsable(dtgsOfStateVariables.get(sv));
        }

        Collection<Timeline> potentialIndirectSupporters = new LinkedList<>();

        for(Timeline tl : st.getTimelines()) {
            if(tl == og || tl.hasSinglePersistence() || !st.unifiable(tl, og) || !st.canBeBefore(tl, og))
                continue;

            potentialIndirectSupporters.add(tl);
        }

        // filter to keep only those that can be directly before
        List<Timeline> toRemove = new LinkedList<>();
        for(Timeline tl1 : potentialIndirectSupporters) {
            for(Timeline tl2 : potentialIndirectSupporters) {
                if(tl1 == tl2)
                    continue;
                if(st.unified(tl1, tl2) && !st.canBeBefore(tl2, tl1) && !st.canBeBefore(og, tl2)) {
                    // the order is necessarily tl1 -> tl2 -> og. Hence tl1 can be removed from the set of potential supporters
                    toRemove.add(tl1);
                    if(toRemove.contains(tl2)) {
                        if(!st.canBeBefore(tl1, tl2) && !st.canBeBefore(tl2, tl1) && st.unified(tl1,tl2))
                            throw new NoSolutionException("Unsolvable threat: "+tl1+"  "+tl2); // unsolvable threat
                    }
                }
            }
        }
        potentialIndirectSupporters.removeAll(toRemove);

        for(Timeline tl : potentialIndirectSupporters)
            pathFinder.setDTGUsable(dtgsOfTimelines.get(tl));

        pathFinder.addTransitionTargets(ogs, og.getFirstChangeTimePoint());

        return pathFinder.bestPath(actionCostEvaluator);
    }

    OpenGoalTransitionFinder.CostEvaluator actionCostEvaluator = new OpenGoalTransitionFinder.CostEvaluator() {
        public int cost(int liftedActionID, int gActionID, int fluentID) {
            return cost(st.getAction(liftedActionID), planner.preprocessor.getGroundAction(gActionID), planner.preprocessor.getFluent(fluentID).sv);
        }

        @Override
        public int distTo(int gActionID) {
            return distTo(planner.preprocessor.getGroundAction(gActionID));
        }

        @Override
        public boolean addable(int gActionID) {
            return st.addableActions.contains(gActionID);
        }

        @Override
        public boolean possibleInPlan(int gActionID) {
            return st.reachabilityGraphs.causalLevelOf(planner.preprocessor.getGroundAction(gActionID)) >= 0;
        }

        public int cost(Action a, GAction ga, GStateVariable sv) {
            return costOfAction(a, ga, sv);
        }

        public int distTo(GAction ga) {
            return costOfPreconditions(ga);
        }
    };

    HashSet<Fluent> achievedFluents = new HashSet<>();

    private void setActionUsed(GAction ga) {
        assert st.reachabilityGraphs.causalLevelOf(ga) >= 0 : "Trying to use an action that is not feasible.";
        if(!alreadyUsed.contains(ga) && displayResolution())
            System.out.println("  act: "+ga+" (causal level: "+st.reachabilityGraphs.causalLevelOf(ga)+")");

        alreadyUsed.add(ga);
        for(Fluent f : ga.add) {
            achievedFluents.add(f);
        }
        for(Fluent f : ga.pre) {
            if(!causalPending.contains(f) && !achievedFluents.contains(f)) {
                causalPending.add(f);
                assert st.reachabilityGraphs.causalLevelOfFluent(f) >= 0;
                if (displayResolution())
                    System.out.println("    pending: " + f);
            }
        }
    }

    private void initActionUsage() {
        alreadyUsed = new HashSet<>();
        achievedFluents = new HashSet<>();
    }

    public List<OpenGoalTransitionFinder.Path> getPaths() throws NoSolutionException {
        List<OpenGoalTransitionFinder.Path> allPaths = new ArrayList<>();

        for(Timeline tl : st.getTimelines()) {
            if(!tl.hasSinglePersistence()) {
                final DTGImpl tlDtg = DTGImpl.buildFromTimeline(tl, planner, st);
                final int tlDtgID = dtgs.add(tlDtg);
                assert !dtgsOfTimelines.containsKey(tl);
                dtgsOfTimelines.put(tl, tlDtgID);
            }
        }

        List<Timeline> opengoals = new ArrayList<>(st.tdb.getConsumers());
        opengoals.sort(Comparator.comparing((Timeline tl) -> st.getEarliestStartTime(tl.getFirst().getFirst().start()))
                                 .thenComparing(tl -> tl.mID));

        for(Timeline og : opengoals) {
            if(displayResolution())
                System.out.println("og: "+Printer.inlineTemporalDatabase(st, og));

            OpenGoalTransitionFinder.Path path;
            if(og.hasSinglePersistence()) {
                path = getPathToPersistence(og);
                assert path.start.dtgID == path.dtgOfStart || (!dtgs.get(path.start.dtgID).isSink || dtgs.get(path.dtgOfStart).isSink);
                dtgs.addPathEnd(path.dtgOfStart, dtgs.fluentOf(path.start));
            } else {
                path = getPathToTransition(og);
            }
            allPaths.add(path);

            final GStateVariable sv = dtgs.stateVariable(path);
            for(OpenGoalTransitionFinder.DualEdge edge : path.edges) {
                final Action lifted = dtgs.liftedAction(edge);
                final GAction ground = dtgs.groundAction(edge);

                if(ground == null) // not corresponding to any ground action
                    continue;

                setActionUsed(ground);
                if(lifted == null) { // not an instantiation
                    counter.addActionOccurrence(ground, sv);
                } else { // instantiation of the lifted action
                    setActionInstantiation(lifted, ground);
                }
            }
        }

        return allPaths;
    }

    Set<Action> instantiated = new HashSet<>();
    ActionUsageTracker counter = null;

    public int myPerfectHeuristic() {
        planner.preprocessor.getFeasibilityReasoner().getAddableActions(st, planner.preprocessor.getAllActions());


        counter = new ActionUsageTracker();
        try {
            if(displayResolution()) {
                System.out.println("\n\n ---- state: "+st.mID+"   -----");
                System.out.println("-> lifted in plan: "+st.getAllActions());
                System.out.println("-> addable actions: "+st.addableActions);
            }
            initActionUsage();
            actionInstantiations = getInitialPossibleActionsInstantiations();
            List<OpenGoalTransitionFinder.Path> paths = getPaths();

            Set<GAction> allGroundActions = new HashSet<>(counter.getAllUsedAction());

            for(Action a : counter.instantiations.keySet())
                assert instantiated.contains(a) && actionInstantiations.get(a).size() == 1;

            for(GAction ga : alreadyUsed) {
                causalPending.addAll(ga.pre);
            }

            if(displayResolution()) {
                System.out.println("\n---- Instantiation -----");
            }

            for(GAction ga : alreadyUsed) assert st.reachabilityGraphs.causalLevelOf(ga) >= 0;

            for(Map.Entry<Action, Set<GAction>> actInst : actionInstantiations.entrySet()) {
                if(!instantiated.contains(actInst.getKey())) {
                    final GAction ga = selectMostInterestingAction(actInst.getValue());
                    setActionInstantiation(actInst.getKey(), ga);
                    setActionUsed(ga);
                    actInst.getValue().clear();
                    actInst.getValue().add(ga);
                    counter.addActionInstantiation(actInst.getKey(), ga);
                    allGroundActions.add(ga);
                }
            }
            for(GAction ga : allGroundActions)
                setActionUsed(ga);

            for(GAction ga : alreadyUsed) {
                causalPending.addAll(ga.pre);
            }

            if(displayResolution()) {
                System.out.println("\n------- RPG ------");
            }

            while (!causalPending.isEmpty()) {
                Fluent og = causalPending.iterator().next();
                if(displayResolution())
                    System.out.println("pending: "+og+" (causal level = "+st.reachabilityGraphs.causalLevelOfFluent(og)+")");
                causalPending.remove(og);
                GAction ga;
                assert st.reachabilityGraphs.causalLevelOfFluent(og) != -1 : "Fluent "+og+" has been added to pending list but is not achievable";
                if(st.reachabilityGraphs.causalLevelOfFluent(og) == 0)
                    ga = null; // initial fact
                else
                    ga = selectMostInterestingAction(st.reachabilityGraphs.addableAndInPlanSupporters(og));

                // if we need action (ga !=null) and we didn't already used it
                if (ga != null && !alreadyUsed.contains(ga)) {
                    // find if we can use the instantiation of an action already in the plan
                    setActionUsed(ga);
                    causalPending.addAll(ga.pre);
                    if(planner.preprocessor.isHierarchical()) {
                        if (!inPlanActions.contains(ga)) // TODO this ignores unmotivated actions
                            // deriv all actions that are not already in the plan
                            derivPending.add(ga.task);

//                        System.out.println("2"+ga+" "+derivPending);
                        decompPending.addAll(ga.subTasks);
                    }
                    alreadyUsed.add(ga);
                    assert counter.totalOccurrences(ga) == 0; //!actionCount.containsKey(ga);
                    counter.addIndependentUsage(ga);
                }
            }
            int total = 0;
            for(GAction ga : counter.getAllUsedAction()) {
                total += counter.totalOccurrences(ga);
            }
            return total - st.getNumActions();
        } catch (NoSolutionException e) {
            if(displayResolution())
                e.printStackTrace();

            return 99999;
        }
    }

    /** Returns the expected cost of either:
     *    - binding lifted to ga (if lifted != null)
     *    - adding the action ga for support on the state varaible sv
     */
    private int costOfAction(Action lifted, GAction ga, GStateVariable sv) {
        if(lifted != null) {
            assert ga != null;
            if(actionInstantiations.get(lifted).contains(ga))
                return 0; // instantiation is in the domain
            else
                // instantiation outside of the current domain, hence the action has already been instantiated and it will be instantiated a second time
                return 1;
        } else {
            if(ga == null)
                return 0;
            else if(counter.maxAdditionalOccurrences(ga) > counter.occurencesForStateVariable(ga, sv))
                // for free: this action is already used more often on another state variable
                return 0;
            else
                return 1;
        }
    }
}
