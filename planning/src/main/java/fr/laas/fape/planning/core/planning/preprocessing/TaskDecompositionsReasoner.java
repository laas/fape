package fr.laas.fape.planning.core.planning.preprocessing;

import fr.laas.fape.anml.model.AnmlProblem;
import fr.laas.fape.anml.model.LActRef;
import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.abs.AbstractTask;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.util.Pair;

import java.util.*;

/**
 * This class is used to provide information on the possible outcomes of a decomposition.
 * More specifically, it provides methods to find which decompositions of an action might produce other actions.
 *
 * In the current state, no preprocessing is done and analyse is repeated on every method invocation.
 */
public class TaskDecompositionsReasoner {

    final AnmlProblem pb;

    /**
     * Creates a new ActionDecompositions tied to an AnmlProblem
     * @param pb Problem to be inspected.
     */
    TaskDecompositionsReasoner(AnmlProblem pb) {
        this.pb = pb;
    }

    /**
     * Return true if the action aa can be refined into an action in targets.
     * Actions in alreadyChecked are those for which we know they cannot be refined into on of targets.
     */
    public boolean canBeRefinedInto(AbstractAction aa, Collection<AbstractAction> targets, Collection<AbstractAction> alreadyChecked) {
        if(targets.contains(aa))
            return true;
        if(alreadyChecked.contains(aa))
            return false;

        alreadyChecked.add(aa);
        for(AbstractTask subTask : aa.jSubTasks()) {
            for(AbstractAction derivableAction : pb.getSupportersForTask(subTask.name())) {
                if(canBeRefinedInto(derivableAction, targets, alreadyChecked))
                    return true;
            }
        }
        return false;
    }

    /**
     * Returns a set of method that might produce one of the actions in targets.
     * @param task The action whose decompositions will be inspected.
     * @param targets Actions to look for
     * @return All actions supporting the task that can be refined into an action in targets.
     */
    public Collection<AbstractAction> possibleMethodsToDeriveTargetActions(Task task, Collection<AbstractAction> targets) {
        Set<AbstractAction> actions = new HashSet<>();

        for(AbstractAction supportingAction : pb.getSupportersForTask(task.name())) {
            if(canBeRefinedInto(supportingAction, targets, new HashSet<>()))
                actions.add(supportingAction);
        }

        //TODO: this sgould be transfered to tasks
        return actions;
    }

    /**
     * Lookup a partial set of resolvers for a motivated action a.
     *
     * Retrieves a tuple (abs:AbstractAction, decompositionID:integer, actRef:LocalActionReference),
     * where abs must be instantiated and inserted into the plan.
     *  - Its decomposition number decompositionID must then be performed. If decompositionID == -1,
     *  it means that the supporting actionReference is the body of the action itself. Hence no decomposition is needed.
     * The actRef gives the ID of a task condition that is approprate to support a.
     * @param a An action that need support (it is motivated and must be part of decomposition)
     */
    public List<Pair<AbstractAction, LActRef>> supportersForMotivatedAction(Action a) {
        List<Pair<AbstractAction, LActRef>> supporters = new LinkedList<>();
        for(AbstractAction abs : pb.abstractActions()) {
            for(AbstractTask actRef : abs.jSubTasks()) {
                if(actRef.name().equals(a.taskName()))
                    supporters.add(new Pair<>(abs, actRef.localId()));
            }
        }
        return supporters;
    }
}
