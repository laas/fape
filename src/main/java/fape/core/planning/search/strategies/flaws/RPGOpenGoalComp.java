package fape.core.planning.search.strategies.flaws;

import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.GroundProblem;
import fape.core.planning.planninggraph.RelaxedPlanningGraph;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fape.core.planning.states.State;

import java.util.HashMap;
import java.util.Map;

public class RPGOpenGoalComp implements FlawComparator {

    final State st;
    final Map<UnsupportedTimeline, Integer> depths = new HashMap<>();

    public static GroundProblem gpb = null;

    public RPGOpenGoalComp(State st) {
        this.st = st;

        if(gpb == null || gpb.liftedPb != st.pb) {
            gpb = new GroundProblem(st.pb);
        }
    }

    @Override
    public String shortName() {
        return "rpgog";
    }

    public int evaluate(UnsupportedTimeline udb) {
        if(!depths.containsKey(udb)) {
            GroundProblem pb = new GroundProblem(gpb, st, udb.consumer);
            RelaxedPlanningGraph rpg = new RelaxedPlanningGraph(pb);
            int depth = rpg.buildUntil(new DisjunctiveFluent(udb.consumer.stateVariable, udb.consumer.getGlobalConsumeValue(), st));
            depths.put(udb, depth);
//            System.out.println(""+depth+"   " +Printer.inlineTemporalDatabase(st, udb.consumer));
        }
        return depths.get(udb);
    }

    @Override
    public int compare(Flaw flaw, Flaw t1) {
        int v1, v2;
        if(flaw instanceof UnsupportedTimeline)
            v1 = evaluate((UnsupportedTimeline) flaw);
        else
            v1 = 999999;
        if(t1 instanceof UnsupportedTimeline)
            v2 = evaluate((UnsupportedTimeline) t1);
        else
            v2 = 999999;

        return v1 - v2;
    }
}
