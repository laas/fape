package fape.core.planning.search;

import fape.core.planning.model.Action;

public class UndecomposedAction extends Flaw {

    public final Action action;

    public UndecomposedAction(Action a) {
        this.action = a;
    }
}
