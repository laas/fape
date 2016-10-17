package fr.laas.fape.planning.core.planning.search.strategies.plans;

import fr.laas.fape.planning.core.planning.states.State;

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
    public double g(State st) {
        return 0;
    }

    @Override
    public double h(State st) {
        return numOpenGoals(st);
    }

    @Override
    public double hc(State st) {
        return numOpenGoals(st);
    }
}
