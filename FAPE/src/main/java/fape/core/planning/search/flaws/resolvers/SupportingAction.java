package fape.core.planning.search.flaws.resolvers;

import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;

import java.util.Collection;
import java.util.Map;

public class SupportingAction extends Resolver {

    public final AbstractAction act;
    public final Map<LVarRef, Collection<String>> values;

    public SupportingAction(AbstractAction act) {
        this.act = act;
        values = null;
    }

    public SupportingAction(AbstractAction act, Map<LVarRef, Collection<String>> values) {
        this.act = act;
        this.values = values;
    }

    public boolean hasActionInsertion() {
        return true;
    }
}
