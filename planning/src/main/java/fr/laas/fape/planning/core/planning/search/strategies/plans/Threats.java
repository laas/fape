package fr.laas.fape.planning.core.planning.search.strategies.plans;


import fr.laas.fape.planning.core.planning.states.State;

import java.util.HashMap;

public class Threats extends PartialPlanComparator {

    HashMap<Integer, Integer> numThreats = new HashMap<>();

    int numThreats(State st) {
        if(!numThreats.containsKey(st.mID))
            numThreats.put(st.mID, st.getAllThreats().size());
        return numThreats.get(st.mID);
    }

    @Override
    public String shortName() {
        return "threats";
    }

    @Override
    public String reportOnState(State st) {
        return "Threats:\tnum-threats: "+st.getAllThreats().size();
    }

    @Override
    public double g(State st) {
        return 0;
    }

    @Override
    public double h(State st) {
        return numThreats(st);
    }

    @Override
    public double hc(State st) {
        return numThreats(st);
    }
}
