package fape.core.planning.preprocessing;

import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LActRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractTask;
import planstack.anml.model.abs.AbstractDecomposition;
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

        for(int i=0 ; i<act.jDecompositions().size() ; i++) {
            if(mightContains(act.jDecompositions().get(i), targets, new LinkedList<AbstractAction>())) {
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
    private boolean mightContains(AbstractDecomposition dec, Collection<AbstractAction> targets, List<AbstractAction> treated) {

        for(AbstractTask actRef : dec.jActions()) {

            AbstractAction abs = pb.getAction(actRef.name());
            if(treated.contains(abs))
                continue;
            else
                treated.add(abs);
            if(targets.contains(abs)) {
                return true;
            }
            for(AbstractTask ref : abs.jActions()) {
                AbstractAction subAct = pb.getAction(ref.name());
                if(targets.contains(subAct))
                    return true;
            }

            for(AbstractDecomposition nextDec : abs.jDecompositions()) {
                if(mightContains(nextDec, targets, treated)) {
                    return true;
                }
            }
        }
        return false;
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
    public List<Tuple3<AbstractAction, Integer, LActRef>> supporterForMotivatedAction(Action a) {
        List<Tuple3<AbstractAction, Integer, LActRef>> supporters = new LinkedList<>();
        for(AbstractAction abs : pb.abstractActions()) {
            for(AbstractTask actRef : abs.jActions()) {
                if(actRef.name().equals(a.taskName()))
                    supporters.add(new Tuple3<AbstractAction, Integer, LActRef>(abs, -1, actRef.localId()));
            }
            for(int decID=0 ; decID<abs.jDecompositions().size() ; decID++) {
                AbstractDecomposition dec = abs.jDecompositions().get(decID);
                for(AbstractTask actRef : dec.jActions()) {
                    if(actRef.name().equals(a.taskName())) {
                        supporters.add(new Tuple3<AbstractAction, Integer, LActRef>(abs, decID, actRef.localId()));
                    }
                }
            }
        }
        return supporters;
    }

    /**
     * Lookup a partial set of resolvers for a motivated action a.
     *
     * Given an undecomposed action "supporting", it
     * retrieves a pair (decompositionID:integer, actRef:LocalActionReference). "supporting" must
     * be decomposition with decomposition number decompositionID.
     * must then be performed. The actRef gives the ID of a task condition that is appropriate to support a.
     * @param consuming An action that need support (it is motivated and must be part of decomposition)
     * @param supporting An action in the plan that can be decomposed to provide a task condition matching
     *                   the consuming action.
     */
    public List<Tuple2<Integer, LActRef>> supporterForMotivatedAction(Action supporting, Action consuming) {
        List<Tuple2<Integer, LActRef>> supporters = new LinkedList<>();
        for(int decID=0 ; decID<supporting.abs().jDecompositions().size() ; decID++) {
            AbstractDecomposition dec = supporting.abs().jDecompositions().get(decID);
            for(AbstractTask actRef : dec.jActions()) {
                if(actRef.name().equals(consuming.name())) {
                    supporters.add(new Tuple2<Integer, LActRef>(decID, actRef.localId()));
                }
            }
        }
        return supporters;
    }
}
