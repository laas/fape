package fape.core.planning.states;

import fape.core.planning.search.Handler;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.concrete.Chronicle;

/**
 * Represents information that can be attached to a state.
 *
 * This is intended to keep the code in State.java generic while extensions of the main
 * planning algorithm can rely on such structures to store the additional information they need.
 */
public interface StateExtension {

    /** Builds a clone of this extension that will attached to the state given in parameter */
    StateExtension clone(State st);

    default void timelineAdded(Timeline tl) {}
    default void timelineRemoved(Timeline tl) {}
    default void timelineExtended(Timeline tl) {}

    default void chronicleMerged(Chronicle c) {}

    default void notify(Handler.StateLifeTime stateLifeTime) {}
}
