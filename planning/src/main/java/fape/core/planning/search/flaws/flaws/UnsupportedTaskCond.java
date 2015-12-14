package fape.core.planning.search.flaws.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.resolvers.ExistingTaskSupporter;
import fape.core.planning.search.flaws.resolvers.NewTaskSupporter;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.states.State;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;

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

    public final Task task;

    public UnsupportedTaskCond(Task ac) { task = ac; }

    @Override
    public List<Resolver> getResolvers(State st, APlanner planner) {
        if (resolvers != null)
            return resolvers;

        resolvers = new LinkedList<>();

        // inserting a new action is always a resolver.
        for(AbstractAction abs : st.pb.getSupportersForTask(task.name()))
            if(st.isAddable(abs))
                resolvers.add(new NewTaskSupporter(task, abs));

        if(!planner.isTopDownOnly())
            for (Action act : st.getAllActions()) {
                if ((planner.options.actionsSupportMultipleTasks || !st.taskNet.isSupporting(act))
                        && st.canSupport(act, task))
                    resolvers.add(new ExistingTaskSupporter(task, act));
            }

        return resolvers;
    }

    @Override
    public int compareTo(Flaw o) {
        assert o instanceof UnsupportedTaskCond;
        return ((UnsupportedTaskCond) o).task.start().id() - task.start().id();
    }
}
