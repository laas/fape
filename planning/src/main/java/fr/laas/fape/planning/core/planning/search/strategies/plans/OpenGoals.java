package fr.laas.fape.planning.core.planning.search.strategies.plans;

import fr.laas.fape.planning.core.planning.states.PartialPlan;

import java.util.HashMap;

public class OpenGoals extends PartialPlanComparator {

    private HashMap<Integer, Integer> numOpenGoals = new HashMap<>();

    private int numOpenGoals(PartialPlan st) {
        if(!numOpenGoals.containsKey(st.mID))
            numOpenGoals.put(st.mID, st.tdb.getConsumers().size());
        return numOpenGoals.get(st.mID);
    }

    @Override
    public String shortName() {
        return "opengoals";
    }

    @Override
    public String reportOnState(PartialPlan plan) {
        return "OpenGoals:\tnum-open-goals: "+ plan.tdb.getConsumers().size();
    }

    @Override
    public double g(PartialPlan plan) {
        return 0;
    }

    @Override
    public double h(PartialPlan plan) {
        return numOpenGoals(plan);
    }

    @Override
    public double hc(PartialPlan plan) {
        return numOpenGoals(plan);
    }
}
