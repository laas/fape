package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.Planner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnrefinedTask;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Task;

import java.util.LinkedList;
import java.util.List;

public class UnrefinedTaskFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(State st, Planner planner) {
        List<Flaw> flaws = new LinkedList<>();

        for(Task ac : st.getOpenTasks())
            flaws.add(new UnrefinedTask(ac));

        return flaws;
    }
}
