package fape.core.planning.search.conflicts;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.Flaw;
import fape.core.planning.search.UnsupportedTaskCond;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.ActionCondition;

import java.util.LinkedList;
import java.util.List;

public class UnsupportedTaskConditionFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(State st, APlanner planner) {
        List<Flaw> flaws = new LinkedList<>();

        for(ActionCondition ac : st.getOpenTaskConditions())
            flaws.add(new UnsupportedTaskCond(ac));

        return flaws;
    }
}
