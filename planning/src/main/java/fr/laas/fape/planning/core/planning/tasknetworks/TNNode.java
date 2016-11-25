package fr.laas.fape.planning.core.planning.tasknetworks;

import fr.laas.fape.planning.exceptions.FAPEException;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Task;

/**
 * Represents a node of task network that can be either an action or a decomposition.
 *
 * An action is considered decomposed if it has at least one decomposition child.
 */
public class TNNode {

    protected final Action act;
    protected final Task actCond;

    public TNNode(Action action) {
        assert action != null;
        this.act = action;
        actCond = null;
    }

    public TNNode(Task actCond) {
        assert actCond != null;
        this.actCond = actCond;
        this.act = null;
    }

    public boolean isAction() {
        return act != null;
    }

    public boolean isTask() {
        return actCond != null;
    }

    @Override
    public String toString() {
        if(isAction()) return asAction().toString();
        else if(isTask()) return asTask().toString();
        else throw new FAPEException("Uncomplete switch");
    }

    public Action asAction() {
        assert isAction() : "This node is not an action.";
        return act;
    }

    public Task asTask() {
        assert isTask() : "This node is not an action condition.";
        return actCond;
    }

    @Override
    public int hashCode() {
        if(isAction())
            return asAction().hashCode();
        else
            return asTask().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof TNNode) {
            return ((TNNode) o).act == act && ((TNNode) o).actCond == actCond;
        } else if(o instanceof Action) {
            return isAction() && act == o;
        } else if(o instanceof Task) {
            return isTask() && actCond == o;
        } else {
            return false;
        }
    }

}
