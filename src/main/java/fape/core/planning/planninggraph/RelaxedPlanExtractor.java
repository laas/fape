package fape.core.planning.planninggraph;

import fape.core.inference.HLeveledReasoner;
import fape.core.planning.grounding.*;
import fape.core.planning.heuristics.DefaultIntRepresentation;
import fape.core.planning.heuristics.Preprocessor;
import fape.core.planning.heuristics.relaxed.*;
import fape.core.planning.planner.APlanner;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.NoSolutionException;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.*;

public class RelaxedPlanExtractor {

    final APlanner planner;
    final HLeveledReasoner<GAction,GTaskCond> decompReas;
    final HLeveledReasoner<GAction,GTaskCond> derivReaas;
    final HLeveledReasoner<GAction,Fluent> baseCausalReas;
    final State st;
    final Set<GAction> allowedActions;
    Set<GAction> inPlanActions;

    private boolean displayResolution() { return false; }

    Collection<GAction> alreadyUsed = new HashSet<>();

    Set<GTaskCond> derivPending = new HashSet<>();
    Set<GTaskCond> decompPending = new HashSet<>();
    Set<Fluent> causalPending = new HashSet<>();

    /** a causal reasoner for the current operations, this will change overtime */
    HLeveledReasoner<GAction,Fluent> currentCausalReasoner = null;


    public RelaxedPlanExtractor(APlanner planner, State st) {
        this.planner = planner;
        this.st = st;
        this.dtgs = new DTGCollection(planner, st, displayResolution());
        allowedActions = new HashSet<>(planner.preprocessor.getFeasibilityReasoner().getAllActions(st));
        inPlanActions = new HashSet<>();
        for(Action a : st.getAllActions()) {
            for(GAction ga : planner.preprocessor.getFeasibilityReasoner().getGroundActions(a, st))
                inPlanActions.add(ga);
        }
        if(planner.preprocessor.isHierarchical()) {
            decompReas = decomposabilityReasoner(st);
            derivReaas = derivabilityReasoner(st);
        } else {
            decompReas = null;
            derivReaas = null;
        }
//        baseCausalReas = new HLeveledReasoner<>();
//        for (GAction ga : planner.preprocessor.getFeasibilityReasoner().getAllActions(st)) {
//            baseCausalReas.addClause(ga.pre, ga.add, ga);
//        }
        baseCausalReas = planner.preprocessor.getLeveledCausalReasoner(st);
    }


    /** initial "facts" are actions with no subtasks */
    public HLeveledReasoner<GAction, GTaskCond> decomposabilityReasoner(State st) {
        HLeveledReasoner<GAction, GTaskCond> baseHLR = new HLeveledReasoner<>(planner.preprocessor.groundActionIntRepresentation(), new DefaultIntRepresentation<>());
        for (GAction ga : planner.preprocessor.getFeasibilityReasoner().getAllActions(st)) {
            GTaskCond[] effect = new GTaskCond[1];
            effect[0] = ga.task;
            baseHLR.addClause(ga.subTasks.toArray(new GTaskCond[ga.subTasks.size()]), effect, ga);
        }

        baseHLR.infer();
        for(GAction ga : allowedActions) {
            assert baseHLR.levelOfClause(ga) > -1 || inPlanActions.contains(ga);
        }
        return baseHLR;
    }

    /** initial facts opened tasks and initial clauses are non-motivated actions*/
    public HLeveledReasoner<GAction, GTaskCond> derivabilityReasoner(State st) {
        HLeveledReasoner<GAction, GTaskCond> baseHLR = new HLeveledReasoner<>(planner.preprocessor.groundActionIntRepresentation(), new DefaultIntRepresentation<>());
        for (GAction ga : planner.preprocessor.getFeasibilityReasoner().getAllActions(st)) {
            if(ga.abs.motivated()) {
                GTaskCond[] condition = new GTaskCond[1];
                condition[0] = ga.task;
                baseHLR.addClause(condition, ga.subTasks.toArray(new GTaskCond[ga.subTasks.size()]), ga);
            } else {
                baseHLR.addClause(new GTaskCond[0], ga.subTasks.toArray(new GTaskCond[ga.subTasks.size()]), ga);
            }
        }
        // all tasks (for complete relaxed plan)
        for(Task liftedTask : st.taskNet.getAllTasks()) {
            for(GTaskCond task : planner.preprocessor.getFeasibilityReasoner().getGroundedTasks(liftedTask, st))
                baseHLR.set(task);
        }
//        for(GTaskCond tc : planner.reachability.getDerivableTasks(st)) {
//            baseHLR.set(tc);
//        }
        baseHLR.infer();
        for(GAction ga : allowedActions) {
            assert baseHLR.levelOfClause(ga) > -1 || inPlanActions.contains(ga);
        }
        return baseHLR;
    }

