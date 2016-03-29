package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

public class OrderedDecompositions extends PartialPlanComparator {
    @Override
    public String shortName() {
        return "ord-dec";
    }

    @Override
    public String reportOnState(State st) {
        return "last decomposition id: "+st.getLastDecompositionNumber();
    }

    @Override
    public float g(State st) {
        return st.getLastDecompositionNumber();
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
