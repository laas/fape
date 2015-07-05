package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

import java.util.HashMap;

public class Threats implements PartialPlanComparator {

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
    public int compare(State state, State t1) {
        return numThreats(t1) - numThreats(state);
    }
}
