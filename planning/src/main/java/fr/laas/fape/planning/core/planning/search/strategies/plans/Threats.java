package fr.laas.fape.planning.core.planning.search.strategies.plans;


import fr.laas.fape.planning.core.planning.states.PartialPlan;

import java.util.HashMap;

public class Threats extends PartialPlanComparator {

    HashMap<Integer, Integer> numThreats = new HashMap<>();

    int numThreats(PartialPlan st) {
        if(!numThreats.containsKey(st.mID))
            numThreats.put(st.mID, st.getAllThreats().size());
        return numThreats.get(st.mID);
    }

    @Override
    public String shortName() {
        return "threats";
    }

    @Override
    public String reportOnState(PartialPlan plan) {
        return "Threats:\tnum-threats: "+ plan.getAllThreats().size();
    }

    @Override
    public double g(PartialPlan plan) {
        return 0;
    }

    @Override
    public double h(PartialPlan plan) {
        return numThreats(plan);
    }

    @Override
    public double hc(PartialPlan plan) {
        return numThreats(plan);
    }
}
