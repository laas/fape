package fape.core.planning.search;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;

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
        SELECTION // when a state is selected from the queue as the next one to be expanded
    }

    /**
     * This method is invoked when a (initial) state is first attached to a planner.
     * It is not called on any descendant of this state. Handlers should use this method
     * to initialize their data structures and bootstrap the calculations.
     *
     * For instance, the state given here might already contain some actions that the handler
     * might need to process. All actions later added to this state or its descendants will be
     * notified incrementally through the `actionInserted` method.
     */
    default void stateBindedToPlanner(State st, APlanner pl) {}

    /**
     * Informs the handler that the given state has reached a given point in its life.
     * The handler can start any computation he wants to do at that point.
     */
    default void apply(State st, StateLifeTime time, APlanner planner) {}

    default void actionInserted(Action a, State st, APlanner planner) {}
    default void taskInserted(Task a, State st, APlanner planner) {}

    /** Notifies the handler that in this action was set to support this task in the given state */
    default void supportLinkAdded(Action a, Task t, State st) {}
}
