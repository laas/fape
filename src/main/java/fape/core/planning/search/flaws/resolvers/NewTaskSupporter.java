package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;
import planstack.anml.model.concrete.Factory;

/**
 * Inserts a new action to support an action condition.
 */
public class NewTaskSupporter extends Resolver {

    /** Action condition to support */
    public final Task condition;

    /** Abstract action to be instantiated and inserted. */
    public final AbstractAction abs;

    public NewTaskSupporter(Task cond, AbstractAction abs) {
        this.condition = cond;
        this.abs = abs;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        // create a new action with the same args as the condition
        Action act = Factory.getInstantiatedAction(st.pb, abs, condition.args(), st.refCounter);
        st.insert(act);

        // enforce equality of time points and add support to task network
        st.addSupport(condition, act);

        return true;
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof NewTaskSupporter;
        NewTaskSupporter o = (NewTaskSupporter) e;
        assert condition == o.condition : "Comparing two resolvers on different flaws.";
        assert abs != o.abs : "Comparing two identical resolvers.";
        return abs.name().compareTo(o.abs.name());
    }
}
