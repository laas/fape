package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.VarRef;

import java.util.LinkedList;
import java.util.List;

/**
 * Binds a variable to the given value.
 */
public class VarBinding extends Resolver {

    public final VarRef var;
    public final String value;

    public VarBinding(VarRef var, String value) {
        this.var = var;
        this.value = value;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        List<String> values = new LinkedList<>();
        values.add(value);
        st.restrictDomain(var, values);

        return true;
    }
}
