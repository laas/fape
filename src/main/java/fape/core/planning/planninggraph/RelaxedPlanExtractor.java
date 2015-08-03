package fape.core.planning.planninggraph;

import fape.core.inference.HLeveledReasoner;
import fape.core.planning.grounding.*;
import fape.core.planning.heuristics.relaxed.*;
import fape.core.planning.planner.APlanner;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.NoSolutionException;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionCondition;
import planstack.anml.model.concrete.statements.LogStatement;

import java.sql.Time;
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
        HLeveledReasoner<GAction, GTaskCond> baseHLR = new HLeveledReasoner<>();
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
        HLeveledReasoner<GAction, GTaskCond> baseHLR = new HLeveledReasoner<>();
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
        for(ActionCondition liftedTask : st.taskNet.getAllTasks()) {
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
        Collection<Fluent> init = GroundProblem.fluentsBefore(st, consumer.getFirstTimePoints());
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
            throw new NoSolutionException();
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
        if(displayResolution())
            System.out.print("");
        if(bestAction != null) {
//            if(st.mID == 16) {
//                System.out.println(bestAction+"\n");
//            }
            if(planner.preprocessor.isHierarchical()) {
                if (!inPlanActions.contains(bestAction))
                    for (GTaskCond task : derivReaas.conditionsOf(bestAction))
                        assert derivReaas.levelOfFact(task) >= 0;

                for (GTaskCond task : decompReas.conditionsOf(bestAction))
                    assert decompReas.levelOfFact(task) >= 0;
            }

            return bestAction;

        } else {
            throw new NoSolutionException();
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
            throw new NoSolutionException();
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
                        Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.endValue(), st, true);
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


    //    static AnmlProblem lastProblem = null;
//    static HLeveledReasoner<GAction,Fluent>
    protected static boolean debugging = false;
    public static boolean debugging2 = false;
    public int relaxedGroundPlan(State st) {
        try {
            alreadyUsed = new HashSet<>();
            inPlanActions = new HashSet<>();

            actionInstantiations = getInitialPossibleActionsInstantiations();
            currentCausalReasoner = getCausalModelOfInitialDefinitions();

            for (Timeline tl : st.tdb.getConsumers()) {
                if(debugging)
                    System.out.println("opengoal: "+Printer.inlineTemporalDatabase(st, tl));
                Collection<Action> possiblyBefore = new LinkedList<>();
                for(Action a : st.getAllActions()) {
                    if(st.canAllBeBefore(a.start(), tl.getFirstTimePoints())) {
                        possiblyBefore.add(a);
                    }
                }
                assert causalPending.isEmpty();
                causalPending = new HashSet<>();

                Collection<Fluent> disjunctiveGoals = DisjunctiveFluent.fluentsOf(tl.stateVariable, tl.getGlobalConsumeValue(), st, true);
                Fluent selectedGoal = selectMostInterestingFluent(disjunctiveGoals);
                causalPending.add(selectedGoal);
                if(debugging) {
                    System.out.println("  goals: "+disjunctiveGoals);
                    System.out.println("  selected: "+selectedGoal);
                }

                while (!causalPending.isEmpty()) {
                    Fluent og = causalPending.iterator().next();
                    if(debugging)
                        System.out.println("    current subgoal: "+og);
                    causalPending.remove(og);
                    GAction ga = selectMostInterestingActionForRelaxedPlan(currentCausalReasoner.candidatesFor(og), possiblyBefore, actionInstantiations);
                    if(debugging)
                        System.out.println("    action for sub goal: "+ga);

                    // if we need action (ga !=null) and we didn't already used it
                    if (ga != null && !alreadyUsed.contains(ga)) {
                        // find if we can use the instantiation of an action already in the plan
                        for(Action a : possiblyBefore) {
                            if(actionInstantiations.get(a).contains(ga)) {
                                actionInstantiations.get(a).clear();
                                actionInstantiations.get(a).add(ga); // this action is instantiated with that
                                break;
                            }
                        }

                        causalPending.addAll(ga.pre);
                        if (!inPlanActions.contains(ga)) // TODO this ignores unmotivated actions
                            // deriv all actions that are not already in the plan
                            derivPending.addAll(derivReaas.conditionsOf(ga));

                        decompPending.addAll(decompReas.conditionsOf(ga));
                        alreadyUsed.add(ga);
                    }
                }
            }

            for(Action a : actionInstantiations.keySet()) {
                assert actionInstantiations.get(a).size() > 0;

                Collection<Action> possiblyBefore = new LinkedList<>();
                for(Action lifted : st.getAllActions()) {
                    if(st.canBeBefore(a.start(), lifted.start())) {
                        possiblyBefore.add(lifted);
                    }
                }

                if(actionInstantiations.get(a).size() != 1 || !alreadyUsed.contains(actionInstantiations.get(a).iterator().next())) {
                    // this lifted action is not instantiated or not recorded as part of the plan
                    GAction instantiation = selectMostInterestingAction(actionInstantiations.get(a));
                    actionInstantiations.get(a).clear();
                    actionInstantiations.get(a).add(instantiation);
                    assert instantiation != null;
                    causalPending.addAll(currentCausalReasoner.conditionsOf(instantiation));

                    while (!causalPending.isEmpty()) {
                        Fluent og = causalPending.iterator().next();
                        causalPending.remove(og);
                        GAction ga = selectMostInterestingActionForRelaxedPlan(currentCausalReasoner.candidatesFor(og), possiblyBefore, actionInstantiations);

                        // if we need action (ga !=null) and we didn't already used it
                        if (ga != null && !alreadyUsed.contains(ga)) {
                            // find if we can use the instantiation of an action already in the plan
                            for (Action liftedAction : possiblyBefore) {
                                if (actionInstantiations.get(liftedAction).contains(ga)) {
                                    actionInstantiations.get(liftedAction).clear();
                                    actionInstantiations.get(liftedAction).add(ga); // this action is instantiated with that
                                    break;
                                }
                            }

                            causalPending.addAll(ga.pre);
                            if (!inPlanActions.contains(ga)) // TODO this ignores unmotivated actions
                                // deriv all actions that are not already in the plan
                                derivPending.addAll(derivReaas.conditionsOf(ga));

                            decompPending.addAll(decompReas.conditionsOf(ga));
                            alreadyUsed.add(ga);
                        }
                    }
                }
                assert actionInstantiations.get(a).size() == 1;
            }
//            if(debugging)
//                System.out.println("    state: "+st.mID);
            for(Action a : st.getAllActions()) {
                assert actionInstantiations.get(a).size() == 1;
                GAction instantiated = actionInstantiations.get(a).iterator().next();
                alreadyUsed.remove(instantiated);
                if(debugging) {
                    System.out.println("    " + instantiated + "  <-  " + a);
                }
            }

//            if(debugging)
//                for(GAction ga : alreadyUsed)
//                    System.out.println("    "+ga+"      additional");

//            if(debugging)
//                System.out.println("    Size: "+(alreadyUsed.size()+st.getNumActions()));
//            System.out.println("Size of relaxed plan: "+alreadyUsed.size());
//            System.out.println(alreadyUsed);
//            System.out.println(actionInstantiations);
            currentCausalReasoner = null;
            return alreadyUsed.size();
        } catch (NoSolutionException e) {
//            e.printStackTrace();
            return 999999;

//            throw new FAPEException("");
//            System.out.println("no-solutions");
//            return null;
        }
    }

    public static int next = 0;

    public PartialPathDTG getPathToPersistence(Timeline og) throws NoSolutionException {
        assert og.hasSinglePersistence();
        OpenGoalTransitionFinder pathFinder = new OpenGoalTransitionFinder();
        Collection<Fluent> ogs = DisjunctiveFluent.fluentsOf(og.stateVariable, og.getGlobalConsumeValue(), st, false);
        Set<GStateVariable> possibleStateVariables = new HashSet<>();
        for (Fluent f : ogs)
            possibleStateVariables.add(f.sv);
//            transitions.addStartNodes(ogs); TODO

        for (GStateVariable sv : possibleStateVariables) {
            pathFinder.addDTG(planner.preprocessor.getDTG(sv));
            if (previousPaths.containsKey(sv))
                for (PartialPathDTG dtg : previousPaths.get(sv))
                    pathFinder.addDTG(dtg);
        }

        Collection<Timeline> potentialIndirectSupporters = new LinkedList<>();

        for (Timeline tl : st.getTimelines()) {
            if (tl == og)
                continue;
            if (tl.hasSinglePersistence())
                continue;
            if (!st.unifiable(tl, og))
                continue;
            if (!st.canAllBeBefore(tl.getFirstChange().end(), og.getFirstTimePoints()))
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
                                    throw new NoSolutionException(); // unsolvable threat
                            }
                        }
                }
            }
        }
        potentialIndirectSupporters.removeAll(toRemove);

        for (Timeline tl : potentialIndirectSupporters) {
            pathFinder.addDTG(timelineDTGs.get(tl));
            for (PartialPathDTG dtg : previousSolutions.get(tl)) {
                pathFinder.addDTG(dtg);
            }
        }
        assert og.chain.length == 1;
        pathFinder.addPersistenceTargets(ogs, og.getFirst().getConsumeTimePoint(), og.getFirst().getSupportTimePoint(), st);

