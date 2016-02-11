package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.heuristics.temporal.DependencyGraph;
import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.Preprocessor;
import fape.core.planning.search.strategies.plans.Heuristic;
import fape.core.planning.search.strategies.plans.PartialPlanComparator;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.util.Pair;
import fr.laas.fape.structures.DijkstraQueue;
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

    private static void log(String s) {
        if(true) System.out.println(s);
    }

    @Override
    public String shortName() {
        return "tsp";
    }

    @Override
    public String reportOnState(State st) {
        return " ";
    }

    @Override
    public int compare(State state, State t1) {
        return 0;
    }

    @Override
    public float g(State st) { return 0; }

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
        Preprocessor pp = st.pl.preprocessor;
        GoalNetwork gn = goalNetwork(st);
        PartialState ps = new PartialState();
        Map<GStateVariable, String> res = new HashMap<>();

        while(!gn.isEmpty()) {
            List<Pair<GLogStatement, DisjunctiveGoal>> sat = gn.satisfiable(ps);
            log("Satisfiable: "+sat.stream().map(x -> x.value1).collect(Collectors.toList()));

            if(!sat.isEmpty()) {
                Pair<GLogStatement, DisjunctiveGoal> p = best(sat);
                ps.progress(p.value1);
                gn.setAchieved(p.value2, p.value1);
                String base = res.getOrDefault(p.value1.sv, p.value1.sv.toString());
                res.put(p.value1.sv, base +"   "+ p.value1);
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

                DijkstraQueue<Fluent> q = new DijkstraQueue<>(pp.store.getIntRep(Fluent.class));
                Map<Fluent, GLogStatement> predecessors = new HashMap<>();

                for(GStateVariable sv : pp.store.getInstances(GStateVariable.class)) {
                    int baseTime = -1;
                    if(ps.labels.containsKey(sv)) {
                        PartialState.Label l = ps.labels.get(sv);
                        q.insert(pp.getFluent(sv, l.getVal()), l.getUntil());
                        baseTime = l.getUntil();
                    }

                    for(GAssignment ass : pp.getDTG(sv).unconditionalTransitions) {
                        Fluent f = pp.getFluent(sv, ass.to);
                        if(!q.contains(f)) {
                            q.insert(f, baseTime + 1);
                            predecessors.put(f, ass);
                        }
                    }
                }
                log("  Targets: "+targets);
                Fluent sol = null;
                while(!q.isEmpty() && sol == null) {
                    Fluent cur = q.poll();
                    log("  dij current: "+cur+"  "+q.getCost(cur));
                    if(targets.contains(cur)) {
                        sol = cur;
                    } else {
                        DTG dtg = pp.getDTG(cur.sv);
                        for(GTransition trans : dtg.outGoingTransitions.get(cur.value)) {
                            assert cur == pp.getFluent(trans.sv, trans.from);
                            Fluent succ = pp.getFluent(trans.sv, trans.to);
                            if(!q.hasCost(succ)) { // never inserted
                                q.insert(succ, q.getCost(cur)+1);
                                predecessors.put(succ, trans);
                            } else if(q.getCost(succ) > q.getCost(cur)+1) {
                                q.update(succ, q.getCost(cur)+1);
                                predecessors.put(succ, trans);
                            }
                        }
                    }
                }
                if(sol != null) {
                    ps.labels.put(sol.sv, new PartialState.Label(sol.value, q.getCost(sol), q.getCost(sol)));
                    Fluent cur = sol;
                    LinkedList<GLogStatement> preds = new LinkedList<>();
                    while (cur != null) { // extract predecessor list
                        if(!predecessors.containsKey(cur))
                            cur = null;
                        else {
                            preds.addFirst(predecessors.get(cur));
                            if(predecessors.get(cur) instanceof GTransition)
                                cur = pp.getFluent(cur.sv, ((GTransition) predecessors.get(cur)).from);
                            else
                                cur = null;
                        }
                    }
                    String base = res.getOrDefault(sol.sv, sol.sv+" ");
                    res.put(sol.sv, base + "  "+preds+"  ");
                    log("Dij choice: "+sol);
                } else {
                    log("DEAD-END!!!!");
                    break;
                }
            }


        }
        for(GStateVariable sv : res.keySet())
            log(sv+ res.get(sv).replaceAll("at\\(r1\\)",""));

        return 0;
    }


    public static GoalNetwork goalNetwork(State st) {
        APlanner planner = st.pl;

        GoalNetwork gn = new GoalNetwork();

        //TODO: (1) add temporal constraints

        for (Timeline tl : st.getTimelines()) {
            // ordered goals that will be extracted from this timeline
            GoalNetwork.DisjunctiveGoal[] goals;

            if(tl.hasSinglePersistence()) {
                goals = new GoalNetwork.DisjunctiveGoal[1];

                LogStatement s = tl.getChainComponent(0).getFirst();
                assert s instanceof Persistence;
                Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.endValue(), st, planner);

                Set<GLogStatement> persistences = fluents.stream()
                        .map(f -> new GAction.GPersistence(f.sv, f.value))
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
                                .map(f -> new GAction.GAssignment(f.sv, f.value))
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
