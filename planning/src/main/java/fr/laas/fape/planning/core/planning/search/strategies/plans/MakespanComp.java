package fr.laas.fape.planning.core.planning.search.strategies.plans;

import fr.laas.fape.planning.core.planning.states.PartialPlan;

public class MakespanComp extends PartialPlanComparator {
    @Override
    public String shortName() {
        return "makespan";
    }

    @Override
    public double g(PartialPlan plan) {
        return plan.getMakespan();
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
