package fape.core.planning.search.resolvers;

import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionCondition;

public class ExistingTaskSupporter extends Resolver {

    public final ActionCondition condition;
    public final Action act;

    public ExistingTaskSupporter(ActionCondition cond, Action act) {
        this.condition = cond;
        this.act = act;
    }
}

