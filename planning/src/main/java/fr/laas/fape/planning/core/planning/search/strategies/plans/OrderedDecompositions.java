package fr.laas.fape.planning.core.planning.search.strategies.plans;


import fr.laas.fape.planning.core.planning.states.State;

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
    public double g(State st) {
        return st.getLastDecompositionNumber();
    }

    @Override
    public double h(State st) {
        return 0;
    }

    @Override
    public double hc(State st) {
        return 0;
    }
}
