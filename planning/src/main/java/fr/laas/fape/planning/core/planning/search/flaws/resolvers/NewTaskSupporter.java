package fr.laas.fape.planning.core.planning.search.flaws.resolvers;

import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Factory;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.states.PartialPlan;
import fr.laas.fape.planning.core.planning.states.modification.ActionInsertion;
import fr.laas.fape.planning.core.planning.states.modification.SequenceOfPartialPlanModifications;
import fr.laas.fape.planning.core.planning.states.modification.PartialPlanModification;
import fr.laas.fape.planning.core.planning.states.modification.TaskRefinement;

import java.util.Arrays;

/**
 * Inserts a new action to support an unrefined task
 */
public class NewTaskSupporter implements Resolver {

    /** task to support */
    public final Task unrefined;

    /** Abstract action to be instantiated and inserted. */
    public final AbstractAction abs;

    public NewTaskSupporter(Task unrefinedTask, AbstractAction abs) {
        this.unrefined = unrefinedTask;
        this.abs = abs;
    }

    @Override
    public PartialPlanModification asStateModification(PartialPlan partialPlan) {
        Action action = Factory.getStandaloneAction(partialPlan.pb, abs, partialPlan.refCounter);
        return new SequenceOfPartialPlanModifications(Arrays.asList(
                new ActionInsertion(action),
                new TaskRefinement(unrefined, action)));
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof NewTaskSupporter;
        NewTaskSupporter o = (NewTaskSupporter) e;
        assert unrefined == o.unrefined : "Comparing two resolvers on different flaws.";
        assert abs != o.abs : "Comparing two identical resolvers.";
        return abs.name().compareTo(o.abs.name());
    }
}
