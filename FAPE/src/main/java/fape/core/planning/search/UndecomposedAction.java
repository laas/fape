package fape.core.planning.search;

import planstack.anml.model.concrete.Action;

public class UndecomposedAction extends Flaw {

    public final Action action;

    public UndecomposedAction(Action a) {
        this.action = a;
    }

    @Override
    public String toString() {
        return "Undecomposed: " + action;
    }
}
