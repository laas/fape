package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

import java.util.HashMap;

public class OpenGoals implements PartialPlanComparator {

    HashMap<Integer, Integer> numOpenGoals = new HashMap<>();

    int numOpenGoals(State st) {
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
    public int compare(State state, State t1) {
        return numOpenGoals(t1) - numOpenGoals(state);
    }
}
