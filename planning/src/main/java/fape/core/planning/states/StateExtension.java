package fape.core.planning.states;

import fape.core.planning.timelines.Timeline;

/**
 * Represents imforamtion that can ve attached to a state.
 *
 * This is intended to keep the code in State.java generic will extensions of the main
 * planning algorithm can rely on such structures to store the additional information they need.
 */
public interface StateExtension {

    StateExtension clone(State st);

    default void timelineAdded(Timeline tl) {}
    default void timelineRemoved(Timeline tl) {}
    default void timelineExtended(Timeline tl) {}
}
