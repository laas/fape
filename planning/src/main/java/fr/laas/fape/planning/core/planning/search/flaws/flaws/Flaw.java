package fr.laas.fape.planning.core.planning.search.flaws.flaws;


import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.Resolver;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.List;


/**
 * Represents a flaw in a partial plan.
 * It is used to allow extraction of the resolvers directly from the state.
 */
public abstract class Flaw {

    /**
     * List of resolvers for this flaw.
     *
     * This is stored locally to make sure they are not computed twice.
     */
    public List<Resolver> resolvers = null; // TODO: move back to protected once resources are fixed

    /**
     * Returns the number of resolvers for this flaws.
     * This method is advised when the number of resolver is needed since it does not
     * necessarily generate all resolvers (thus reducing the stress on the garbage collection).
     * @param st State used to look for resolvers.
     * @param planner The planner instance from which this method is called.
     *                It can be used to look for options as well as general knowledge
     *                on the problem (typically coming from preprocessing)
     * @return Number of resolvers for this flaw.
     */
    public int getNumResolvers(State st, Planner planner) {
        if(resolvers == null)
            resolvers = getResolvers(st, planner);
        return resolvers.size();
    }

    /**
     * Finds and returns all resolvers for the flaw.
     * Best effort is done to make sure all resolvers are applicable in the state which might
     * result in expensive checks. Also some resolvers might not be applicable.
     *
     * @param st State used to look for resolvers.
     * @param planner The planner instance from which this method is called.
     *                It can be used to look for options as well as general knowledge
     *                on the problem (typically coming from preprocessing)
     * @return A list of resolvers.
     */
    public abstract List<Resolver> getResolvers(State st, Planner planner);

    public abstract int compareTo(Flaw o);
}