//        if (debugging2 && pathFinder.startNodes.isEmpty()) {
//            System.out.println(ogs);
//            System.out.println(Printer.temporalDatabase(st, og));
//            for (Timeline t : toRemove)
//                System.out.println("  " + Printer.inlineTemporalDatabase(st, t));
//            System.out.println("BREAK HERE");
//        }

        return pathFinder.bestPath(actionCostEvaluator);
    }

    public PartialPathDTG getPathToTransition(Timeline og) throws NoSolutionException {
        assert !og.hasSinglePersistence();
        OpenGoalTransitionFinder pathFinder = new OpenGoalTransitionFinder();
        Collection<Fluent> ogs = DisjunctiveFluent.fluentsOf(og.stateVariable, og.getGlobalConsumeValue(), st, false);
        Set<GStateVariable> possibleStateVariables = new HashSet<>();
        for(Fluent f : ogs)
            possibleStateVariables.add(f.sv);

        for(GStateVariable sv : possibleStateVariables) {
            pathFinder.addDTG(planner.preprocessor.getDTG(sv));
            if(previousPaths.containsKey(sv))
                for(PartialPathDTG dtg : previousPaths.get(sv))
                    pathFinder.addDTG(dtg);
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
                if(st.unified(tl1, tl2)) {
                    // if the last time points of tl2 cannot be before the first of tl1, then tl1 can't be after tl2.
//                        if(!st.canAllBeBefore(tl2.getLastTimePoints(), tl1.getFirstChangeTimePoint()))
                    if(!st.canBeBefore(tl2, tl1))
                        // in addition og can't be before tl2
                        if(!st.canBeBefore(og, tl2)) {
                            // the order is necessarily tl1 -> tl2 -> og hence tl1 can be removed from the set of potential supporters
                            toRemove.add(tl1);
                            if(toRemove.contains(tl2)) {
                                if(!st.canBeBefore(tl1, tl2) && !st.canBeBefore(tl2, tl1) && st.unified(tl1,tl2))
                                    throw new NoSolutionException(); // unsolvable threat
                            }
                        }
                }
            }
        }
        potentialIndirectSupporters.removeAll(toRemove);

        for(Timeline tl : potentialIndirectSupporters) {
            pathFinder.addDTG(timelineDTGs.get(tl));
            for(PartialPathDTG dtg : previousSolutions.get(tl)) {
                pathFinder.addDTG(dtg);
            }
        }

        pathFinder.addTransitionTargets(ogs, og.getFirstChangeTimePoint());
