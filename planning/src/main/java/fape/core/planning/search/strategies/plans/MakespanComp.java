package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

class MakespanComp extends PartialPlanComparator {
    @Override
    public String shortName() {
        return "makespan";
    }

    @Override
    public float g(State st) {
        return st.getMakespan();
    }

    @Override
    public float h(State st) {
        return 0;
    }

    @Override
    public float hc(State st) {
        return 0;
    }
}
