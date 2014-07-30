package fape.core.planning.search;

import planstack.anml.model.concrete.ActionCondition;

public class UnsupportedTaskCond extends Flaw {

    public final ActionCondition actCond;

    public UnsupportedTaskCond(ActionCondition ac) { actCond = ac; }
}
