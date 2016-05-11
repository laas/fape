package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.Planner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;

import java.util.List;

public class AllThreatFinder implements FlawFinder {

    public List<Flaw> getFlaws(State st, Planner planner) {
        return st.getAllThreats(); 
    }
}