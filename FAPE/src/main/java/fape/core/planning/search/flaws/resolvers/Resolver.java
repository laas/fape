package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import planstack.anml.model.concrete.Action;

/**
 * A resolver is recipe to fix a Flaw.
 * It provides an apply method that modifies a state so that the flaw is fixed.
 */
public abstract class Resolver {

    public boolean representsCausalLinkAddition() {
        return this instanceof SupportingDatabase ||
                this instanceof SupportingAction ||
                this instanceof SupportingActionDecomposition;
    }

    public boolean hasDecomposition() {
        return false;
    }
    public Action actionToDecompose() {
        throw new FAPEException("This resolver does not provide decomposition");
    }

    /**
     * Modifies the state so that the flaw this resolver was created from is fixed.
     * @param st State to modify
     * @param planner The planner from which this method is called. It used to extract options and
     *                results from preprocessing that might be used.
     * @return True if the resolvers was successfully applied. (Note that the state might still e inconsistent, the only
     *         guarantee is that the flaw is fixed.
     */
    public abstract boolean apply(State st, APlanner planner);
}
