package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;

import java.util.LinkedList;
import java.util.List;

public class OpenGoalFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(State st, APlanner planner) {
        List<Flaw> flaws = new LinkedList<>();

        for(Timeline consumer : st.tdb.getConsumers())
            flaws.add(new UnsupportedTimeline(consumer));

        return flaws;
    }
}
