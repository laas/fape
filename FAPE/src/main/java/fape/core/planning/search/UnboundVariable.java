package fape.core.planning.search;

import planstack.anml.model.concrete.VarRef;

public class UnboundVariable extends Flaw {

    public final VarRef var;

    public UnboundVariable(VarRef v) {
        this.var = v;
    }
}
