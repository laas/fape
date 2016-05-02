package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.Planner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fape.core.planning.states.State;

import java.util.List;
import java.util.stream.Collectors;

public class OpenGoalFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(State st, Planner planner) {
        return st.tdb.getConsumers().stream()
                .filter(tl -> !planner.pb.allActionsAreMotivated() || !st.getHierarchicalConstraints().isWaitingForADecomposition(tl))
                .map(UnsupportedTimeline::new)
                .collect(Collectors.toList());
    }
}
