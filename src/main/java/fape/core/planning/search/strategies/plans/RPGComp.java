package fape.core.planning.search.strategies.plans;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.UnsupportedDatabase;
import fape.core.planning.search.strategies.flaws.RPGOpenGoalComp;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;

import java.util.HashMap;
import java.util.Map;

public class RPGComp implements PartialPlanComparator {

    Map<State, Integer> hs = new HashMap<>();

    @Override
    public String shortName() {
        return "rpgmax";
    }

    public int evaluate(State st) {
        RPGOpenGoalComp comp = new RPGOpenGoalComp(st);
        if(!hs.containsKey(st)) {
            int max = 0;
            for (TemporalDatabase db : st.consumers) {
                int h = comp.evaluate(new UnsupportedDatabase(db));
                max += h;
//                if(h > max)
//                    max = h;
            }
            hs.put(st, max);
        }
        return hs.get(st) + st.getNumActions()*4 + st.consumers.size();
    }

    @Override
    public int compare(State state, State t1) {
        return  -(evaluate(t1) - evaluate(state));
    }
}
