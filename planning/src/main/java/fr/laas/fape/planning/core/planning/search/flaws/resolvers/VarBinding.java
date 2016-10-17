package fr.laas.fape.planning.core.planning.search.flaws.resolvers;


import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.LinkedList;
import java.util.List;

/**
 * Binds a variable to the given value.
 */
public class VarBinding implements Resolver {

    public final VarRef var;
    public final String value;

    public VarBinding(VarRef var, String value) {
        this.var = var;
        this.value = value;
    }

    @Override
    public boolean apply(State st, Planner planner, boolean isFastForwarding) {
        List<String> values = new LinkedList<>();
        values.add(value);
        st.restrictDomain(var, values);

        return true;
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof VarBinding;
        VarBinding o = (VarBinding) e;
        if(!value.equals(o.value))
            return value.compareTo(o.value);
        assert var != o.var;
        return var.id() - o.var.id();
    }
}
