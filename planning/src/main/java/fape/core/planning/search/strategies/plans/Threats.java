package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

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
    public float g(State st) {
        return 0;
    }

    @Override
    public float h(State st) {
        return numThreats(st);
    }

    @Override
    public float hc(State st) {
        return numThreats(st);
    }
}
