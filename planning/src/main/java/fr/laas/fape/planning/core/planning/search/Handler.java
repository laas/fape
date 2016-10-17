package fr.laas.fape.planning.core.planning.search;

import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.SearchNode;
import fr.laas.fape.planning.core.planning.states.State;

/**
 * A handler is a computation unit that implements specific (and usually optional) aspects of the
 * planning process.
 *
 * Every recorded handler will be notified whenever a search state reaches a remarkable point of its lifetime.
 * The handler can then perform any computation to update the state.
 */
public abstract class Handler {

    /** Represent all possible points in a State life time at which a handler can be executed. */
    public enum StateLifeTime {
        SELECTION, // when a state is selected from the queue as the next one to be expanded
        PRE_QUEUE_INSERTION // signal sent just before inserting a state to the queue
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
    public void stateBindedToPlanner(State st, Planner pl) {}

    /**
     * Informs the handler that the given state has reached a given point in its life.
     * The handler can start any computation he wants to do at that point.
     */
    protected void apply(State st, StateLifeTime time, Planner planner) {}

    public final void addOperation(SearchNode n, StateLifeTime time, Planner planner) {
        n.addOperation(s -> {
            apply(s, time, planner);
            s.checkConsistency();
        });
    }

    public void actionInserted(Action a, State st, Planner planner) {}
    public void taskInserted(Task a, State st, Planner planner) {}

    /** Notifies the handler that in this action was set to support this task in the given state */
    public void supportLinkAdded(Action a, Task t, State st) {}
}