    public HLeveledReasoner<GAction,Fluent> causalReasonerForOpenGoal(State st, Timeline consumer) {
        HLeveledReasoner<GAction, Fluent> hlr = baseCausalReas.clone();
        Collection<Fluent> init = planner.preprocessor.getGroundProblem().fluentsBefore(st, consumer.getFirstTimePoints());
        for (Fluent i : init) {
            hlr.set(i);
        }
        hlr.infer();
        return hlr;
    }

    private int costOfFluent(Fluent f) {
        if(achievedFluents.contains(f))
            // already achieved
            return 0;
        else if(causalPending.contains(f))
            // we already need to achieve it
            return 0;
        else
            return currentCausalReasoner.levelOfFact(f);
    }

    private int costOfPreconditions(GAction ga) {
        assert ga != null;
        if(alreadyUsed.contains(ga))
            return 0;
        int maxPreconditionCost = 0;
        for(Fluent f : currentCausalReasoner.conditionsOf(ga)) {
            int cost = costOfFluent(f);
            if (cost > maxPreconditionCost)
                maxPreconditionCost = cost;
        }
        assert maxPreconditionCost >= 0;
        return maxPreconditionCost;
    }

    private int costOfTask(GTaskCond t) {
        assert t != null;
        if(!derivReaas.knowsFact(t))
            return -1;
        if(!decompReas.knowsFact(t))
            return -1;
        if(derivReaas.levelOfFact(t) == -1 || decompReas.levelOfFact(t) == -1)
            return -1;
        return derivReaas.levelOfFact(t) + decompReas.levelOfFact(t);
    }

    public Fluent selectMostInterestingFluent(Collection<Fluent> fluents) throws NoSolutionException {
        Fluent bestFluent = null;
        int bestFluentCost = Integer.MAX_VALUE;
        for(Fluent f : fluents) {
            if(currentCausalReasoner.knowsFact(f) && currentCausalReasoner.levelOfFact(f) != -1) {
                int bestEnablerCost = Integer.MAX_VALUE;
                for(GAction ga : currentCausalReasoner.candidatesFor(f)) {
                    if(ga == null)
                        return f; // init fact, we can not do better
                    assert allowedActions.contains(ga);
                    int cost = costOfPreconditions(ga);
                    if(cost >= 0 && cost < bestEnablerCost)
                        bestEnablerCost = cost;
                }
                assert bestEnablerCost >= 0;
                if(bestEnablerCost < bestFluentCost) {
                    bestFluent = f;
                    bestFluentCost = bestEnablerCost;
                }
            }
        }
        if(bestFluent != null)
            return bestFluent;
        else
            throw new NoSolutionException("No best fluent in: "+fluents);
    }

