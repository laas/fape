package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.anml.model.concrete.statements.LogStatement;
import fr.laas.fape.planning.core.planning.grounding.Fluent;
import fr.laas.fape.planning.core.planning.grounding.GAction.*;
import fr.laas.fape.planning.core.planning.grounding.GStateVariable;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.preprocessing.Preprocessor;
import fr.laas.fape.planning.core.planning.preprocessing.dtg.TemporalDTG;
import fr.laas.fape.planning.core.planning.search.strategies.plans.PartialPlanComparator;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import fr.laas.fape.planning.exceptions.FAPEException;
import fr.laas.fape.planning.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class Htsp extends PartialPlanComparator {

    public enum DistanceEvaluationMethod {dtg, tdtg, cea}

    protected static int dbgLvl = 0;
    static void log1(String s) { assert dbgLvl>=1; System.out.println(s); }
    static void log2(String s) { assert dbgLvl>=2; System.out.println(s); }
    static void log3(String s) { assert dbgLvl>=3; System.out.println(s); }

    final TSPRoutePlanner routePlanner;
    Planner pl;

    public Htsp(DistanceEvaluationMethod method) {
        if(method == DistanceEvaluationMethod.dtg)
            routePlanner = new DTGRoutePlanner();
        else if(method == DistanceEvaluationMethod.tdtg)
            routePlanner = new TemporalDTGRoutePlanner();
        else
            throw new FAPEException("Unsupported distance evaluation method for Htsp: "+method);
    }


    private Map<Integer,Integer> makespans = new HashMap<>();
    private Map<Integer,Integer> additionalCosts = new HashMap<>();
    private Map<Integer,Integer> existingCosts = new HashMap<>();

    @Override
    public String shortName() {
        return "tsp";
    }

    @Override
    public String reportOnState(State st) {
        return "g: "+g(st)+"hc: "+hc(st)+"  makespan: "+makespans.get(st.mID);
    }

    @Override
    public double g(State st) { hc(st); return existingCosts.get(st.mID); }

    @Override
    public double h(State st) { return hc(st); }

    private Pair<GLogStatement, GoalNetwork.DisjunctiveGoal> best(Collection<Pair<GLogStatement, GoalNetwork.DisjunctiveGoal>> candidates) {
        if (candidates.stream().anyMatch(x -> x.value1 instanceof GPersistence))
            return candidates.stream().filter(x -> x.value1 instanceof GPersistence).findFirst().get();

        if (candidates.stream().anyMatch(x -> x.value1 instanceof GTransition))
            return candidates.stream().filter(x -> x.value1 instanceof GTransition).findFirst().get();

        return candidates.stream().findFirst().get();
    }

    private boolean firstTime = true;

//    @Override
    public double hc(State st) {

//        if(firstTime) {
//            for (GStateVariable sv : st.pl.preprocessor.getAllStateVariables()) {
//                TemporalDTG dtg = st.pl.preprocessor.getTemporalDTG(sv);
//                System.out.println(dtg);
//            }
//            firstTime = false;
//        }


        if(additionalCosts.containsKey(st.mID))
            return additionalCosts.get(st.mID);

        Preprocessor pp = st.pl.preprocessor;
        GoalNetwork gn = goalNetwork(st);
        PartialState ps = new PartialState(st.pl);

        int additionalCost = 0;
        int existingCost = 0;

        // reduce the goal network until it is empty
        while(!gn.isEmpty()) {
            List<Pair<GLogStatement, GoalNetwork.DisjunctiveGoal>> sat = gn.satisfiable(ps);
            if(dbgLvl >= 2)
                log2("Satisfiable: "+sat.stream().map(x -> x.value1).collect(Collectors.toList()));

            if(!sat.isEmpty()) {
                // at least one goal is achievable right away. Greedily select the best one and achieve it.
                Pair<GLogStatement, GoalNetwork.DisjunctiveGoal> p = best(sat);
                ps.progress(p.value1, p.value2);
                if(p.value1.isChange())
                    existingCost++;
                gn.setAchieved(p.value2, p.value1);
            } else { // expand with dijkstra
                // defines a set of target fluents; We can stop when one of those is reached
                Set<Fluent> targets = gn.getActiveGoals().stream()
                        .flatMap(g -> g.getGoals().stream())
                        .map(s -> pp.getFluent(s.sv, s.startValue()))
                        .collect(Collectors.toSet());

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
        if(dbgLvl >= 1) {
            log1("\nState: " + st.mID + "  cost: " + additionalCost);
            log1(" makespan: " + makespan);
            for (GStateVariable sv : ps.labels.keySet()) {
                String res = "  " + sv.toString() + " ";
                for (PartialState.Label lbl : ps.labels.get(sv))
                    if (!lbl.isUndefined())
                        res += String.format("  [%d,%d] %s", lbl.getSince(), lbl.getUntil(), lbl.getVal());
                log1(res);
            }
            log1("\n\n");
        }

        additionalCosts.put(st.mID, additionalCost);
        existingCosts.put(st.mID, existingCost);
        makespans.put(st.mID, makespan);

        return additionalCost;
    }

    private int distPerSV(TemporalDTG.Node n, GoalNetwork gn, GoalNetwork.DisjunctiveGoal fluentOrigin) {
        GStateVariable sv = n.getStateVariable();
        TemporalDTG dtg = pl.preprocessor.getTemporalDTG(sv);

        int maxDist = -1;
        for(GoalNetwork.DisjunctiveGoal dg : gn.getAllGoals()) {
            if(dg == fluentOrigin)
                continue;

            int minDist = dg.getGoals().stream()
                    .filter(s -> s.getStateVariable() == sv)
                    .map(s -> {
                        if(s.isAssignement())
                            return 0;
                        else
                            return n.getMinDelayTo(dtg.getBaseNode(s.startValue())); //TODO: the baseNode needs more evolved handling
                    })
                    .min(Integer::compare)
                    .orElse(0);
            assert minDist >= 0;
            maxDist = Math.max(maxDist, minDist);
        }
        return maxDist;
    }

    public float newhc(State st) {
        if(additionalCosts.containsKey(st.mID))
            return additionalCosts.get(st.mID);

        pl = st.pl;
        Preprocessor pp = st.pl.preprocessor;
        GoalNetwork gn = goalNetwork(st);
        PartialState ps = new PartialState(st.pl);

        int additionalCost = 0;
        int existingCost = 0;

        // reduce the goal network until it is empty
        while(!gn.isEmpty()) {
            Collection<GoalNetwork.DisjunctiveGoal> actives = gn.getActiveGoals();

            int bestG = Integer.MAX_VALUE/2-1;
            int bestH = Integer.MAX_VALUE/2-1;
            GLogStatement bestStatement = null;
            GoalNetwork.DisjunctiveGoal bestGoalSet = null;

            for(GoalNetwork.DisjunctiveGoal goalSet : actives) {
                for(GLogStatement cur : goalSet.getGoals()) {
                    TemporalDTG dtg = pp.getTemporalDTG(cur.sv);
                    int g = ps.latestLabel(cur.sv).getUntil(); //TODO: use until when on assignments/changes
                    if(cur.isAssignement()) {
                        g += 0;
                    } else if(ps.latestLabel(cur.sv).isUndefined()) {
                        // g is the minimal duration on incoming assignments
                        g += dtg.getBaseNode(cur.startValue()).inChanges().stream()
                                .filter(ch -> !ch.isTransition())
                                .map(ch -> ch.getDuration())
                                .min(Integer::compare)
                                .orElse(Integer.MAX_VALUE/2-1);
                    } else  {
                        // dist from state to statement start
                        TemporalDTG.Node startVal = dtg.getBaseNode(cur.startValue());
                        g += ps.latestLabel(cur.sv).getNode().getMinDelayTo(startVal);
                    };
                    int h = distPerSV(dtg.getBaseNode(cur.endValue()), gn, goalSet);
//                    h = 0;

                    if(g+h < bestG+bestH) {
                        bestG = g;
                        bestH = h;
                        bestStatement = cur;
                        bestGoalSet = goalSet;
                    }
                }
            }
            if(bestStatement != null) {
                existingCost++;
                if(!bestStatement.isAssignement()
                        && !ps.latestLabel(bestStatement.sv).isUndefined()
                        && ps.latestLabel(bestStatement.sv).getVal().equals(bestStatement.startValue()))
                    additionalCost++; // at least one transition
                ps.setValue(bestStatement.sv, bestStatement.endValue(), bestG, bestStatement.minDuration);
                gn.setAchieved(bestGoalSet, bestStatement);
//                System.out.println(bestStatement);
//                System.out.println("  g: "+bestG+"  h: "+bestH);
            } else {
                additionalCost = 999999;
                break;
            }
        }
        int makespan = ps.labels.values().stream()
                .map(list -> list.getLast())
                .map(lbl -> lbl.getUntil())
                .max(Integer::compare).get();

        if(dbgLvl >= 1) {
            log1("\nState: " + st.mID + "  cost: " + additionalCost);
            log1(" makespan: " + makespan);
            for (GStateVariable sv : ps.labels.keySet()) {
                String res = "  " + sv.toString() + " ";
                for (PartialState.Label lbl : ps.labels.get(sv))
                    if (!lbl.isUndefined())
                        res += String.format("  [%d,%d] %s", lbl.getSince(), lbl.getUntil(), lbl.getVal());
                log1(res);
            }
            log1("\n\n");
        }

        additionalCosts.put(st.mID, additionalCost);
        existingCosts.put(st.mID, existingCost);
        makespans.put(st.mID, makespan);

        return additionalCost;
    }

    public static GoalNetwork goalNetwork(State st) {
        GoalNetwork gn = new GoalNetwork();

        for (Timeline tl : st.getTimelines()) {
            // ordered goals that will be extracted from this timeline
            GoalNetwork.DisjunctiveGoal[] goals;

            if(tl.hasSinglePersistence()) {
                goals = new GoalNetwork.DisjunctiveGoal[1];
                LogStatement s = tl.getChainComponent(0).getFirst();
                Set<GLogStatement> persistences = st.getGroundStatements(s);
                goals[0] = new GoalNetwork.DisjunctiveGoal(persistences, s.start(), s.end());
            } else {
                goals = new GoalNetwork.DisjunctiveGoal[tl.numChanges()];
                for (int i = 0; i < tl.numChanges(); i++) {
                    LogStatement s = tl.getChangeNumber(i).getFirst();
                    Set<GLogStatement> gStatements = st.getGroundStatements(s);
                    goals[i] = new GoalNetwork.DisjunctiveGoal(gStatements, s.start(), s.end());
                }
            }

            Iterable<GoalNetwork.DisjunctiveGoal> previousGoals = gn.getAllGoals();

            for(int i=0 ; i<goals.length ; i++) {
                goals[i].setEarliest(st.getEarliestStartTime(goals[i].getStart()));
                if(i>0)
                    gn.addGoal(goals[i], goals[i-1]);
                else
                    gn.addGoal(goals[i], null);

                GoalNetwork.DisjunctiveGoal cur = goals[i];
                for(GoalNetwork.DisjunctiveGoal old : previousGoals) {
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
