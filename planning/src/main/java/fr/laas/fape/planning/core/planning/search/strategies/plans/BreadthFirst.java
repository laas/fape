package fr.laas.fape.planning.core.planning.search.strategies.plans;

import fr.laas.fape.planning.core.planning.states.PartialPlan;

/**
 * Breadth first search strategy: the least deep state is always selected first.
 */
public class BreadthFirst extends PartialPlanComparator {
    @Override
    public String shortName() {
        return "bfs";
    }

    @Override
    public String reportOnState(PartialPlan plan) {
        return "BFS:\t depth: "+ plan.getDepth();
    }

    @Override
    public double g(PartialPlan plan) {
        return plan.getDepth();
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
