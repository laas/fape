package fape.core.planning.search.resolvers;

import planstack.anml.model.concrete.Action;

public class Decomposition extends Resolver {

    /** Action to decompose */
    public final Action act;

    /** ID of the decomposition to apply */
    public final int decID;

    public Decomposition(Action a, int id) {
        act = a;
        decID = id;
    }

    @Override
    public boolean hasDecomposition() { return true; }
    @Override
    public Action actionToDecompose() { return act; }
}
