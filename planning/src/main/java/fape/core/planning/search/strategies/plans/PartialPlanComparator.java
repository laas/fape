package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

import java.util.Comparator;

public interface PartialPlanComparator extends Comparator<State> {

    String shortName();

    /** Gives a human readable string of the metrics used to evaluate a state, and their values. */
    String reportOnState(State st);
}
