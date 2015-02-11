package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.VarRef;

/**
 * Simply adds a difference constraint between the two variables.
 */
public class BindingSeparation extends Resolver {

    public final VarRef a, b;

    public BindingSeparation(VarRef a, VarRef b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        st.addSeparationConstraint(a, b);

        return true;
    }
}
