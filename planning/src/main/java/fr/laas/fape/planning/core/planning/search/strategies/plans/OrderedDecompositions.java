package fr.laas.fape.planning.core.planning.search.strategies.plans;


import fr.laas.fape.planning.core.planning.states.PartialPlan;

public class OrderedDecompositions extends PartialPlanComparator {
    @Override
    public String shortName() {
        return "ord-dec";
    }

    @Override
    public String reportOnState(PartialPlan plan) {
        return "last decomposition id: "+ plan.getLastDecompositionNumber();
    }

    @Override
    public double g(PartialPlan plan) {
        return plan.getLastDecompositionNumber();
    }

    @Override
    public double h(PartialPlan plan) {
        return 0;
    }

    @Override
    public double hc(PartialPlan plan) {
        return 0;
    }
}
