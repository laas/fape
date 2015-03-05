package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionCondition;

/**
 * Mark an action (already in the plan) as supporting an action condition.
 */
public class ExistingTaskSupporter extends Resolver {

    /** Unsupported condition */
    public final ActionCondition condition;

    /** Supporting action. */
    public final Action act;

    public ExistingTaskSupporter(ActionCondition cond, Action act) {
        this.condition = cond;
        this.act = act;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        assert condition.args().size() == act.args().size();
        assert condition.abs() == act.abs();

        // add equality constraint between all args
        for (int i = 0; i < condition.args().size(); i++) {
            st.addUnificationConstraint(act.args().get(i), condition.args().get(i));
        }
        //enforce equality of time points and add support to task network
        st.addSupport(condition, act);

        return true;
    }
}

