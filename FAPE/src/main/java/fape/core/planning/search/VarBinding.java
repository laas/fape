package fape.core.planning.search;

import planstack.anml.model.concrete.VarRef;

public class VarBinding extends SupportOption {

    public final VarRef var;
    public final String value;

    public VarBinding(VarRef var, String value) {
        this.var = var;
        this.value = value;
    }
}