    public GAction selectMostInterestingAction(Collection<GAction> actions) throws NoSolutionException {
        GAction bestAction = null;
        int bestActionCost = Integer.MAX_VALUE;
        for(GAction ga : actions) {
            if(ga == null)
                return null; // init fact, we can not do better
            if(alreadyUsed.contains(ga))
                return ga; // no additional cost as the action is already part of the relaxed plan

            int maxCostOfAchieved = 0;
            int sumPreconditionsCosts = 0;
            for(Fluent f : ga.pre) {
                int base = costOfFluent(f);
                if(base != 0) {
                    for(Action a : actionInstantiations.keySet()) {
                        for(GAction candidate : actionInstantiations.get(a)) {
                            for(Fluent gaAddition : candidate.add) {
                                if(gaAddition.equals(f)) {
                                    base = 0;
                                    break;
                                }
                            }
                            if(base == 0)
                                break;
                        }
                        if(base == 0)
                            break;
                    }
                }
                sumPreconditionsCosts += base;
            }
            for(Fluent f : ga.add) {
                if(causalPending.contains(f) && !achievedFluents.contains(f)) {
                    int c = currentCausalReasoner.levelOfFact(f);
                    assert c >= 0;
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
            if(planner.preprocessor.isHierarchical()) {
                if (!inPlanActions.contains(bestAction))
                    for (GTaskCond task : derivReaas.conditionsOf(bestAction))
                        assert derivReaas.levelOfFact(task) >= 0;

                for (GTaskCond task : decompReas.conditionsOf(bestAction))
                    assert decompReas.levelOfFact(task) >= 0;
            }

            return bestAction;

        } else {
            throw new NoSolutionException("No best action in "+actions);
        }
    }

    public GTaskCond selectMostInterestingTask(Collection<GTaskCond> tasks) {
        GTaskCond best = null;
        int bestCost = Integer.MAX_VALUE;
        for(GTaskCond t : tasks) {
            int cost = costOfTask(t);
            if(cost != -1 && cost < bestCost) {
                best = t;
                bestCost = cost;
            }
        }
        assert best != null;
        return best;
    }

    public GAction selectMostInterestingActionForRelaxedPlan(Collection<GAction> actions, Collection<Action> possiblyBfore,
                                                             Map<Action,Set<GAction>> actionInstantiations) throws NoSolutionException {

        GAction bestAction = null;
        int bestActionCost = Integer.MAX_VALUE;
        for(GAction ga : actions) {
            if(ga == null)
                return null; // init fact, we can not do better
            if(alreadyUsed.contains(ga))
                return ga; // no additional cost as the action is already part of the relaxed plan

            for(Action a : possiblyBfore) {
                if(actionInstantiations.get(a).contains(ga))
                    return ga; // this corresponds to an action we need to instantiate anyway
            }

            int cost = costOfPreconditions(ga);
            if(cost >= 0 && cost < bestActionCost) {
                bestActionCost = cost;
                bestAction = ga;
            }
        }
        if(bestAction != null) {
            if(!inPlanActions.contains(bestAction))
                for(GTaskCond task : derivReaas.conditionsOf(bestAction))
                    assert derivReaas.levelOfFact(task) >= 0;

            for(GTaskCond task : decompReas.conditionsOf(bestAction))
                assert decompReas.levelOfFact(task) >= 0;

            return bestAction;

        } else
            throw new NoSolutionException("No application actions in: "+actions);
    }

    Map<Action, Set<GAction>> actionInstantiations;

    private Map<Action, Set<GAction>> getInitialPossibleActionsInstantiations() {
        Map<Action, Set<GAction>> instantiations = new HashMap<>();

        for (Action a : st.getAllActions()) {
            instantiations.put(a, planner.preprocessor.getFeasibilityReasoner().getGroundActions(a, st));
        }
        return instantiations;
    }

    private HLeveledReasoner<GAction, Fluent> getCausalModelOfInitialDefinitions() {
        HLeveledReasoner<GAction, Fluent> causalModel = planner.preprocessor.getLeveledCausalReasoner(st); //baseCausalReas.clone();

        for (Timeline tl : st.getTimelines()) {
            for (ChainComponent cc : tl.getComponents()) {
                if (cc.change) {
                    LogStatement s = cc.getFirst();
                    if (st.getActionContaining(s) == null) {
                        // statement part of the initial problem definition
                        Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.endValue(), st, planner);
                        for (Fluent f : fluents) {
                            causalModel.set(f);
                        }
                    }
                }
            }
        }
        causalModel.infer();
        return causalModel;
    }



    final DTGCollection dtgs;

    /** maps a timeline to its DTG */
    Map<Timeline, Integer> dtgsOgTimelines = new HashMap<>();

    Map<GStateVariable, Integer> dtgsOfStateVariables= new HashMap<>();

    public OpenGoalTransitionFinder.Path getPathToPersistence(Timeline og) throws NoSolutionException {
        assert og.hasSinglePersistence();
        OpenGoalTransitionFinder pathFinder = new OpenGoalTransitionFinder(dtgs, displayResolution());
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
            pathFinder.setDTGUsable(dtgsOgTimelines.get(tl));
        }
        assert og.chain.length == 1;
        pathFinder.addPersistenceTargets(ogs, og.getFirst().getConsumeTimePoint(), og.getFirst().getSupportTimePoint());

        return pathFinder.bestPath(actionCostEvaluator);
    }

    public OpenGoalTransitionFinder.Path getPathToTransition(Timeline og) throws NoSolutionException {
        assert !og.hasSinglePersistence();
        OpenGoalTransitionFinder pathFinder = new OpenGoalTransitionFinder(dtgs, displayResolution());
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
            pathFinder.setDTGUsable(dtgsOfStateVariables.get(sv)); // TODO: might need to be built?
        }

