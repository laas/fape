package fr.laas.fape.planning.core.planning.search.flaws.resolvers;


import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.states.modification.StateModification;
import fr.laas.fape.planning.core.planning.states.modification.TaskRefinement;

/**
 * Mark an action (already in the plan) as supporting an action condition.
 */
public class ExistingTaskSupporter implements Resolver {

    /** Unsupported task */
    public final Task task;

    /** Supporting action. */
    public final Action act;

    public ExistingTaskSupporter(Task cond, Action act) {
        this.task = cond;
        this.act = act;
    }

    @Override
    public StateModification asStateModification(State state) {
        return new TaskRefinement(task, act);
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof ExistingTaskSupporter;
        ExistingTaskSupporter o = (ExistingTaskSupporter) e;
        assert task == o.task : "Comparing two resolvers on different flaws.";
        assert act != o.act;
        return act.id().id() - o.act.id().id();
    }
}

