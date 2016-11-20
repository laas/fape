package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.anml.model.concrete.statements.LogStatement;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import lombok.Value;

import java.util.Arrays;
import java.util.Collection;

@Value
public class SupportRestriction implements StateModification {

    /** Assertion to be supported */
    public final LogStatement assertion;

    /** If not null, support be be provided by a descendant of this task */
    private final Task task;

    /** If not null, support be be provided by a descendant of this action */
    private final Action action;

    public SupportRestriction(LogStatement assertion, Action action) {
        this.assertion = assertion;
        this.action = action;
        this.task = null;
    }

    public SupportRestriction(LogStatement assertion, Task task) {
        this.assertion = assertion;
        this.action = null;
        this.task = task;
    }

    @Override
    public void apply(State st, boolean isFastForwarding) {
        Timeline tl = st.tdb.getTimelineContaining(assertion);
        if(action != null)
            st.getHierarchicalConstraints().setSupportConstraint(tl, action);
        else
            st.getHierarchicalConstraints().setSupportConstraint(tl, task);
    }

    @Override
    public Collection<Object> involvedObjects() {
        if(action != null)
            return Arrays.asList(assertion, action);
        else
            return Arrays.asList(assertion, task);
    }
}
