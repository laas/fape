package fape.core.planning.search.flaws.resolvers;

import planstack.anml.model.concrete.VarRef;

public class BindingSeparation extends Resolver {

    public final VarRef a, b;

    public BindingSeparation(VarRef a, VarRef b) {
        this.a = a;
        this.b = b;
    }
}
