package fape.core.planning.search;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;

/**
 * A handler is a computation unit that implements specific (and usually optional) aspects of the
 * planning process.
 *
 * Every recorded handler will be notified whenever a search state reaches a remarkable point of its lifetime.
 * The handler can then perform any computation to update the state.
 */
public interface Handler {

    /** Represent all possible points in a State life time at which a handler can be executed. */
    enum StateLifeTime {
        SELECTION // when a state is selected from the queue as the next one to be expandedx
    }

    /**
     * Informs the handler that the given state has reached a given point in its life.
     * The handler can start any computation he wants to do at that point.
     */
    void apply(State st, StateLifeTime time, APlanner planner);
}
