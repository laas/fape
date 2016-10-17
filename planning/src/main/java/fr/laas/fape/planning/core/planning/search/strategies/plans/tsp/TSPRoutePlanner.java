package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.planning.core.planning.grounding.Fluent;
import fr.laas.fape.planning.core.planning.states.State;
import lombok.Value;

import java.util.Collection;
import java.util.function.Consumer;

public interface TSPRoutePlanner {

    @Value class Result {
        /** the goal in targets, that is closest from the partial state */
        final Fluent closestGoal;
        /** cost (in terms of search to reach this goal */
        final int cost;
        /** A transformation applicable to the partial state.
         * The selected goal must be achivable in this state. */
        final Consumer<PartialState> transformation;
    }

    Result getPlan(Collection<Fluent> targets, PartialState ps, State st);
}
