package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

import java.util.Comparator;

public interface PartialPlanComparator extends Comparator<State> {

    public abstract String shortName();
}
