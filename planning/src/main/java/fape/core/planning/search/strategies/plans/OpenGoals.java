package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

import java.util.HashMap;

public class OpenGoals extends PartialPlanComparator {

    private HashMap<Integer, Integer> numOpenGoals = new HashMap<>();

    private int numOpenGoals(State st) {
        if(!numOpenGoals.containsKey(st.mID))
            numOpenGoals.put(st.mID, st.tdb.getConsumers().size());
        return numOpenGoals.get(st.mID);
    }

    @Override
    public String shortName() {
        return "opengoals";
    }

    @Override
    public String reportOnState(State st) {
        return "OpenGoals:\tnum-open-goals: "+st.tdb.getConsumers().size();
    }

    @Override
    public float g(State st) {
        return 0;
    }

    @Override
    public float h(State st) {
        return numOpenGoals(st);
    }

    @Override
    public float hc(State st) {
        return numOpenGoals(st);
    }
}
