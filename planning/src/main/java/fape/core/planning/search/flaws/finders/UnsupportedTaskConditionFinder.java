package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnsupportedTask;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Task;

import java.util.LinkedList;
import java.util.List;

public class UnsupportedTaskConditionFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(State st, APlanner planner) {
        List<Flaw> flaws = new LinkedList<>();

        for(Task ac : st.getOpenTasks())
            flaws.add(new UnsupportedTask(ac));

        return flaws;
    }
}
