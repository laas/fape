package fr.laas.fape.planning.core.planning.states;

import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.tasknetworks.TNNode;
import fr.laas.fape.planning.core.planning.timelines.Timeline;

import java.util.*;
import java.util.stream.Collectors;

public class HierarchicalConstraints implements StateExtension {
    private final Map<Integer, TNNode> timelineSupportConstraints;
    private final State st;
    private final Map<Task, Set<AbstractAction>> possibleRefinements;

    public HierarchicalConstraints(State st) {
        this.st = st;
        timelineSupportConstraints = new HashMap<>();
        possibleRefinements = new HashMap<>();
    }

    public HierarchicalConstraints(HierarchicalConstraints toCopy, State st) {
        this.st = st;
        timelineSupportConstraints = new HashMap<>(toCopy.timelineSupportConstraints);
        possibleRefinements = new HashMap<>(toCopy.possibleRefinements);
    }

    public boolean isConstrained(Timeline tl) {
        return timelineSupportConstraints.containsKey(tl.mID);
    }

    public void setSupportConstraint(Timeline consumer, Action a) {
        assert !isConstrained(consumer) || st.taskNet.isDescendantOf(a, timelineSupportConstraints.get(consumer.mID));
        timelineSupportConstraints.put(consumer.mID, new TNNode(a));
    }

    public void setSupportConstraint(Timeline consumer, Task task) {
        assert !isWaitingForADecomposition(consumer);
        assert !isConstrained(consumer)
                || st.taskNet.isDescendantOf(task, timelineSupportConstraints.get(consumer.mID));

        st.enforceStrictlyBefore(task.start(), consumer.getConsumeTimePoint());
        timelineSupportConstraints.put(consumer.mID, new TNNode(task));

        // all actions with a statements affecting this state variable of the consumer
        Collection<AbstractAction> potentiallySupportingAction =
                st.pl.getActionSupporterFinder().getActionsSupporting(st, consumer).stream()
                .map(x -> x.absAct)
                .collect(Collectors.toSet());

        Collection<AbstractAction> decs = st.pl.preprocessor.getTaskDecompositionsReasoner()
                .possibleMethodsToDeriveTargetActions(task, potentiallySupportingAction);

        HashSet<AbstractAction> possibleSupportersForTask = new HashSet<>(decs);
        possibleSupportersForTask.retainAll(getPossibleRefinements(task));

        if(possibleSupportersForTask.size() < getPossibleRefinements(task).size())
            possibleRefinements.put(task, Collections.unmodifiableSet(possibleSupportersForTask));
    }

    public boolean isValidTaskSupport(Task t, Timeline consumer) {
        if(!isConstrained(consumer))
            return true;
        else
            return st.taskNet.isDescendantOf(t, timelineSupportConstraints.get(consumer.mID));
    }

    public boolean isWaitingForADecomposition(Timeline consumer) {
        if(isConstrained(consumer)) {
            TNNode n = timelineSupportConstraints.get(consumer.mID);
            if(n.isTask())
                return !st.taskNet.isSupported(n.asActionCondition());
            else
                return false;
        } else
            return false;
    }

    public Set<AbstractAction> getPossibleRefinements(Task t) {
        return possibleRefinements.computeIfAbsent(t, task -> Collections.unmodifiableSet(new HashSet<>(st.pb.getSupportersForTask(task.name()))));
    }

    @Override
    public StateExtension clone(State containingState) {
        return new HierarchicalConstraints(this, containingState);
    }
}
