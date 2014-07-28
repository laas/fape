package fape.core.planning.search.resolvers;

import fape.exceptions.FAPEException;
import planstack.anml.model.concrete.Action;

public class Resolver {

    public boolean representsCausalLinkAddition() {
        return this instanceof SupportingDatabase;
    }

    public boolean hasDecomposition() {
        return false;
    }
    public Action actionToDecompose() {
        throw new FAPEException("This resolver does not provide decomposition");
    }

    public boolean hasActionInsertion() {
        return false;
    }
}
