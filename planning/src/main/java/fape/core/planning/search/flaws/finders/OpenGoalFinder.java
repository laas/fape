package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.abs.AbstractAction;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class OpenGoalFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(State st, APlanner planner) {
        return st.tdb.getConsumers().stream()
                .filter(tl -> !AbstractAction.allActionsAreMotivated() || !st.getHierarchicalConstraints().isWaitingForADecomposition(tl))
                .map(UnsupportedTimeline::new)
                .collect(Collectors.toList());
    }
}
