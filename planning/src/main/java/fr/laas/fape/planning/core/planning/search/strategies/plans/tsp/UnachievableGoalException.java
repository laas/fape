package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.exceptions.InconsistencyException;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value @EqualsAndHashCode(callSuper = false)
public class UnachievableGoalException extends InconsistencyException {

    final public Timeline og;
}
