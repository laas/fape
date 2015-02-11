package fape.core.planning.search.flaws.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.resolvers.ExistingTaskSupporter;
import fape.core.planning.search.flaws.resolvers.NewTaskSupporter;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionCondition;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a task condition that is not supported by any action.
 *
 * Resolvers can be
 *  - unification with an existing action
 *  - insertion of a new action to support it
 */
public class UnsupportedTaskCond extends Flaw {

    public final ActionCondition actCond;

    public UnsupportedTaskCond(ActionCondition ac) { actCond = ac; }

    @Override
    public List<Resolver> getResolvers(State st, APlanner planner) {
        if (resolvers != null)
            return resolvers;

        resolvers = new LinkedList<>();

        // inserting a new action is always a resolver.
        resolvers.add(new NewTaskSupporter(actCond, actCond.abs()));

        // existing action can be a supporter if every one of its parameters
        // are unifiable with the task condition
        for (Action act : st.getAllActions()) {
            if (act.abs() == actCond.abs()) {
                boolean unifiable = true;
                for (int i = 0; i < act.args().size(); i++) {
                    unifiable &= st.unifiable(act.args().get(i), actCond.args().get(i));
                }
                unifiable &= st.canBeBefore(act.start(), actCond.start());
                unifiable &= st.canBeBefore(actCond.start(), act.start());
                unifiable &= st.canBeBefore(act.end(), actCond.end());
                unifiable &= st.canBeBefore(actCond.end(), act.end());
                if (unifiable)
                    resolvers.add(new ExistingTaskSupporter(actCond, act));
            }
        }
        return resolvers;
    }
}
