package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.states.PartialPlan;

import java.util.Arrays;
import java.util.Collection;

public class TaskRefinement implements PartialPlanModification {

    public final Task task;
    public final Action refiningAction;

    public TaskRefinement(Task task, Action refiningAction) {
        this.task = task;
        this.refiningAction = refiningAction;
    }

    @Override
    public void apply(PartialPlan plan, boolean isFastForwarding) {
        plan.addSupport(task, refiningAction);

        // if we had a choice between different resolvers, record which decomposition number we chose
        if(!isFastForwarding)
            plan.setLastDecompositionNumber(refiningAction.abs().decID());
    }

    @Override
    public Collection<Object> involvedObjects() {
        return Arrays.asList(task, refiningAction);
    }

    public Task getTask() {
        return this.task;
    }

    public Action getRefiningAction() {
        return this.refiningAction;
    }

    public String toString() {
        return "TaskRefinement(task=" + this.getTask() + ", refiningAction=" + this.getRefiningAction() + ")";
    }
}
