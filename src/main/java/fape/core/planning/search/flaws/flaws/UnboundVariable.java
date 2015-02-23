package fape.core.planning.search.flaws.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.search.flaws.resolvers.VarBinding;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.VarRef;

import java.util.LinkedList;
import java.util.List;

/**
 * A variable that is not binded (i.e. domain contains more than one value)
 */
public class UnboundVariable extends Flaw {

    public final VarRef var;

    public UnboundVariable(VarRef v) {
        this.var = v;
    }

    /**
     * Here the number of resolvers is the size of the domain.
     * Hence we lazily return that without creating the resolvers themselves.
     */
    @Override
    public int getNumResolvers(State st, APlanner planner) {
        if(resolvers != null)
            return resolvers.size();
        else
            return st.domainSizeOf(var);
    }

    @Override
    public List<Resolver> getResolvers(State st, APlanner planner) {
        if(resolvers != null)
            return resolvers;

        resolvers = new LinkedList<>();
         for (String value : st.domainOf(var)) {
            resolvers.add(new VarBinding(var, value));
        }

        return resolvers;
    }
}
