package fape.core.planning.search;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.resolvers.Decomposition;
import fape.core.planning.search.resolvers.Resolver;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Action;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;


/**
 * Represents a decomposable action that has not yet been decomposed.
 *
 * There is one resolver for each possible decomposition.
 */
public class UndecomposedAction extends Flaw {

    public final Action action;

    public UndecomposedAction(Action a) {
        this.action = a;
    }

    @Override
    public String toString() {
        return "Undecomposed: " + action;
    }

    @Override
    public int getNumResolvers(State st, APlanner planner) {
        if(resolvers != null)
            return resolvers.size();
        else
            return action.decompositions().size();
    }

    @Override
    public List<Resolver> getResolvers(State st, APlanner planner) {
        if(resolvers != null)
            return resolvers;

        resolvers = new LinkedList<>();
        for (int decompositionID = 0; decompositionID < action.decompositions().size(); decompositionID++) {
            resolvers.add(new Decomposition(action, decompositionID));
        }

        return resolvers;
    }
}