//        if(debugging2 && pathFinder.startNodes.isEmpty()) {
//            System.out.println(ogs);
//            System.out.println(Printer.temporalDatabase(st, og));
//            for(Timeline t : toRemove)
//                System.out.println("  "+Printer.inlineTemporalDatabase(st, t));
//
//        }



//        if(st.mID == 322 && og.numChanges() > 30)
//            pathFinder.print("coucou.dot", actionCostEvaluator);

        return pathFinder.bestPath(actionCostEvaluator);
    }

    OpenGoalTransitionFinder.CostEvaluator actionCostEvaluator = new OpenGoalTransitionFinder.CostEvaluator() {
        @Override
        public int cost(Action a, GAction ga, GStateVariable sv) {
            return costOfAction(a, ga, sv);
        }

        @Override
        public int distTo(GAction ga) {
            return costOfPreconditions(ga);
        }

        @Override
        public boolean usable(GAction ga) {
            return allowedActions.contains(ga);
        }
    };

    Map<Timeline, TimelineDTG> timelineDTGs;
    Map<Timeline, List<PartialPathDTG>> previousSolutions;
    Map<GStateVariable, List<PartialPathDTG>> previousPaths;

    HashSet<Fluent> achievedFluents = new HashSet<>();

    private void setActionUsed(GAction ga) {
        if(!alreadyUsed.contains(ga) && displayResolution()) {
            System.out.println("  act: "+ga);
        }
        alreadyUsed.add(ga);
        for(Fluent f : ga.add) {
            achievedFluents.add(f);
        }
        for(Fluent f : ga.pre) {
            if(!causalPending.contains(f) && !achievedFluents.contains(f)) {
                causalPending.add(f);
                if (displayResolution()) {
                    System.out.println("    pending: " + f);
                }
            }
        }
    }

    private void initActionUsage() {
        alreadyUsed = new HashSet<>();
        achievedFluents = new HashSet<>();
    }

    public List<PartialPathDTG> getPaths() throws NoSolutionException {
        timelineDTGs = new HashMap<>();
        previousSolutions = new HashMap<>();
        previousPaths = new HashMap<>();

//        if(debugging2 && st.mID == 148) {
//            System.out.println(Printer.temporalDatabaseManager(st));
//        }

        for(Timeline tl : st.getTimelines()) {
            if(!tl.hasSinglePersistence()) {
                timelineDTGs.put(tl, new TimelineDTG(tl, st, planner));
                previousSolutions.put(tl, new LinkedList<PartialPathDTG>());
            }
        }

        List<Timeline> opengoals = new LinkedList<>(st.tdb.getConsumers());
        Collections.sort(opengoals, new Comparator<Timeline>() {
            @Override
            public int compare(Timeline t1, Timeline t2) {
                return st.getEarliestStartTime(t1.getFirst().getFirst().start()) - st.getEarliestStartTime(t2.getFirst().getFirst().start());
            }
        });
//        System.out.println();
        for(Timeline og : opengoals) {
            if(displayResolution()) {
                System.out.println("og: "+Printer.inlineTemporalDatabase(st, og));
            }
//            System.out.println(Printer.inlineTemporalDatabase(st, og));
            PartialPathDTG path;
            if(og.hasSinglePersistence()) {
                path = getPathToPersistence(og);
            } else {
                path = getPathToTransition(og);
                path.extendWith(timelineDTGs.get(og));
                timelineDTGs.get(og).hasBeenExtended = true; //TODO add tiempoint infomation ?
                previousSolutions.get(og).add(path);
            }
//            if(debugging2) {
//                System.out.println(Printer.inlineTemporalDatabase(st, og));
//                System.out.println(path);
//            }
            if(path.extendedTimeline() != null) { //TODO handle case with no supporter
                previousSolutions.get(path.extendedTimeline()).add(path);
            } else {
                if(!previousPaths.containsKey(path.getStateVariable()))
                    previousPaths.put(path.getStateVariable(), new LinkedList<>());
                previousPaths.get(path.getStateVariable()).add(path);
            }
//            System.out.println("OpenGoal: "+Printer.inlineTemporalDatabase(st, og));
            for(DomainTransitionGraph.DTEdge addedEdge : path.additionalEdges()) {
                if(addedEdge.ga != null) {
                    setActionUsed(addedEdge.ga);
                    if(addedEdge.act == null) {// not an instantiation
                        counter.addActionOccurrence(addedEdge.ga, path.getStateVariable());
//                        System.out.println("  " + addedEdge.ga);
                    } else {// instantiation
                        counter.addActionInstantiation(addedEdge.act, addedEdge.ga);
//                        System.out.println("  " + addedEdge.ga + "    (--" + addedEdge.act + "--)");
                        if(!instantiated.contains(addedEdge.act)) {
                            assert actionInstantiations.get(addedEdge.act).contains(addedEdge.ga);
                            actionInstantiations.get(addedEdge.act).clear();
                            actionInstantiations.get(addedEdge.act).add(addedEdge.ga);
                            instantiated.add(addedEdge.act);
                        } else {
                            assert actionInstantiations.get(addedEdge.act).size() == 1;
                        }
                    }
                }
            }

            next ++;
        }

        List<PartialPathDTG> allPaths = new LinkedList<>();
        for(Timeline tl : previousSolutions.keySet())
            allPaths.addAll(previousSolutions.get(tl));
        for(GStateVariable sv : previousPaths.keySet())
            allPaths.addAll(previousPaths.get(sv));
        return allPaths;
    }
    Set<Action> instantiated = new HashSet<>();
    ActionUsageTracker counter = null;

    int breakState = 82;

    public int myPerfectHeuristic() {
        counter = new ActionUsageTracker();
        try {
            if(displayResolution()) {
                System.out.println("\n\n ---- state: "+st.mID+"   -----");
            }
            initActionUsage();
            currentCausalReasoner = getCausalModelOfInitialDefinitions();
            actionInstantiations = getInitialPossibleActionsInstantiations();
            List<PartialPathDTG> paths = getPaths();

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
                    GAction ga = selectMostInterestingAction(actInst.getValue());
//                    if(st.mID == 16)
//                        System.out.print("");
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

//                    if(debugging)
//                        System.out.println(st.mID+"    New action from RPG: "+ga);
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
//            if(debugging2)
//                System.out.println("State id : "+st.mID);
            for(GAction ga : counter.getAllUsedAction()) {
                total += counter.totalOccurrences(ga);
            }
//            if(debugging2) {
//                System.out.println(total);
//                System.out.println("not inst: " + (st.getAllActions().size() - instantiated.size()));
//            }
            return total - st.getNumActions();
        } catch (NoSolutionException e) {
//            if(debugging2)
//                e.printStackTrace();
            return 99999;
        }
    }

    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nState: "+st.mID+"\n");
        sb.append("-------------- Timelines -----------\n");
        for(Timeline tl : previousSolutions.keySet()) {
            sb.append(Printer.inlineTemporalDatabase(st, tl)+"\n");
            for(PartialPathDTG ppd : previousSolutions.get(tl)) {
                sb.append("  ");
                sb.append(ppd.startPath+"\n");
            }
        }
        sb.append("\n-------------- State variables -----------\n");
        for(GStateVariable sv : previousPaths.keySet()) {
            sb.append(sv+"\n");
            for(PartialPathDTG ppd : previousPaths.get(sv)) {
                sb.append("  "+ppd.startPath+"\n");
            }
        }
        sb.append("\n");
        return sb.toString();
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
