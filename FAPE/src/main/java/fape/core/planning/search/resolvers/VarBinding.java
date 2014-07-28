package fape.core.planning.search.resolvers;

import planstack.anml.model.concrete.VarRef;

public class VarBinding extends Resolver {

    public final VarRef var;
    public final String value;

    public VarBinding(VarRef var, String value) {
        this.var = var;
        this.value = value;
    }
}
