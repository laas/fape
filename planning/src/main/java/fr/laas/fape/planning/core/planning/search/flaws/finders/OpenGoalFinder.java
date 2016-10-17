package fr.laas.fape.planning.core.planning.search.flaws.finders;


import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenGoalFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(State st, Planner planner) {
        return st.tdb.getConsumers().stream()
                .filter(tl -> !st.getHierarchicalConstraints().isWaitingForADecomposition(tl))
                .map(tl -> new UnsupportedTimeline(tl, planner.options.actionInsertionStrategy))
                .collect(Collectors.toList());
    }
}
