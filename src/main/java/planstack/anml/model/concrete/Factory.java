package planstack.anml.model.concrete;

import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LActRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractDecomposition;

import java.util.List;

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
    public static Action getStandaloneAction(AnmlProblem pb, AbstractAction abs, RefCounter refCounter) {
        return Action$.MODULE$.getNewStandaloneAction(pb, abs, refCounter);
    }

    /**
     * Creates a new action with some predefined arguments.
     * @param pb Problem in which the action appears.
     * @param abs AbstractAction to make concrete.
     * @param parameters List of parameters.
     * @return A concrete action with the given parameters.
     */
    public static Action getInstantiatedAction(AnmlProblem pb, AbstractAction abs, List<VarRef> parameters, RefCounter refCounter) {
        return Action$.MODULE$.jNewAction(pb, abs, parameters, new LActRef(), refCounter, null, null);
    }

    /**
     * Creates a new action with some predefined arguments and an identifier.
     * Do not define an identifier yourself unless you know what you're doing.
     * @param pb Problem in which the action appears.
     * @param abs AbstractAction to make concrete.
     * @param parameters List of parameters.
     * @param id ID of the action
     * @return A concrete action with the given parameters.
     *
    public static Action getInstantiatedAction(AnmlProblem pb, AbstractAction abs, List<VarRef> parameters, ActRef id) {
        return Action$.MODULE$.jNewAction(pb, abs, parameters, new LActRef(), id, null, null);
    }*/

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
    public static Decomposition getDecomposition(AnmlProblem pb, Action parent, AbstractDecomposition abs, RefCounter refCounter) {
        return Decomposition$.MODULE$.apply(pb, parent, abs, refCounter);
    }

}
