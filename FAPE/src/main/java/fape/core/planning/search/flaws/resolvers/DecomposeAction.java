package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Decomposition;
import planstack.anml.model.concrete.Factory;

/**
 * This resolver simply deomposes an action.
 */
public class DecomposeAction extends Resolver {

    /** Action to decompose */
    public final Action act;

    /** ID of the decomposition to apply */
    public final int decID;

    public DecomposeAction(Action a, int id) {
        act = a;
        decID = id;
    }

    @Override
    public boolean hasDecomposition() { return true; }
    @Override
    public Action actionToDecompose() { return act; }

    @Override
    public boolean apply(State st, APlanner planner) {
        // Apply the i^th decomposition of o.actionToDecompose, where i is given by
        // o.decompositionID

        // Abstract version of the decomposition
        AbstractDecomposition absDec = act.decompositions().get(decID);

        // Decomposition (ie implementing StateModifier) containing all changes to be made to a search state.
        Decomposition dec = Factory.getDecomposition(st.pb, act, absDec);

        st.applyDecomposition(dec);

        return true;
    }
}
