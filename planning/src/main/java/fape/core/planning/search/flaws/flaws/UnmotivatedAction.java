package fape.core.planning.search.flaws.flaws;

import fape.core.planning.planner.Planner;
import fape.core.planning.preprocessing.TaskDecompositionsReasoner;
import fape.core.planning.search.flaws.resolvers.ExistingTaskSupporter;
import fape.core.planning.search.flaws.resolvers.MotivatedSupport;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.states.State;
import fape.util.Pair;
import planstack.anml.model.LActRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;

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
    public List<Resolver> getResolvers(State st, Planner planner) {
        if(resolvers != null)
            return resolvers;

        resolvers = new LinkedList<>();
        assert(st.taskNet.getNumOpenTasks() == st.getOpenTasks().size());

        // any task condition unifiable with act
        for(Task task : st.getOpenTasks()) {
            if(st.canSupport(act, task))
                resolvers.add(new ExistingTaskSupporter(task, act));
        }

        TaskDecompositionsReasoner preproc = st.pl.preprocessor.getTaskDecompositionsReasoner();

        // resolvers: any action we add to the plan and that might provide (through decomposition)
        // a task condition
        for(Pair<AbstractAction, LActRef> insertion : preproc.supportersForMotivatedAction(act)) {
            if(st.isAddable(insertion.value1))
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
