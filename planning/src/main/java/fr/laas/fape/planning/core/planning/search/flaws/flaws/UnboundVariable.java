package fr.laas.fape.planning.core.planning.search.flaws.flaws;


import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.Resolver;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.VarBinding;
import fr.laas.fape.planning.core.planning.states.PartialPlan;

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
    public int getNumResolvers(PartialPlan plan, Planner planner) {
        if(resolvers != null)
            return resolvers.size();
        else
            return plan.domainSizeOf(var);
    }

    @Override
    public List<Resolver> getResolvers(PartialPlan plan, Planner planner) {
        if(resolvers != null)
            return resolvers;

        resolvers = new LinkedList<>();
         for (String value : plan.domainOf(var)) {
            resolvers.add(new VarBinding(var, value));
        }

        return resolvers;
    }

    @Override
    public int compareTo(Flaw o) {
        assert o instanceof UnboundVariable;
        return ((UnboundVariable) o).var.id() - var.id();
    }
}
