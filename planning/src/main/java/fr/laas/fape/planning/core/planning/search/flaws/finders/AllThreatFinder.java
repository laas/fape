package fr.laas.fape.planning.core.planning.search.flaws.finders;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.List;

public class AllThreatFinder implements FlawFinder {

    public List<Flaw> getFlaws(State st, Planner planner) {
        return st.getAllThreats();
    }
}