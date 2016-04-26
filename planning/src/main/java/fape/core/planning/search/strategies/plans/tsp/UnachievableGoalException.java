package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.timelines.Timeline;
import fr.laas.fape.exceptions.InconsistencyException;
import lombok.Value;

@Value
public class UnachievableGoalException extends InconsistencyException {

    final public Timeline og;
}
