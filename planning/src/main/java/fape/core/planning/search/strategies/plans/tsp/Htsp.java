package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.Preprocessor;
import fape.core.planning.search.strategies.plans.Heuristic;
import fape.core.planning.search.strategies.plans.PartialPlanComparator;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import planstack.anml.model.LStatementRef;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.statements.Assignment;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Persistence;

import static fape.core.planning.grounding.GAction.*;
import static fape.core.planning.search.strategies.plans.tsp.GoalNetwork.DisjunctiveGoal;

import java.util.*;
import java.util.stream.Collectors;

public class Htsp implements PartialPlanComparator, Heuristic {

    public enum DistanceEvaluationMethod {dtg, cea}

    private static int dbgLvl = 0;
    static void log1(String s) { if(dbgLvl>=1) System.out.println(s); }
    static void log2(String s) { if(dbgLvl>=2) System.out.println(s); }
    static void log3(String s) { if(dbgLvl>=3) System.out.println(s); }
    static void log4(String s) { if(dbgLvl>=4) System.out.println(s); }

    final TSPRoutePlanner routePlanner;

    public Htsp(DistanceEvaluationMethod method) {
        if(method == DistanceEvaluationMethod.dtg)
            routePlanner = new DTGRoutePlanner();
        else
            throw new FAPEException("Unsupported distance evaluation method for Htsp: "+method);
    }


    public Map<Integer,Integer> makespans = new HashMap<>();
    public Map<Integer,Integer> additionalCosts = new HashMap<>();
    public Map<Integer,Integer> existingCosts = new HashMap<>();

    @Override
    public String shortName() {
        return "tsp";
    }

    @Override
    public String reportOnState(State st) {
        return "g: "+g(st)+"hc: "+hc(st)+"  makespan: "+makespans.get(st.mID);
    }
    @Override
    public int compare(State s1, State s2) {
        return (int) (f(s1) - f(s2));
    }

    @Override
    public float g(State st) { hc(st); return existingCosts.get(st.mID); }

    @Override
    public float h(State st) { return hc(st); }

    private Pair<GLogStatement, DisjunctiveGoal> best(Collection<Pair<GLogStatement, DisjunctiveGoal>> candidates) {
        if (candidates.stream().anyMatch(x -> x.value1 instanceof GPersistence))
            return candidates.stream().filter(x -> x.value1 instanceof GPersistence).findFirst().get();

        if (candidates.stream().anyMatch(x -> x.value1 instanceof GTransition))
            return candidates.stream().filter(x -> x.value1 instanceof GTransition).findFirst().get();

        return candidates.stream().findFirst().get();
    }

    @Override
    public float hc(State st) {
        if(additionalCosts.containsKey(st.mID))
            return additionalCosts.get(st.mID);

        Preprocessor pp = st.pl.preprocessor;
        GoalNetwork gn = goalNetwork(st);
        PartialState ps = new PartialState();

        int additionalCost = 0;
        int existingCost = 0;

        while(!gn.isEmpty()) {
            List<Pair<GLogStatement, DisjunctiveGoal>> sat = gn.satisfiable(ps);
            log2("Satisfiable: "+sat.stream().map(x -> x.value1).collect(Collectors.toList()));

            if(!sat.isEmpty()) {
                Pair<GLogStatement, DisjunctiveGoal> p = best(sat);
                ps.progress(p.value1, p.value2);
                if(!(p.value1 instanceof GPersistence))
                    existingCost++;
                gn.setAchieved(p.value2, p.value1);
            } else { // expand with dijkstra
                // defines a set of target fluents; We can stop whn one of those is reached
                Set<Fluent> targets = gn.getActiveGoals().stream()
                        .flatMap(g -> g.getGoals().stream())
                        .map(s -> {
                            if(s instanceof GPersistence)
                                return pp.getFluent(s.sv, ((GPersistence)s).value);
                            else
                                return pp.getFluent(s.sv, ((GTransition)s).from);
                        }).collect(Collectors.toSet());

                TSPRoutePlanner.Result plan = routePlanner.getPlan(targets, ps, st);
                if(plan != null) {
                    additionalCost += plan.getCost();
                    plan.getTransformation().accept(ps);
                } else {
                    // was not able to find a plan, put a very high cost
                    additionalCost = 99999;
                    break;
                }
            }
        }
        int makespan = ps.labels.values().stream().map(list -> list.getLast()).map(lbl -> lbl.getUntil()).max(Integer::compare).get();
        log1("State: "+st.mID+"  cost: "+additionalCost);
        log1(" makespan: "+makespan);
        for(GStateVariable sv : ps.labels.keySet()) {
            String res = "  "+sv.toString()+" ";
            for(PartialState.Label lbl : ps.labels.get(sv))
                if(lbl.getVal() != null)
                    res += String.format("  [%d,%d] %s", lbl.getSince(), lbl.getUntil(), lbl.getVal());
            log1(res);
        }

        additionalCosts.put(st.mID, additionalCost);
        existingCosts.put(st.mID, existingCost);
        makespans.put(st.mID, makespan);

        log1("");

        return additionalCost;
    }


