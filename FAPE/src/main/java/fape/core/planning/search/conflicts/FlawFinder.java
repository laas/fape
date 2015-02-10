package fape.core.planning.search.conflicts;

import fape.core.planning.search.Flaw;
import fape.core.planning.states.State;

import java.util.List;


/**
 * Classes implementing this interface are used to extract flaws from a state.
 *
 * A planner will typically have one such finder for every type of flaw.
 */
public interface FlawFinder {

    abstract public List<Flaw> getFlaws(State st);
}
