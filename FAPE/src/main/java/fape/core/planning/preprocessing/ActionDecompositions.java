package fape.core.planning.preprocessing;

import planstack.anml.model.AnmlProblem;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractActionRef;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.Action;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is used to provide information on the possible outcomes of a decomposition.
 * More specifically, it provides methods to find which decompositions of an action might produce other actions.
 *
 * In the current state, no processing is done and analyse is repeated on every method invocation.
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

        for(int i=0 ; i<act.jDecompositions().size() ; i++) {
            if(mightContains(act.jDecompositions().get(i), targets)) {
                decompositionIDs.add(i);
            }
        }

        return decompositionIDs;
    }

    /**
     * Recursively look into a decomposition to see if it might produce an action.
     * @param dec Decomposition to inspect.
     * @param targets Actions to look for.
     * @return True if any action in targets can appear in this decomposition or its children. False otherwise
     */
    private boolean mightContains(AbstractDecomposition dec, Collection<AbstractAction> targets) {
        for(AbstractActionRef actRef : dec.jActions()) {
            AbstractAction abs = pb.getAction(actRef.name());
            if(targets.contains(abs)) {
                return true;
            }

            for(AbstractDecomposition nextDec : abs.jDecompositions()) {
                if(mightContains(nextDec, targets)) {
                    return true;
                }
            }
        }
        return false;
    }
}
