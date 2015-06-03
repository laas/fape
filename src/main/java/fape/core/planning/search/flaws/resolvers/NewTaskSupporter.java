package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.PlanningGraphReachability;
import fape.core.planning.states.State;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionCondition;
import planstack.anml.model.concrete.Factory;
import planstack.anml.model.concrete.VarRef;

import java.util.LinkedList;
import java.util.List;

/**
 * Inserts a new action to support an action condition.
 */
public class NewTaskSupporter extends Resolver {

    /** Action condition to support */
    public final ActionCondition condition;

    /** Abstract action to be instantiated and inserted. */
    public final AbstractAction abs;

    public NewTaskSupporter(ActionCondition cond, AbstractAction abs) {
        this.condition = cond;
        this.abs = abs;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        // create a new action with the same args as the condition
        Action act = Factory.getInstantiatedAction(st.pb, abs, condition.args());
        st.insert(act);

        // enforce equality of time points and add support to task network
        st.addSupport(condition, act);

        return true;
    }
}
