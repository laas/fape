package fape.core.planning.planninggraph;

import fape.core.inference.HLeveledReasoner;
import fape.core.planning.grounding.*;
import fape.core.planning.heuristics.relaxed.OpenGoalTransitionFinder;
import fape.core.planning.heuristics.relaxed.PathDTG;
import fape.core.planning.heuristics.relaxed.TimelineDTG;
import fape.core.planning.planner.APlanner;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.NoSolutionException;
import fape.util.Pair;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionCondition;
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


    Collection<GAction> alreadyUsed = new HashSet<>();

    Set<GTaskCond> derivPending = new HashSet<>();
    Set<GTaskCond> decompPending = new HashSet<>();
    Set<Fluent> causalPending = new HashSet<>();

    /** a causal reasoner for the current operations, this will change overtime */
    HLeveledReasoner<GAction,Fluent> currentCausalReasoner = null;


    public RelaxedPlanExtractor(APlanner planner, State st) {
        this.planner = planner;
        this.st = st;
        allowedActions = new HashSet<>(planner.reachability.getAllActions(st));
        inPlanActions = new HashSet<>();
        for(Action a : st.getAllActions()) {
            for(GAction ga : planner.reachability.getGroundActions(a, st))
                inPlanActions.add(ga);
        }
        decompReas = decomposabilityReasoner(st);
        derivReaas = derivabilityReasoner(st);
        baseCausalReas = new HLeveledReasoner<>();
        for (GAction ga : planner.reachability.getAllActions(st)) {
            baseCausalReas.addClause(ga.pre, ga.add, ga);
        }
    }


    /** initial "facts" are actions with no subtasks */
    public HLeveledReasoner<GAction, GTaskCond> decomposabilityReasoner(State st) {
        HLeveledReasoner<GAction, GTaskCond> baseHLR = new HLeveledReasoner<>();
        for (GAction ga : planner.reachability.getAllActions(st)) {
            GTaskCond[] effect = new GTaskCond[1];
            effect[0] = ga.task;
            baseHLR.addClause(ga.subTasks.toArray(new GTaskCond[ga.subTasks.size()]), effect, ga);
        }

        baseHLR.infer();
        for(GAction ga : allowedActions) {
            if(!(baseHLR.levelOfClause(ga) > -1) && !inPlanActions.contains(ga))
                System.out.println(baseHLR.report());
            assert baseHLR.levelOfClause(ga) > -1 || inPlanActions.contains(ga);
        }
        return baseHLR;
    }


    /** initial facts opened tasks and initial clauses are non-motivated actions*/
    public HLeveledReasoner<GAction, GTaskCond> derivabilityReasoner(State st) {
        HLeveledReasoner<GAction, GTaskCond> baseHLR = new HLeveledReasoner<>();
        for (GAction ga : planner.reachability.getAllActions(st)) {
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
            for(GTaskCond task : planner.reachability.getGroundedTasks(liftedTask, st))
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

    private int costOfAction(GAction ga) {
        assert ga != null;
//        System.out.println(currentCausalReasoner.levelOfClause(ga) +" "+ derivReaas.levelOfClause(ga)+" "+ decompReas.levelOfClause(ga));
        if(inPlanActions.contains(ga))
            return 0;
        if(debugging)
            System.out.println(ga+": "+currentCausalReasoner.levelOfClause(ga) +" "+ derivReaas.levelOfClause(ga)+" "+ decompReas.levelOfClause(ga));
        if(currentCausalReasoner.levelOfClause(ga) <0 || derivReaas.levelOfClause(ga)<0 || decompReas.levelOfClause(ga)<0)
            return -1; // impossible in at least one
        return currentCausalReasoner.levelOfClause(ga) +
                derivReaas.levelOfClause(ga) -1 +
                decompReas.levelOfClause(ga) -1;
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
                    int cost = costOfAction(ga);
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
            int cost = costOfAction(ga);
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

        } else {
//            selectMostInterestingAction(actions);
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

    public int numAdditionalSteps() {
        inPlanActions = new HashSet<>();
        for(Action a : st.getAllActions()) {
            for(GAction ga : planner.reachability.getGroundActions(a, st))
                inPlanActions.add(ga);
        }
        alreadyUsed = new HashSet<>();
        assert alreadyUsed.isEmpty() : "THis method should be called only once for this object.";
        try {

            for (Timeline tl : st.tdb.getConsumers()) {
                currentCausalReasoner = causalReasonerForOpenGoal(st, tl);
                assert causalPending.isEmpty();
                causalPending = new HashSet<>();

                Collection<Fluent> goals = DisjunctiveFluent.fluentsOf(tl.stateVariable, tl.getGlobalConsumeValue(), st, true);
                causalPending.add(selectMostInterestingFluent(goals));

                while (!causalPending.isEmpty()) {
                    Fluent og = causalPending.iterator().next();
                    causalPending.remove(og);
                    GAction ga = selectMostInterestingAction(currentCausalReasoner.candidatesFor(og));

                    // if we need action (ga !=null) and we didn't already used it
                    if(ga != null && !alreadyUsed.contains(ga)) {
                        causalPending.addAll(ga.pre);
                        if(!inPlanActions.contains(ga)) // TODO this ignores unmotivated actions
                            // deriv all actions that are not alreay in the plan
                            derivPending.addAll(derivReaas.conditionsOf(ga));

//                        System.out.println("2"+ga+" "+derivPending);
                        decompPending.addAll(decompReas.conditionsOf(ga));
                        alreadyUsed.add(ga);
                    }
                }
                currentCausalReasoner = null;
            }

            assert causalPending.isEmpty();
            causalPending = new HashSet<>();
            currentCausalReasoner = baseCausalReas.clone();
            Collection<Fluent> init = GroundProblem.allFluents(st); //GroundProblem.fluentsBefore(st, consumer.getFirstTimePoints());
            for (Fluent i : init) {
                currentCausalReasoner.set(i);
            }
            currentCausalReasoner.infer();
            for(ActionCondition liftedTask : st.getOpenTaskConditions()) {
                Collection<GTaskCond> tasks = planner.reachability.getGroundedTasks(liftedTask, st);
                decompPending.add(selectMostInterestingTask(tasks));
            }
            for(Action liftedAction : st.getOpenLeaves()) {
                Collection<GAction> groundActions = planner.reachability.getGroundActions(liftedAction, st);
                GAction candidate =selectMostInterestingAction(groundActions);
//                System.out.println("openleaves:"+Printer.action(st, liftedAction) + "  " + candidate);
                decompPending.addAll(decompReas.conditionsOf(candidate));
            }
            for(Action liftedAction : st.getUnmotivatedActions()) {
                Collection<GAction> groundActions = planner.reachability.getGroundActions(liftedAction, st);
                GAction candidate = selectMostInterestingAction(groundActions);
                derivPending.addAll(derivReaas.conditionsOf(candidate));
            }

            while(!causalPending.isEmpty() || !derivPending.isEmpty() || !decompPending.isEmpty()) {
                GAction ga;
                if(!causalPending.isEmpty()) {
                    Fluent og = causalPending.iterator().next();
                    causalPending.remove(og);
                    ga = selectMostInterestingAction(currentCausalReasoner.candidatesFor(og));
                } else if(!derivPending.isEmpty()) {
                    GTaskCond task = derivPending.iterator().next();
                    derivPending.remove(task);
                    if(!derivReaas.knowsFact(task) || derivReaas.levelOfFact(task) == -1)
                        throw new NoSolutionException();
                    ga = selectMostInterestingAction(derivReaas.candidatesFor(task));
                } else {
                    assert !decompPending.isEmpty();
                    GTaskCond task = decompPending.iterator().next();
                    decompPending.remove(task);
                    ga = selectMostInterestingAction(decompReas.candidatesFor(task));
                }
                // if we need action (ga !=null) and we didn't already used it
                if(ga != null && !alreadyUsed.contains(ga)) {
                    causalPending.addAll(ga.pre);
                    derivPending.addAll(derivReaas.conditionsOf(ga));
                    decompPending.addAll(decompReas.conditionsOf(ga));
                    alreadyUsed.add(ga);
                }
            }
            currentCausalReasoner = null;

            int actionsToAdd = 0;
            for(GAction ga : alreadyUsed) {
                if(!inPlanActions.contains(ga))
                    actionsToAdd++;
            }
            return actionsToAdd;
        } catch (NoSolutionException e) {
            return 9999999;
        }
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

            int cost = costOfAction(ga);
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
            instantiations.put(a, planner.reachability.getGroundActions(a, st));
        }
        return instantiations;
    }

    private HLeveledReasoner<GAction, Fluent> getCausalModelOfInitialDefinitions() {
        HLeveledReasoner<GAction, Fluent> causalModel = baseCausalReas.clone();

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
    boolean debugging = false;
    public int relaxedGroundPlan(State st) {
        if(debugging)
            System.out.println("State: "+st.mID);
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

//                        System.out.println("2"+ga+" "+derivPending);
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

//                        System.out.println("2"+ga+" "+derivPending);
                            decompPending.addAll(decompReas.conditionsOf(ga));
                            alreadyUsed.add(ga);
                        }
                    }
                }
                assert actionInstantiations.get(a).size() == 1;
            }
            boolean display = debugging;
            if(display)
                System.out.println("    state: "+st.mID);
            for(Action a : st.getAllActions()) {
                assert actionInstantiations.get(a).size() == 1;
                GAction instantiated = actionInstantiations.get(a).iterator().next();
                alreadyUsed.remove(instantiated);
                if(display) {
                    System.out.println("    " + instantiated + "  <-  " + a);
                }
            }

            if(display)
                for(GAction ga : alreadyUsed)
                    System.out.println("    "+ga+"      additional");

            if(display)
                System.out.println("    Size: "+(alreadyUsed.size()+st.getNumActions()));
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

    public PathDTG getPath(Timeline og) throws NoSolutionException {
        OpenGoalTransitionFinder pathFinder = new OpenGoalTransitionFinder();
        Collection<Fluent> ogs = DisjunctiveFluent.fluentsOf(og.stateVariable, og.getGlobalConsumeValue(), st, false);
        Set<GStateVariable> possibleStateVariables = new HashSet<>();
        for(Fluent f : ogs)
            possibleStateVariables.add(f.sv);
//            transitions.addStartNodes(ogs); TODO

        for(GStateVariable sv : possibleStateVariables) {
            if (dtgs.hasDTGFor(sv))
                pathFinder.addDTG(dtgs.getDTGOf(sv));
            if(previousPaths.containsKey(sv))
                for(PathDTG dtg : previousPaths.get(sv))
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
            if(!st.canAllBeBefore(tl.getFirstChange().end(), og.getFirstTimePoints()))
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
            for(PathDTG dtg : previousSolutions.get(tl)) {
                pathFinder.addDTG(dtg);
            }
        }

        pathFinder.addSources(ogs);
        if(pathFinder.startNodes.isEmpty()) {
            System.out.println(ogs);
            System.out.println(Printer.temporalDatabase(st, og));
            for(Timeline t : toRemove)
                System.out.println("  "+Printer.inlineTemporalDatabase(st, t));
            System.out.println("BREAK HERE");
        }

        OpenGoalTransitionFinder.TransitionSequence seq = pathFinder.bestPath(new OpenGoalTransitionFinder.CostEvaluator() {
            @Override
            public int cost(Action a, GAction ga) {
                return costOfAction(a, ga);
            }
        });
        PathDTG path = seq.getDTG();
        return path;
    }

    GroundDTGs dtgs;
    Map<Timeline, TimelineDTG> timelineDTGs;
    Map<Timeline, List<PathDTG>> previousSolutions;
    Map<GStateVariable, List<PathDTG>> previousPaths;

    public List<PathDTG> getPaths() throws NoSolutionException {
        dtgs = new GroundDTGs(allowedActions);
        timelineDTGs = new HashMap<>();
        previousSolutions = new HashMap<>();
        previousPaths = new HashMap<>();

        for(Timeline tl : st.getTimelines()) {
            if(!tl.hasSinglePersistence()) {
                timelineDTGs.put(tl, new TimelineDTG(tl, st, planner, planner.reachability));
                previousSolutions.put(tl, new LinkedList<PathDTG>());
            }
        }

        for(Timeline og : st.tdb.getConsumers()) {
            if(og.hasSinglePersistence())
                continue;
//            DomainTransitions transitions = new DomainTransitions(planner.reachability, st);

            if(seq.supporter != null) { //TODO handle case with no supporter
                previousSolutions.get(seq.supporter).add(path);
            } else {
                if(!previousPaths.containsKey(path.getStateVariable()))
                    previousPaths.put(path.getStateVariable(), new LinkedList<PathDTG>());
                previousPaths.get(path.getStateVariable()).add(path);
            }
//            if(seq.supporter != null && seq.hasGraphChange()) {
//                System.out.println(Printer.inlineTemporalDatabase(st, seq.supporter));
//                System.out.println(seq.getActionsSequence());
//                System.out.println();
//            }
            next ++;
//                transitions.print("/tmp/transitions-" + next++);
//                System.out.println("BREAK");
        }

        // cleaning up redundant paths
        for(Timeline tl : previousSolutions.keySet()) {
            Set<PathDTG> toRemove = new HashSet<>();
            for(PathDTG p1 : previousSolutions.get(tl)) {
                for(PathDTG p2 : previousSolutions.get(tl)) {
                    if(p1 == p2)
                        continue;
                    if(p2.contains(p1) && !toRemove.contains(p2))
                        toRemove.add(p1);
                }
            }
            previousSolutions.get(tl).removeAll(toRemove);
        }

        for(GStateVariable sv : previousPaths.keySet()) {
            Set<PathDTG> toRemove = new HashSet<>();
            for(PathDTG p1 : previousPaths.get(sv)) {
                for(PathDTG p2 : previousPaths.get(sv)) {
                    if(p1 == p2)
                        continue;
                    if(p2.contains(p1) && !toRemove.contains(p2))
                        toRemove.add(p1);
                }
            }
            previousPaths.get(sv).removeAll(toRemove);
        }

//        if(st.mID > 25) {
//            System.out.println("BREAK.");
//            for(Timeline tl : previousSolutions.keySet()) {
//                System.out.println(Printer.inlineTemporalDatabase(st, tl));
//                for(PathDTG path : previousSolutions.get(tl))
//                    System.out.println("  "+path+(path.extendedSolution != null ? ("      ext:"+path.extendedSolution) : ""));
//            }
//        }
        List<PathDTG> allPaths = new LinkedList<>();
        for(Timeline tl : previousSolutions.keySet())
            allPaths.addAll(previousSolutions.get(tl));
        for(GStateVariable sv : previousPaths.keySet())
            allPaths.addAll(previousPaths.get(sv));
        return allPaths;
    }

    public int myPerfectHeuristic() {
        try {
            currentCausalReasoner = getCausalModelOfInitialDefinitions();

            List<PathDTG> paths = getPaths();
            actionInstantiations = getInitialPossibleActionsInstantiations();
            Set<Action> instantiated = new HashSet<>();
            Set<GAction> allGroundActions = new HashSet<>();
            for(PathDTG path : paths) {
                for(Pair<Action, GAction> actBinding : path.actionBindings()) {
                    if(!instantiated.contains(actBinding.value1)) {
                        instantiated.add(actBinding.value1);
                        assert actionInstantiations.get(actBinding.value1).contains(actBinding.value2);
                        actionInstantiations.get(actBinding.value1).clear();
                        actionInstantiations.get(actBinding.value1).add(actBinding.value2);
                    }
                }
                allGroundActions.addAll(path.allGroundActions());
            }
            Map<GAction, Map<GStateVariable, Integer>> actionCountPetSV = new HashMap<>();
            for(PathDTG path : paths) {
                GStateVariable sv = path.getStateVariable();
                for(GAction ga : path.allGroundActions()) {
                    if(!actionCountPetSV.containsKey(ga))
                        actionCountPetSV.put(ga, new HashMap<GStateVariable, Integer>());
                    if(!actionCountPetSV.get(ga).containsKey(sv))
                    actionCountPetSV.get(ga).put(sv, 0);
                    actionCountPetSV.get(ga).put(sv, 1+ actionCountPetSV.get(ga).get(sv));
                }
            }
            Map<GAction,Integer> actionCount = new HashMap<>();
            for(Map.Entry<GAction,Map<GStateVariable,Integer>> actionMapEntry : actionCountPetSV.entrySet()) {
                int maxCount = 0;
                for(Integer cnt : actionMapEntry.getValue().values()) {
                    if(cnt > maxCount)
                        maxCount = cnt;
                }
                actionCount.put(actionMapEntry.getKey(), maxCount);
            }

            for(Map.Entry<Action, Set<GAction>> actInst : actionInstantiations.entrySet()) {
                if(!instantiated.contains(actInst.getKey())) {
                    instantiated.add(actInst.getKey());
                    GAction ga = selectMostInterestingAction(actInst.getValue());
                    actInst.getValue().clear();
                    actInst.getValue().add(ga);
                    if(!actionCount.containsKey(ga)) {
                        actionCount.put(ga, 1);
                        allGroundActions.add(ga);
                    } else {
                        actionCount.put(ga, actionCount.get(ga) + 1);
                    }
                }
            }

            alreadyUsed = allGroundActions;

            for(GAction ga : alreadyUsed) {
                causalPending.addAll(currentCausalReasoner.conditionsOf(ga));
            }



            while (!causalPending.isEmpty()) {
                Fluent og = causalPending.iterator().next();
                if (debugging)
                    System.out.println("    current subgoal: " + og);
                causalPending.remove(og);
                GAction ga = selectMostInterestingAction(currentCausalReasoner.candidatesFor(og));
                if (debugging)
                    System.out.println("    action for sub goal: " + ga);

                // if we need action (ga !=null) and we didn't already used it
                if (ga != null && !alreadyUsed.contains(ga)) {
                    // find if we can use the instantiation of an action already in the plan

                    causalPending.addAll(ga.pre);
                    if (!inPlanActions.contains(ga)) // TODO this ignores unmotivated actions
                        // deriv all actions that are not already in the plan
                        derivPending.addAll(derivReaas.conditionsOf(ga));

//                        System.out.println("2"+ga+" "+derivPending);
                    decompPending.addAll(decompReas.conditionsOf(ga));
                    alreadyUsed.add(ga);
                    assert !actionCount.containsKey(ga);
                    actionCount.put(ga, 1);
                }
            }
            int total = 0;
            for(Map.Entry<GAction, Integer> actionCountEntry : actionCount.entrySet()) {
                System.out.println(actionCountEntry.getValue() +" "+actionCountEntry.getKey());
                total += actionCountEntry.getValue();
            }
            System.out.println(total);
            System.out.println("not inst: "+(st.getAllActions().size() - instantiated.size()));
        } catch (NoSolutionException e) {
            return 99999;
        }

        return 0;
    }

    private int costOfAction(Action lifted, GAction ga) {
        if(ga == null)
            return 0;
        if(lifted == null)
            return costOfAction(ga);

        Set<GAction> liftedDom = actionInstantiations.get(lifted);
        if(liftedDom.contains(ga)) {
            return 0;
        } else {
            return costOfAction(ga);
        }
//        System.out.println(currentCausalReasoner.levelOfClause(ga) +" "+ derivReaas.levelOfClause(ga)+" "+ decompReas.levelOfClause(ga));
//        if(inPlanActions.contains(ga))
//            return 0;
//        if(debugging)
//            System.out.println(ga+": "+currentCausalReasoner.levelOfClause(ga) +" "+ derivReaas.levelOfClause(ga)+" "+ decompReas.levelOfClause(ga));
//        if(currentCausalReasoner.levelOfClause(ga) <0 || derivReaas.levelOfClause(ga)<0 || decompReas.levelOfClause(ga)<0)
//            return -1; // impossible in at least one
//        return currentCausalReasoner.levelOfClause(ga) +
//                derivReaas.levelOfClause(ga) -1 +
//                decompReas.levelOfClause(ga) -1;
    }

}
