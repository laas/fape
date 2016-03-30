package fape.core.planning.states;

import fape.core.planning.grounding.GAction;
import fape.core.planning.tasknetworks.TNNode;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.HashMap;
import java.util.Map;

public class HierarchicalConstraints implements StateExtension {

    private final Map<Integer, Task> timelineSupportConstraints;
    private final State st;

    public HierarchicalConstraints(State st) {
        this.st = st;
        timelineSupportConstraints = new HashMap<>();
    }

    public HierarchicalConstraints(HierarchicalConstraints toCopy, State st) {
        this.st = st;
        timelineSupportConstraints = new HashMap<>(toCopy.timelineSupportConstraints);
    }

    public boolean isConstrained(Timeline tl) {
        return timelineSupportConstraints.containsKey(tl.mID);
    }

    public void setSupportConstraint(Timeline consumer, Task task) {
        assert !isConstrained(consumer) || st.taskNet.isSupported(timelineSupportConstraints.get(consumer.mID));
        st.enforceStrictlyBefore(task.start(), consumer.getConsumeTimePoint());
        timelineSupportConstraints.put(consumer.mID, task);
    }

    public boolean isValidSupport(LogStatement supporter, Timeline consumer) {
        if(!timelineSupportConstraints.containsKey(consumer.mID))
            return true;

        if(st.getActionContaining(supporter) == null)
            return false;

        Task t = timelineSupportConstraints.get(consumer.mID); // any supporter must be derived from this task
        Action a = st.getActionContaining(supporter); // the action that introduced the statement

        return st.taskNet.isDescendantOf(a, t);
    }

    public boolean isValidTaskSupport(Task t, Timeline consumer) {
        if(!isConstrained(consumer))
            return true;
        else
            return st.taskNet.isDescendantOf(t, timelineSupportConstraints.get(consumer.mID));
    }


    public boolean isWaitingForADecomposition(Timeline consumer) {
        if(isConstrained(consumer))
            return !st.taskNet.isSupported(timelineSupportConstraints.get(consumer.mID));
        else
            return false;
    }

    @Override
    public StateExtension clone(State containingState) {
        return new HierarchicalConstraints(this, containingState);
    }
}
