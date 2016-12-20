package fr.laas.fape.planning.core.planning.search.strategies.plans;

import fr.laas.fape.planning.core.planning.states.PartialPlan;

/**
 * Depth first search strategy: the deepest plan is always selected first.
 */
public class DepthFirst extends PartialPlanComparator {
    @Override
    public String shortName() {
        return "dfs";
    }

    @Override
    public String reportOnState(PartialPlan plan) {
        return String.format("DFS:\t depth: %s", plan.getDepth());
    }

    @Override
    public double g(PartialPlan plan) {
        return 10000f - plan.getDepth();
    }

    @Override
    public double h(PartialPlan plan) {
        return 0f;
    }

    @Override
    public double hc(PartialPlan plan) {
        return 0;
    }
}
