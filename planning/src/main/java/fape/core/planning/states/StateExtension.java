package fape.core.planning.states;

/**
 * Represents imforamtion that can ve attached to a state.
 *
 * This is intended to keep the code in State.java generic will extensions of the main
 * planning algorithm can rely on such structures to store the additional information they need.
 */
public interface StateExtension {

    StateExtension clone(State st);
}
