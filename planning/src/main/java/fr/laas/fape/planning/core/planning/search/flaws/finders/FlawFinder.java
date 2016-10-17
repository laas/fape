package fr.laas.fape.planning.core.planning.search.flaws.finders;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.List;


/**
 * Classes implementing this interface are used to extract flaws from a state.
 *
 * A planner will typically have one such finder for every type of flaw.
 */
public interface FlawFinder {

    /**
     * Returns the flaws present in the current state. Each instance of flaw finder will return a partial
     * view of the flaws (depending of the ones it is looking for).
     *
     * @param st State in which it should look for flaws
     * @param planner Planner from which the method is invoked. It is used to retrieve options as well
     *                problem knowledge (typically coming from preprocessing).
     * @return All flaws this finder found.
     */
    List<Flaw> getFlaws(State st, Planner planner);
}
