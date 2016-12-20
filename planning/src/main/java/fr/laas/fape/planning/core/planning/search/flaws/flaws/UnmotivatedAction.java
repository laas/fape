package fr.laas.fape.planning.core.planning.search.flaws.flaws;

import fr.laas.fape.anml.model.LActRef;
import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.preprocessing.TaskDecompositionsReasoner;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.ExistingTaskSupporter;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.MotivatedSupport;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.Resolver;
import fr.laas.fape.planning.core.planning.states.PartialPlan;
import fr.laas.fape.planning.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * In the "taskcond" setting, represents an action marked as motivated in the domain
 * which does not support any task condition yet.
 *
 * Possible resolvers are to:
 *  - unify it with any existing task condition
 *  - add an action that provides the task condition and unify it
 *    (this step can also require decomposing the action)
 *  - decompose an existing action to insert the task condition
 */
public class UnmotivatedAction extends Flaw {

    public final Action act;

    public UnmotivatedAction(Action act) {
        assert act.mustBeMotivated();
        this.act = act;
    }

    @Override
    public String toString() {
        return "Unmotivated: "+act.toString();
    }

    @Override
    public List<Resolver> getResolvers(PartialPlan plan, Planner planner) {
        if(resolvers != null)
            return resolvers;

        resolvers = new LinkedList<>();
        assert(plan.taskNet.getNumOpenTasks() == plan.getOpenTasks().size());

        // any task condition unifiable with act
        for(Task task : plan.getOpenTasks()) {
            if(plan.canSupport(act, task))
                resolvers.add(new ExistingTaskSupporter(task, act));
        }

        TaskDecompositionsReasoner preproc = plan.pl.preprocessor.getTaskDecompositionsReasoner();

        // resolvers: any action we add to the plan and that might provide (through decomposition)
        // a task condition
        for(Pair<AbstractAction, LActRef> insertion : preproc.supportersForMotivatedAction(act)) {
            if(plan.isAddable(insertion.value1))
                resolvers.add(new MotivatedSupport(act, insertion.value1, insertion.value2));
        }

        return resolvers;
    }

    @Override
    public int compareTo(Flaw o) {
        assert o instanceof UnmotivatedAction;
        return ((UnmotivatedAction) o).act.id().id() - act.id().id();
    }
}
