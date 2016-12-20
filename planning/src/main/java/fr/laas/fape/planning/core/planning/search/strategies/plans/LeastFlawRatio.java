package fr.laas.fape.planning.core.planning.search.strategies.plans;


import fr.laas.fape.planning.core.planning.states.PartialPlan;

/**
 * Selects the plans with the least number of flaws with respect to the number of action.
 *
 * Evaluation function: (num open leaves + num consumers) / numActions.
 */
public class LeastFlawRatio extends PartialPlanComparator {
    @Override
    public String shortName() {
        return "lfr";
    }

    @Override
    public String reportOnState(PartialPlan plan) {
        return "LFR:\tflaw-ratio (%): "+eval(plan);
    }

    float eval(PartialPlan st) {
        return ((float) st.tdb.getConsumers().size()) / ((float) st.getNumActions())*100;
    }

    @Override
    public double g(PartialPlan plan) {
        return 0;
    }

    @Override
    public double h(PartialPlan plan) {
        return eval(plan);
    }

    @Override
    public double hc(PartialPlan plan) {
        return eval(plan);
    }
}
