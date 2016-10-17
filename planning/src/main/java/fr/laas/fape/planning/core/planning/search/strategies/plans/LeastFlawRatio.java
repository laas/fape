package fr.laas.fape.planning.core.planning.search.strategies.plans;


import fr.laas.fape.planning.core.planning.states.State;

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
    public String reportOnState(State st) {
        return "LFR:\tflaw-ratio (%): "+eval(st);
    }

    float eval(State st) {
        return ((float) st.tdb.getConsumers().size()) / ((float) st.getNumActions())*100;
    }

    @Override
    public double g(State st) {
        return 0;
    }

    @Override
    public double h(State st) {
        return eval(st);
    }

    @Override
    public double hc(State st) {
        return eval(st);
    }
}
