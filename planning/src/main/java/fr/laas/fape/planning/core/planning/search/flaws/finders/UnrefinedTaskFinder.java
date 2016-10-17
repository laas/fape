package fr.laas.fape.planning.core.planning.search.flaws.finders;

import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnrefinedTask;
import fr.laas.fape.planning.core.planning.states.State;

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
