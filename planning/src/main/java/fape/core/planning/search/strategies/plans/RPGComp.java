package fape.core.planning.search.strategies.plans;

import fape.core.planning.grounding.GroundProblem;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.RelaxedPlanExtractor;
import fape.core.planning.search.flaws.finders.AllThreatFinder;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;

import java.util.HashMap;

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
            int numFlaws = st.tdb.getConsumers().size();// + threatFinder.getFlaws(st, null).size();
            hc.put(st.mID, numAdditionalSteps(st));// + numFlaws);
        }
        return hc.get(st.mID);
    }

    @Override
    public String shortName() {
        return "rplan";
    }

    @Override
    public String reportOnState(State st) {
        return String.format("RPGComp\tg: %s, h: %s, num-add-steps: %s", g(st), h(st), h(st)-st.tdb.getConsumers().size()); //numAdditionalSteps(st));
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
