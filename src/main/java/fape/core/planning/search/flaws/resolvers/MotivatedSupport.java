package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.PlanningGraphReachability;
import fape.core.planning.states.State;
import planstack.anml.model.LActRef;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Given an action marked as "motivated" to support, this resolvers select an action condition
 * in the given supporting action and unifies it with the one to support.
 *
 * If no existing action is given, a new one will be instantiated from "abs" and will be inserted in
 * the plan.
 */
public class MotivatedSupport extends Resolver {

    /**
     *  null if no action needs to be inserted.
     *  Otherwise the action should be created from this abstract action.
     */
    public final AbstractAction abs;

    /**
     * Action providing the support. If null, it should be created by instantiating abs.
     */
    public Action act;

    /**
     * Id of the decomposition in which the action reference appears. If set to -1, it means that it appears
     * in the main body of the action.
     */
    public final int decID;

    /**
     * ID of the action condition we want for support.
     * It might be in the main action or in one of its decomposition if decID != -1
     */
    public final LActRef actRef;

    /** The motivated action that should be supported. */
    public final Action toSupport;

    public MotivatedSupport(Action toSupport, Action act, int decID, LActRef actRef) {
        assert act != null;
        this.toSupport = toSupport;
        this.act = act;
        this.abs = null;
        this.decID = decID;
        this.actRef = actRef;
    }

    public MotivatedSupport(Action toSupport, AbstractAction abs, int decID, LActRef actRef) {
        assert abs != null;
        this.toSupport = toSupport;
        this.act = null;
        this.abs = abs;
        this.decID = decID;
        this.actRef = actRef;
    }

    @Override
    public String toString() {
        if(act == null)
            return "new: "+abs.toString()+":"+decID+":"+actRef;
        else
            return "existing: "+act+":"+decID+":"+actRef;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        assert toSupport.mustBeMotivated();
        assert planner.useActionConditions() :
                "Error: looking for motivated support in a planner that does not use action conditions.";

        // action that will be decomposed. Either it is already in the plan or we add it now
        if(act == null) {
            act = Factory.getStandaloneAction(st.pb, abs);
            st.insert(act);
        }

        // Look for the action condition with ID actRef
        ActionCondition ac;
        if(decID == -1) {
            // the action condition is directly in the main body
            ac = act.context().actionConditions().apply(actRef);
        } else {
            // we need to make one decomposition
            // decompose the action with the given decomposition ID
            AbstractDecomposition absDec = act.decompositions().get(decID);
            Decomposition dec = Factory.getDecomposition(st.pb, act, absDec);
            st.applyDecomposition(dec);

            // Get the action condition we wanted
            ac = dec.context().actionConditions().apply(actRef);
        }

        // add equality constraint between all args
        for (int i = 0; i < ac.args().size(); i++) {
            st.addUnificationConstraint(toSupport.args().get(i), ac.args().get(i));
        }
        //enforce equality of time points and add the support in the task network
        st.addSupport(ac, toSupport);

        return true;
    }
}
