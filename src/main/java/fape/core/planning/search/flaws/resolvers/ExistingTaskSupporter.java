package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;

/**
 * Mark an action (already in the plan) as supporting an action condition.
 */
public class ExistingTaskSupporter extends Resolver {

    /** Unsupported condition */
    public final Task condition;

    /** Supporting action. */
    public final Action act;

    public ExistingTaskSupporter(Task cond, Action act) {
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

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof ExistingTaskSupporter;
        ExistingTaskSupporter o = (ExistingTaskSupporter) e;
        assert condition == o.condition : "Comparing two resolvers on different flaws.";
        assert act != o.act;
        return act.id().id() - o.act.id().id();
    }
}

