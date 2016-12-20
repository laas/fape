package fr.laas.fape.planning.core.planning.search.flaws.finders;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.states.PartialPlan;

import java.util.List;

public class AllThreatFinder implements FlawFinder {

    public List<Flaw> getFlaws(PartialPlan plan, Planner planner) {
        return plan.getAllThreats();
    }
}