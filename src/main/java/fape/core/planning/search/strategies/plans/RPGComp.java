package fape.core.planning.search.strategies.plans;

import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.DisjunctiveFluent;
import fape.core.planning.planninggraph.GAction;
import fape.core.planning.planninggraph.GroundProblem;
import fape.core.planning.planninggraph.RelaxedPlanningGraph;
import fape.core.planning.search.flaws.finders.AllThreatFinder;
import fape.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fape.core.planning.search.strategies.flaws.RPGOpenGoalComp;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;

import java.util.*;

public class RPGComp implements PartialPlanComparator {

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

    HashMap<State, Integer> f = new HashMap<>();

    public int eval(State st) {
        if(!f.containsKey(st)) {
            int val = evaluate(st);
            f.put(st, val);
        }
        return f.get(st);
    }

    public static int evaluate(State st) {
        if(gpb == null || gpb.liftedPb != st.pb) {
            gpb = new GroundProblem(st.pb);
        }

        List<OpenGoal> openGoals = new LinkedList<>();
        for(Timeline tl : st.consumers)
            openGoals.add(new OpenGoal(tl, st.getEarliestStartTime(tl.getConsumeTimePoint())));

        Collections.sort(openGoals);

        Set<GAction> relaxedPlan = new HashSet<>();
        for(OpenGoal og : openGoals) {
            Timeline tl = og.tl;
            DisjunctiveFluent df = new DisjunctiveFluent(DisjunctiveFluent.fluentsOf(tl.stateVariable, tl.getGlobalConsumeValue(), st, true));
            GroundProblem pb = new GroundProblem(gpb, st, tl); // problem until the open goal tl
            RelaxedPlanningGraph rpg = new RelaxedPlanningGraph(pb);
            rpg.buildUntil(df);
            relaxedPlan = rpg.buildRelaxedPlan(df, relaxedPlan);
            if(relaxedPlan == null)
                break;
        }

        if(relaxedPlan == null)
            return 9999999;

        int numFlaws = st.consumers.size() + st.getNumOpenLeaves() + threatFinder.getFlaws(st, null).size();
        int numActions = st.getNumActions();
        int numAdditionalSteps = relaxedPlan.size();
        return numActions + numFlaws + numAdditionalSteps;
    }

    @Override
    public int compare(State state, State t1) {
        return  -(eval(t1) - eval(state));
    }
}
