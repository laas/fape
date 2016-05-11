package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.search.strategies.plans.PartialPlanComparator;
import fape.core.planning.states.State;

public class MinSpanTreeComp extends PartialPlanComparator {
    @Override
    public String shortName() {
        return "minspan";
    }

    private MinSpanTreeExt getExt(State st) {
        if(!st.hasExtension(MinSpanTreeExt.class))
            st.addExtension(new MinSpanTreeExt(st, true));
        return st.getExtension(MinSpanTreeExt.class);
    }

    @Override
    public float g(State st) {
        return getExt(st).getCurrentCost();
    }

    @Override
    public float h(State st) {
        return hc(st);
    }

    @Override
    public float hc(State st) {
        return getExt(st).getCostToGo();
    }
}
