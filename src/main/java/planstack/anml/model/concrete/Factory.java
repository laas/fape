package planstack.anml.model.concrete;

import planstack.anml.model.AnmlProblem;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractDecomposition;


/**
 * This class provides static factory method for creating concrete objects from abstract ones.
 *
 * It is mainly a Java-friendly wrapper around the factories defined as Scala companion objects.
 */
public class Factory {

    /**
     * Returns a new action as part of a problem.
     * @param pb Problem in which the action appears
     * @param abs Abstract version of the action.
     * @return A fully instantiated Action
     */
    public static Action getStandaloneAction(AnmlProblem pb, AbstractAction abs) {
        return Action$.MODULE$.getNewStandaloneAction(pb, abs);
    }

    /**
     * Returns a concrete decomposition.
     * A Decomposition implements the StateModifier interface to explicitly define what changes have to
     * be made to a search state in which it is applied.
     *
     * @param pb Problem containing the action/decomposition
     * @param parent Action in which the decomposition appears
     * @param abs Abstract version of the decomposition.
     * @return A fully instantiated Decomposition
     */
    public static Decomposition getDecomposition(AnmlProblem pb, Action parent, AbstractDecomposition abs) {
        return Decomposition$.MODULE$.apply(pb, parent, abs);
    }

}
