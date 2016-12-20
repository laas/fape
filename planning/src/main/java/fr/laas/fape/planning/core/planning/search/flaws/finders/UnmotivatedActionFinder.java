package fr.laas.fape.planning.core.planning.search.flaws.finders;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnmotivatedAction;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.planning.core.planning.states.PartialPlan;

import java.util.LinkedList;
import java.util.List;

public class UnmotivatedActionFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(PartialPlan plan, Planner planner) {
        List<Flaw> flaws = new LinkedList<>();

        for(Action unmotivated : plan.getUnmotivatedActions())
            flaws.add(new UnmotivatedAction(unmotivated));

        return flaws;
    }
}
