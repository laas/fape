package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;

/**
 * Mark an action (already in the plan) as supporting an action condition.
 */
public class ExistingTaskSupporter extends Resolver {

    /** Unsupported task */
    public final Task task;

    /** Supporting action. */
    public final Action act;

    public ExistingTaskSupporter(Task cond, Action act) {
        this.task = cond;
        this.act = act;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        assert task.args().size() == act.args().size();
        assert task.name().equals(act.taskName());

        // add equality constraint between all args
        for (int i = 0; i < task.args().size(); i++) {
            st.addUnificationConstraint(act.args().get(i), task.args().get(i));
        }
        //enforce equality of time points and add support to task network
        st.addSupport(task, act);

        return true;
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof ExistingTaskSupporter;
        ExistingTaskSupporter o = (ExistingTaskSupporter) e;
        assert task == o.task : "Comparing two resolvers on different flaws.";
        assert act != o.act;
        return act.id().id() - o.act.id().id();
    }
}

