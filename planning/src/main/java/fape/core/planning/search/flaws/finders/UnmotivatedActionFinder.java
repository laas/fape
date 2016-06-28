package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.Planner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnmotivatedAction;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Action;

import java.util.LinkedList;
import java.util.List;

public class UnmotivatedActionFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(State st, Planner planner) {
        List<Flaw> flaws = new LinkedList<>();

        for(Action unmotivated : st.getUnmotivatedActions())
            flaws.add(new UnmotivatedAction(unmotivated));

        return flaws;
    }
}