        Collection<Timeline> potentialIndirectSupporters = new LinkedList<>();

        for(Timeline tl : st.getTimelines()) {
            if(tl == og)
                continue;
            if(tl.hasSinglePersistence())
                continue;
            if(!st.unifiable(tl, og))
                continue;
            if(!st.canBeBefore(tl, og))
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
            pathFinder.setDTGUsable(dtgsOgTimelines.get(tl));

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
        public boolean usable(int gActionID) {
            return usable(planner.preprocessor.getGroundAction(gActionID));
        }

        public int cost(Action a, GAction ga, GStateVariable sv) {
            return costOfAction(a, ga, sv);
        }

        public int distTo(GAction ga) {
            return costOfPreconditions(ga);
        }

        public boolean usable(GAction ga) {
            return allowedActions.contains(ga);
        }
    };

    HashSet<Fluent> achievedFluents = new HashSet<>();

    private void setActionUsed(GAction ga) {
        if(!alreadyUsed.contains(ga) && displayResolution())
            System.out.println("  act: "+ga);

        alreadyUsed.add(ga);
        for(Fluent f : ga.add) {
            achievedFluents.add(f);
        }
        for(Fluent f : ga.pre) {
            if(!causalPending.contains(f) && !achievedFluents.contains(f)) {
                causalPending.add(f);
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
                assert !dtgsOgTimelines.containsKey(tl);
                dtgsOgTimelines.put(tl, tlDtgID);
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
            } else {
                path = getPathToTransition(og);
            }
            allPaths.add(path);
            assert dtgs.isAccepting(path.start);
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
                    counter.addActionInstantiation(lifted, ground);
                    if(!instantiated.contains(lifted)) {
                        assert actionInstantiations.get(lifted).contains(ground) : "First instantiation is not in the domain.";
                        actionInstantiations.get(lifted).clear();
                        actionInstantiations.get(lifted).add(ground);
                        instantiated.add(lifted);
                    } else {
                        assert actionInstantiations.get(lifted).size() == 1;
                    }
                }
            }
        }

        return allPaths;
    }

    Set<Action> instantiated = new HashSet<>();
    ActionUsageTracker counter = null;

    public int myPerfectHeuristic() {
        counter = new ActionUsageTracker();
        try {
            if(displayResolution()) {
                System.out.println("\n\n ---- state: "+st.mID+"   -----");
            }
            initActionUsage();
            currentCausalReasoner = getCausalModelOfInitialDefinitions();
            actionInstantiations = getInitialPossibleActionsInstantiations();
            List<OpenGoalTransitionFinder.Path> paths = getPaths();

            Set<GAction> allGroundActions = new HashSet<>(counter.getAllUsedAction());

            for(Action a : counter.instantiations.keySet())
                assert instantiated.contains(a) && actionInstantiations.get(a).size() == 1;

            for(GAction ga : alreadyUsed) {
                causalPending.addAll(currentCausalReasoner.conditionsOf(ga));
            }

            if(displayResolution()) {
                System.out.println("\n---- Instantiation -----");
            }

            for(Map.Entry<Action, Set<GAction>> actInst : actionInstantiations.entrySet()) {
                if(!instantiated.contains(actInst.getKey())) {
                    instantiated.add(actInst.getKey());
                    final GAction ga = selectMostInterestingAction(actInst.getValue());
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
                causalPending.addAll(currentCausalReasoner.conditionsOf(ga));
            }

            if(displayResolution()) {
                System.out.println("\n------- RPG ------");
            }

            while (!causalPending.isEmpty()) {
                Fluent og = causalPending.iterator().next();
                if(displayResolution())
                    System.out.println("pending: "+og);
                causalPending.remove(og);
                GAction ga = selectMostInterestingAction(currentCausalReasoner.candidatesFor(og));

                // if we need action (ga !=null) and we didn't already used it
                if (ga != null && !alreadyUsed.contains(ga)) {
                    // find if we can use the instantiation of an action already in the plan
                    setActionUsed(ga);
                    causalPending.addAll(ga.pre);
                    if(planner.preprocessor.isHierarchical()) {
                        if (!inPlanActions.contains(ga)) // TODO this ignores unmotivated actions
                            // deriv all actions that are not already in the plan
                            derivPending.addAll(derivReaas.conditionsOf(ga));

//                        System.out.println("2"+ga+" "+derivPending);
                        decompPending.addAll(decompReas.conditionsOf(ga));
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
