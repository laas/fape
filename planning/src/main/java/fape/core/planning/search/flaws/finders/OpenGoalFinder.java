package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.Planner;
import fape.core.planning.planner.PlanningOptions;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;

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
