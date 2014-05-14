package fape.core.planning.search;

import planstack.anml.model.concrete.VarRef;

public class BindingSeparation extends SupportOption {

    public final VarRef a, b;

    public BindingSeparation(VarRef a, VarRef b) {
        this.a = a;
        this.b = b;
    }
}
