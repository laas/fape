package fape.core.planning.search.flaws.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.search.flaws.resolvers.DecomposeAction;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Action;

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
            if(planner.options.usePlanningGraphReachability) {
                // in this case every action is associated with a variable representing its possible decompositions
                // a decomposition is possible only if this decomposition is in the domain of the said variable
                List<String> possibleDecompositions = st.domainOf(action.decompositionVar());
                if(possibleDecompositions.contains(FeasibilityReasoner.decCSPValue(decompositionID))) {
                    resolvers.add(new DecomposeAction(action, decompositionID));
                }
            } else {
                resolvers.add(new DecomposeAction(action, decompositionID));
            }
        }

        return resolvers;
    }

    @Override
    public int compareTo(Flaw o) {
        assert o instanceof UndecomposedAction;
        return ((UndecomposedAction) o).action.id().id() - action.id().id();
    }
}
