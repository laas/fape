package fape.core.planning.search.strategies.plans;

import fape.core.planning.grounding.*;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.RelaxedPlanExtractor;
import fape.core.planning.planninggraph.RelaxedPlanningGraph;
import fape.core.planning.search.flaws.finders.AllThreatFinder;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;

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
            int numFlaws = st.tdb.getConsumers().size() + st.getNumOpenLeaves();// + threatFinder.getFlaws(st, null).size();
            hc.put(st.mID, numAdditionalSteps(st));// + numFlaws);
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

    @Override
    public String reportOnState(State st) {
        return String.format("RPGComp\tg: %s, h: %s, num-add-steps: %s", g(st), h(st), h(st)-st.tdb.getConsumers().size() - st.getNumOpenLeaves()); //numAdditionalSteps(st));
    }

    HashMap<Integer, Integer> hc = new HashMap<>();

    public int eval(State st) {
        return (int) g(st) + (int) h(st);
    }

    public int numAdditionalSteps(State st) {
        RelaxedPlanExtractor rpe = new RelaxedPlanExtractor(planner, st);
        return rpe.myPerfectHeuristic();
    }

    @Override
    public int compare(State state, State t1) {
        return  -(eval(t1) - eval(state));
    }
}
