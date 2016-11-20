package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.states.State;
import lombok.Value;

import java.util.Arrays;
import java.util.Collection;

@Value
public class TaskRefinement implements StateModification {

    public final Task task;
    public final Action refiningAction;

    @Override
    public void apply(State st, boolean isFastForwarding) {
        st.addSupport(task, refiningAction);

        // if we had a choice between different resolvers, record which decomposition number we chose
        if(!isFastForwarding)
            st.setLastDecompositionNumber(refiningAction.abs().decID());
    }

    @Override
    public Collection<Object> involvedObjects() {
        return Arrays.asList(task, refiningAction);
    }
}
