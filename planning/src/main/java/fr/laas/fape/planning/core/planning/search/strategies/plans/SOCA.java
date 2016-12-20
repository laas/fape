package fr.laas.fape.planning.core.planning.search.strategies.plans;


import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.finders.AllThreatFinder;
import fr.laas.fape.planning.core.planning.states.PartialPlan;

/**
 * Evaluation function: num-actions*10 + num-consumers*3 + num-undecomposed*3
 */
public class SOCA extends PartialPlanComparator {

    private final Planner planner;
    private final AllThreatFinder threatFinder = new AllThreatFinder();

    public SOCA(Planner planner) { this.planner = planner; }

    @Override
    public String shortName() {
        return "soca";
    }

    @Override
    public String reportOnState(PartialPlan plan) {
        return "SOCA:\t g: "+g(plan)+" h: "+h(plan);
    }

    @Override
    public double g(PartialPlan plan) {
        return plan.getNumActions() * 10;
    }

    @Override
    public double h(PartialPlan plan) {
        return threatFinder.getFlaws(plan, planner).size() * 3 +
                plan.tdb.getConsumers().size()*3 +
                plan.taskNet.getNumOpenTasks() *3;
    }

    @Override
    public double hc(PartialPlan plan) {
        return h(plan);
    }
}
