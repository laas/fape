package fape.core.planning.preprocessing;

import fape.util.Pair;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LActRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractTask;
import planstack.anml.model.concrete.Action;
import scala.Tuple2;
import scala.Tuple3;

import java.util.*;

/**
 * This class is used to provide information on the possible outcomes of a decomposition.
 * More specifically, it provides methods to find which decompositions of an action might produce other actions.
 *
 * In the current state, no preprocessing is done and analyse is repeated on every method invocation.
 */
public class ActionDecompositions {

    final AnmlProblem pb;

    /**
     * Creates a new ActionDecompositions tied to an AnmlProblem
     * @param pb Problem to be inspected.
     */
    public ActionDecompositions(AnmlProblem pb) {
        this.pb = pb;
    }

    /**
     * Returns a set of decomposition index that might produce one of the actions in targets.
     * @param act The action whose decompositions will be inspected.
     * @param targets Actions to look for
     * @return Index of all decompositions that might produce on action in target.
     */
    public Collection<Integer> possibleDecompositions(Action act, Collection<AbstractAction> targets) {
        return possibleDecompositions(act.abs(), targets);
    }

    /**
     * Returns a set of decomposition index that might produce one of the actions in targets.
     * @param act The action whose decompositions will be inspected.
     * @param targets Actions to look for
     * @return Index of all decompositions that might produce on action in target.
     */
    public Collection<Integer> possibleDecompositions(AbstractAction act, Collection<AbstractAction> targets) {
        Set<Integer> decompositionIDs = new HashSet<>();

        //TODO: this sgould be transfered to tasks
        return decompositionIDs;
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
    public List<Pair<AbstractAction, LActRef>> supporterForMotivatedAction(Action a) {
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
