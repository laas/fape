package fr.laas.fape.planning.core.planning.search.flaws.resolvers;


import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Factory;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.Timeline;

public class SupportingTaskDecomposition implements Resolver {

    public final Task task;
    public final AbstractAction abs;
    public final Timeline tl;

    public SupportingTaskDecomposition(Task task, AbstractAction method, Timeline consumer) {
        this.task = task;
        this.abs = method;
        this.tl = consumer;
    }

    @Override
    public boolean apply(State st, Planner planner, boolean isFastForwarding) {
        // create a new action with the same args as the condition
        Action act = Factory.getStandaloneAction(st.pb, abs, st.refCounter);
        st.insert(act);

        // enforce equality of time points and arguments and add support to task network
        st.addSupport(task, act);

        // make sure this open goal can only be solved by a statement from this action or one of its child
        st.addSupportConstraint(tl.getFirst(), act);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