    public static GoalNetwork goalNetwork(State st) {
        APlanner planner = st.pl;

        GoalNetwork gn = new GoalNetwork();

        for (Timeline tl : st.getTimelines()) {
            // ordered goals that will be extracted from this timeline
            GoalNetwork.DisjunctiveGoal[] goals;

            if(tl.hasSinglePersistence()) {
                goals = new GoalNetwork.DisjunctiveGoal[1];

                LogStatement s = tl.getChainComponent(0).getFirst();
                assert s instanceof Persistence;
                Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.endValue(), st, planner);

                Set<GLogStatement> persistences = fluents.stream()
                        .map(f -> new GAction.GPersistence(f.sv, f.value, st.csp.stn().getMinDelay(s.start(),s.end())))
                        .collect(Collectors.toSet());
                goals[0] = new GoalNetwork.DisjunctiveGoal(persistences, s.start(), s.end());

            } else {
                goals = new GoalNetwork.DisjunctiveGoal[tl.numChanges()];
                for (int i = 0; i < tl.numChanges(); i++) {
                    LogStatement s = tl.getChangeNumber(i).getFirst();

                    // action by which this statement was introduced (null if no action)
                    Action containingAction = st.getActionContaining(s);

                    if (containingAction == null) { // statement was not added as part of an action
                        assert s instanceof Assignment;
                        assert s.endValue() instanceof InstanceRef;
                        assert i == 0;
                        Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.endValue(), st, planner);

                        Set<GLogStatement> assignments = fluents.stream()
                                .map(f -> new GAction.GAssignment(f.sv, f.value, st.csp.stn().getMinDelay(s.start(), s.end())))
                                .collect(Collectors.toSet());
                        goals[i] = new GoalNetwork.DisjunctiveGoal(assignments, s.start(), s.end());

                    } else { // statement was added as part of an action or a decomposition
                        Collection<GAction> acts = st.getGroundActions(containingAction);

                        // local reference of the statement, used to extract the corresponding ground statement from the GAction
                        assert containingAction.context().contains(s);
                        LStatementRef statementRef = containingAction.context().getRefOfStatement(s);

                        Set<GLogStatement> statements = acts.stream()
                                .map(ga -> ga.statementWithRef(statementRef))
                                .collect(Collectors.toSet());
                        goals[i] = new GoalNetwork.DisjunctiveGoal(statements, s.start(), s.end());
                    }
                }
            }

            Iterable<DisjunctiveGoal> previousGoals = gn.getAllGoals();

            for(int i=0 ; i<goals.length ; i++) {
                goals[i].setEarliest(st.getEarliestStartTime(goals[i].getStart()));
                if(i>0)
                    gn.addGoal(goals[i], goals[i-1]);
                else
                    gn.addGoal(goals[i], null);

                DisjunctiveGoal cur = goals[i];
                for(DisjunctiveGoal old : previousGoals) {
                    if(!st.canBeBefore(cur.start, old.end))
                        gn.addPrecedence(old, cur);
                    else if(!st.canBeBefore(old.start, cur.end))
                        gn.addPrecedence(cur, old);
                }
            }
        }
        return gn;
    }
}
