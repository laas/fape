package fape.core.planning.search.resolvers;

import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionCondition;

public class NewTaskSupporter extends Resolver {

    public final ActionCondition condition;
    public final AbstractAction abs;

    public NewTaskSupporter(ActionCondition cond, AbstractAction abs) {
        this.condition = cond;
        this.abs = abs;
    }
}
