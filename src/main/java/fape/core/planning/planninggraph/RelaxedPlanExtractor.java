package fape.core.planning.planninggraph;

import fape.core.inference.HLeveledReasoner;
import fape.core.planning.grounding.*;
import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.FAPEException;
import fape.exceptions.NoSolutionException;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionCondition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RelaxedPlanExtractor {

    final APlanner planner;
    final HLeveledReasoner<GAction,GTaskCond> decompReas;
    final HLeveledReasoner<GAction,GTaskCond> derivReaas;
    final HLeveledReasoner<GAction,Fluent> baseCausalReas;
    final State st;
    final Set<GAction> allowedActions;
    final Set<GAction> inPlanActions;

    public RelaxedPlanExtractor(APlanner planner, State st) {
        this.planner = planner;
        this.st = st;
        allowedActions = new HashSet<>(planner.reachability.getAllActions(st));
        inPlanActions = new HashSet<>();
        for(Action a : st.getAllActions()) {
            for(GAction ga : planner.reachability.groundedVersions(a, st))
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

        for(GTaskCond tc : planner.reachability.getDerivableTasks(st)) {
            baseHLR.set(tc);
        }
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

    private int costOfAction(GAction ga, HLeveledReasoner<GAction,Fluent> causalReas) {
        assert ga != null;
//        System.out.println(causalReas.levelOfClause(ga) +" "+ derivReaas.levelOfClause(ga)+" "+ decompReas.levelOfClause(ga));
        if(causalReas.levelOfClause(ga) <0 || derivReaas.levelOfClause(ga)<0 || decompReas.levelOfClause(ga)<0)
            return -1; // impossible in at least one
        return causalReas.levelOfClause(ga) +
                derivReaas.levelOfClause(ga) -1 +
                decompReas.levelOfClause(ga) -1;
    }

    private int costOfTask(GTaskCond t, HLeveledReasoner<GAction,Fluent> causalReas) {
        assert t != null;
        if(!derivReaas.knowsFact(t))
            return -1;
        if(!decompReas.knowsFact(t))
            return -1;
        if(derivReaas.levelOfFact(t) == -1 || decompReas.levelOfFact(t) == -1)
            return -1;
        return derivReaas.levelOfFact(t) + decompReas.levelOfFact(t);
    }

    public Fluent selectMostInterestingFluent(Collection<Fluent> fluents, HLeveledReasoner<GAction,Fluent> causalReas) throws NoSolutionException {
        Fluent bestFluent = null;
        int bestFluentCost = Integer.MAX_VALUE;
        for(Fluent f : fluents) {
            if(causalReas.knowsFact(f) && causalReas.levelOfFact(f) != -1) {
                int bestEnablerCost = Integer.MAX_VALUE;
                for(GAction ga : causalReas.candidatesFor(f)) {
                    if(ga == null)
                        return f; // init fact, we can not do better
                    assert allowedActions.contains(ga);
                    int cost = costOfAction(ga, causalReas);
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

    public GAction selectMostInterestingAction(Collection<GAction> actions, HLeveledReasoner<GAction,Fluent> causalReas) throws NoSolutionException {
        GAction bestAction = null;
        int bestActionCost = Integer.MAX_VALUE;
        for(GAction ga : actions) {
            if(ga == null)
                return null; // init fact, we can not do better
            int cost = costOfAction(ga, causalReas);
            if(cost >= 0 && cost < bestActionCost) {
                bestActionCost = cost;
                bestAction = ga;
            }
        }
        if(bestAction != null)
            return bestAction;
        else if(planner.breakIfNoSolution) {
            throw new NoSolutionException();
        } else
            throw new NoSolutionException();
    }

    public GTaskCond selectMostInterestingTask(Collection<GTaskCond> tasks, HLeveledReasoner<GAction,Fluent> causalReas) {
        GTaskCond best = null;
        int bestCost = Integer.MAX_VALUE;
        for(GTaskCond t : tasks) {
            int cost = costOfTask(t, causalReas);
            if(cost != -1 && cost < bestCost) {
                best = t;
                bestCost = cost;
            }
        }
        if(best == null)
            throw new FAPEException("OUPSI");
        return best;
    }

    public int numAdditionalStepWithReasoner() {
        try {
            Collection<GAction> alreadyUsed = new HashSet<>();

            Set<GTaskCond> derivPending = new HashSet<>();
            Set<GTaskCond> decompPending = new HashSet<>();


            for (Timeline tl : st.tdb.getConsumers()) {
                HLeveledReasoner<GAction, Fluent> hlr = causalReasonerForOpenGoal(st, tl);

                Set<Fluent> causalPending = new HashSet<>();

                Collection<Fluent> goals = DisjunctiveFluent.fluentsOf(tl.stateVariable, tl.getGlobalConsumeValue(), st, true);
                causalPending.add(selectMostInterestingFluent(goals, hlr));

                while (!causalPending.isEmpty()) {
                    Fluent og = causalPending.iterator().next();
                    causalPending.remove(og);
                    GAction ga = selectMostInterestingAction(hlr.candidatesFor(og), hlr);

                    // if we need action (ga !=null) and we didn't already used it
                    if(ga != null && !alreadyUsed.contains(ga)) {
                        causalPending.addAll(ga.pre);
                        derivPending.addAll(derivReaas.conditionsOf(ga));
                        decompPending.addAll(decompReas.conditionsOf(ga));
                        alreadyUsed.add(ga);
                    }
                }

            }

            Set<Fluent> causalPending = new HashSet<>();
            HLeveledReasoner<GAction, Fluent> hlr = baseCausalReas.clone();
            Collection<Fluent> init = GroundProblem.allFluents(st); //GroundProblem.fluentsBefore(st, consumer.getFirstTimePoints());
            for (Fluent i : init) {
                hlr.set(i);
            }
            hlr.infer();
            for(ActionCondition liftedTask : st.getOpenTaskConditions()) {
                Collection<GTaskCond> tasks = planner.reachability.getGroundedTasks(liftedTask, st);
                decompPending.add(selectMostInterestingTask(tasks, hlr));
            }

            while(!causalPending.isEmpty() || !derivPending.isEmpty() || !decompPending.isEmpty()) {
                GAction ga = null;
                if(!causalPending.isEmpty()) {
                    Fluent og = causalPending.iterator().next();
                    causalPending.remove(og);
                    ga = selectMostInterestingAction(hlr.candidatesFor(og), hlr);
                } else if(!derivPending.isEmpty()) {
                    GTaskCond task = derivPending.iterator().next();
                    derivPending.remove(task);
                    ga = selectMostInterestingAction(derivReaas.candidatesFor(task), hlr);
                } else {
                    assert !decompPending.isEmpty();
                    GTaskCond task = decompPending.iterator().next();
                    decompPending.remove(task);
                    ga = selectMostInterestingAction(decompReas.candidatesFor(task), hlr);
                }
                // if we need action (ga !=null) and we didn't already used it
                if(ga != null && !alreadyUsed.contains(ga)) {
                    causalPending.addAll(ga.pre);
                    derivPending.addAll(derivReaas.conditionsOf(ga));
                    decompPending.addAll(decompReas.conditionsOf(ga));
                    alreadyUsed.add(ga);
                }
            }


            return alreadyUsed.size();
        } catch (NoSolutionException e) {
            if (planner.breakIfNoSolution) {
                e.printStackTrace();
                System.out.println("BREAK");
            }
            return 9999999;
        }
    }
}
