package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.states.State;
import lombok.Value;

import java.util.Collection;
import java.util.function.Consumer;

public interface TSPRoutePlanner {

    @Value class Result {
        final Fluent closestGoal;
        final int cost;
        final Consumer<PartialState> transformation;
    }

    Result getPlan(Collection<Fluent> targets, PartialState ps, State st);
}
