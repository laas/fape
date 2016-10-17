package fr.laas.fape.planning.core.planning.search.flaws.flaws;

import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.ExistingTaskSupporter;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.NewTaskSupporter;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.Resolver;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a task condition that is not supported by any action.
 *
 * Resolvers can be
 *  - unification with an existing action
 *  - insertion of a new action to support it
 */
public class UnrefinedTask extends Flaw {

    public final Task task;

    public UnrefinedTask(Task ac) { task = ac; }

    @Override
    public List<Resolver> getResolvers(State st, Planner planner) {
        if (resolvers != null)
            return resolvers;

        resolvers = new LinkedList<>();

        // inserting a new action is always a resolver.
        for(AbstractAction abs : st.getHierarchicalConstraints().getPossibleRefinements(task))
            if(st.isAddable(abs))
                resolvers.add(new NewTaskSupporter(task, abs));

        for (Action act : st.getAllActions()) {
            if ((planner.options.actionsSupportMultipleTasks || !st.taskNet.isSupporting(act))
                    && st.canSupport(act, task))
                resolvers.add(new ExistingTaskSupporter(task, act));
        }

        return resolvers;
    }

    @Override
    public int compareTo(Flaw o) {
        assert o instanceof UnrefinedTask;
        return ((UnrefinedTask) o).task.start().id() - task.start().id();
    }
}
