package fape.core.planning.search.strategies.plans;

import fape.core.inference.HLeveledReasoner;
import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GroundProblem;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.RelaxedPlanningGraph;
import fape.core.planning.search.flaws.finders.AllThreatFinder;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.NoSolutionException;

import java.util.*;

public class RPGComp implements PartialPlanComparator, Heuristic {

    final APlanner planner;

    public RPGComp(APlanner planner) {
        this.planner = planner;
    }

    @Override
    public float g(State st) {
        return st.getNumActions();
    }

    @Override
    public float h(State st) {
        return hc(st);
    }

    @Override
    public float hc(State st) {
        if(!hc.containsKey(st.mID)) {
            int numFlaws = st.tdb.getConsumers().size() + st.getNumOpenLeaves() + threatFinder.getFlaws(st, null).size();
            hc.put(st.mID, numAdditionalSteps(st) + numFlaws);
        }
        return hc.get(st.mID);
    }

    static class OpenGoal implements Comparable<OpenGoal> {
        public final Timeline tl;
        public final int earliestStart;

        public OpenGoal(Timeline tl, int earliestStart) {
            this.tl = tl;
            this.earliestStart = earliestStart;
        }
        @Override
        public int compareTo(OpenGoal openGoal) {
            return this.earliestStart - openGoal.earliestStart;
        }
    }

    public static GroundProblem gpb = null;
    private static final AllThreatFinder threatFinder = new AllThreatFinder();

    @Override
    public String shortName() {
        return "rplan";
    }

    HashMap<Integer, Integer> hc = new HashMap<>();

    public int eval(State st) {
        return (int) g(st) + (int) h(st);
    }

    public int numAdditionalSteps(State st) {
        if(gpb == null || gpb.liftedPb != st.pb) {
            if(planner.reachability != null) {
                assert planner.reachability.base.liftedPb == st.pb;
                gpb = planner.reachability.base;
            } else
                gpb = new GroundProblem(st.pb);
        }

        List<OpenGoal> openGoals = new LinkedList<>();
        for(Timeline tl : st.tdb.getConsumers())
            openGoals.add(new OpenGoal(tl, st.getEarliestStartTime(tl.getConsumeTimePoint())));

        Collections.sort(openGoals);

        Set<GAction> relaxedPlan = new HashSet<>();
        for(OpenGoal og : openGoals) {
            Timeline tl = og.tl;
            DisjunctiveFluent df = new DisjunctiveFluent(DisjunctiveFluent.fluentsOf(tl.stateVariable, tl.getGlobalConsumeValue(), st, true));
            GroundProblem pb = new GroundProblem(gpb, st, tl); // problem until the open goal tl
            RelaxedPlanningGraph rpg;
            if(planner.options.usePlanningGraphReachability)
                rpg = new RelaxedPlanningGraph(pb, planner.reachability.getAllActions(st));
            else
                rpg = new RelaxedPlanningGraph(pb);
            rpg.buildUntil(df);
            relaxedPlan = rpg.buildRelaxedPlan(df, relaxedPlan);
            if(relaxedPlan == null)
                break;
        }
//        int reasoned = numAdditionalStepWithReasoner(st);

        if(relaxedPlan == null) {
//            System.out.println("inf "+reasoned);
            return 9999999;
        }


//        System.out.println(relaxedPlan.size()+"  "+reasoned);
        return relaxedPlan.size();
    }

    public int numAdditionalStepWithReasoner(State st) {
        try {

            HLeveledReasoner<GAction, Fluent> baseHLR = new HLeveledReasoner<>();
            for (GAction ga : planner.reachability.getAllActions(st)) {
                baseHLR.addClause(ga.pre, ga.add, ga);
            }

            Collection<GAction> alreadyUsed = new HashSet<>();

            for (Timeline tl : st.tdb.getConsumers()) {
                HLeveledReasoner<GAction, Fluent> hlr = baseHLR.clone();

                Collection<Fluent> goals = DisjunctiveFluent.fluentsOf(tl.stateVariable, tl.getGlobalConsumeValue(), st, true);
                Collection<Fluent> init = GroundProblem.fluentsBefore(st, tl.getFirstTimePoints());
                for (Fluent i : init) {
                    hlr.set(i);
                }
                hlr.infer();
                alreadyUsed = hlr.getStepsToAnyOf(goals, alreadyUsed);
//                System.out.println(Printer.statement(st, tl.get(0).getFirst()) + " " + alreadyUsed);
            }
//            System.out.println();

            return alreadyUsed.size();
        } catch (NoSolutionException e) {
            return 9999999;
        }
    }

    @Override
    public int compare(State state, State t1) {
        return  -(eval(t1) - eval(state));
    }
}
