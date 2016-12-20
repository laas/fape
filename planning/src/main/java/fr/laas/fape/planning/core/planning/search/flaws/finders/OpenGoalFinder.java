package fr.laas.fape.planning.core.planning.search.flaws.finders;


import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fr.laas.fape.planning.core.planning.states.PartialPlan;

import java.util.List;
import java.util.stream.Collectors;

public class OpenGoalFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(PartialPlan plan, Planner planner) {
        return plan.tdb.getConsumers().stream()
                .filter(tl -> !plan.getHierarchicalConstraints().isWaitingForADecomposition(tl))
                .map(tl -> new UnsupportedTimeline(tl, planner.options.actionInsertionStrategy))
                .collect(Collectors.toList());
    }
}
