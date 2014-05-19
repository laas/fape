package fape.core.planning.tasknetworks;

import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Decomposition;

/**
 * Represents a node of task network that can be either an action or a decomposition.
 *
 * An action is considered decomposed if it has at least one decomposition child.
 */
public class TNNode {

    protected final Action act;
    protected final Decomposition dec;

    public TNNode(Action action) {
        assert action != null;
        this.act = action;
        dec = null;
    }

    public TNNode(Decomposition decomposition) {
        assert decomposition != null;
        this.dec = decomposition;
        act = null;
    }

    public boolean isDecomposition() {
        return dec != null;
    }

    public boolean isAction() {
        return act != null;
    }

    public Action asAction() {
        assert isAction() : "This node is not an action.";
        return act;
    }

    public Decomposition asDecomposition() {
        assert isDecomposition() : "This node is not a decomposition";
        return dec;
    }

    @Override
    public int hashCode() {
        if(isAction())
            return act.hashCode();
        else
            return 42 * dec.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof TNNode) {
            return ((TNNode) o).act == act && ((TNNode) o).dec == dec;
        } else if(o instanceof Decomposition) {
            return isDecomposition() && dec == o;
        } else if(o instanceof Action) {
            return isAction() && act == o;
        } else {
            return false;
        }
    }

}
