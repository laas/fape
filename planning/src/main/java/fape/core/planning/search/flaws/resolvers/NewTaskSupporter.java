package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.Planner;
import fape.core.planning.states.State;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Factory;
import planstack.anml.model.concrete.Task;

/**
 * Inserts a new action to support an action condition.
 */
public class NewTaskSupporter implements Resolver {

    /** Action condition to support */
    public final Task unrefined;

    /** Abstract action to be instantiated and inserted. */
    public final AbstractAction abs;

    public NewTaskSupporter(Task unrefinedTask, AbstractAction abs) {
        this.unrefined = unrefinedTask;
        this.abs = abs;
    }

    @Override
    public boolean apply(State st, Planner planner, boolean isFastForwarding) {
        // create a new action with the same args as the condition
        Action act = Factory.getStandaloneAction(st.pb, abs, st.refCounter);
        st.insert(act);

        // enforce equality of time points and add support to task network
        st.addSupport(unrefined, act);

        if(!isFastForwarding)
            st.setLastDecompositionNumber(abs.decID());


        return true;
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof NewTaskSupporter;
        NewTaskSupporter o = (NewTaskSupporter) e;
        assert unrefined == o.unrefined : "Comparing two resolvers on different flaws.";
        assert abs != o.abs : "Comparing two identical resolvers.";
        return abs.name().compareTo(o.abs.name());
    }
}
