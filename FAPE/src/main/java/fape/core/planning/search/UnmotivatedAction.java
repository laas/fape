package fape.core.planning.search;

import planstack.anml.model.concrete.Action;

public class UnmotivatedAction extends Flaw {

    public final Action act;

    public UnmotivatedAction(Action act) {
        this.act = act;
    }

    @Override
    public String toString() {
        return "Unmotivated: "+act.toString();
    }
}
