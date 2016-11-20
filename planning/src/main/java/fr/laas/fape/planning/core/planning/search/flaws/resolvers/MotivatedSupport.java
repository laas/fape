package fr.laas.fape.planning.core.planning.search.flaws.resolvers;

import fr.laas.fape.anml.model.LActRef;
import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Factory;
import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.State;

/**
 * Given an action marked as "motivated" to support, this resolvers select an action condition
 * in the given supporting action and unifies it with the one to support.
 *
 * If no existing action is given, a new one will be instantiated from "abs" and will be inserted in
 * the plan.
 */
public class MotivatedSupport implements Resolver {

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
     * ID of the action condition we want for support.
     * It might be in the main action or in one of its decomposition if decID != -1
     */
    private final LActRef actRef;

    /** The motivated action that should be supported. */
    private final Action toSupport;

    public MotivatedSupport(Action toSupport, Action act, LActRef actRef) {
        assert act != null;
        this.toSupport = toSupport;
        this.act = act;
        this.abs = null;
        this.actRef = actRef;
    }

    public MotivatedSupport(Action toSupport, AbstractAction abs, LActRef actRef) {
        assert abs != null;
        this.toSupport = toSupport;
        this.act = null;
        this.abs = abs;
        this.actRef = actRef;
    }

    @Override
    public String toString() {
        if(act == null)
            return "new: "+abs.toString()+":"+actRef;
        else
            return "existing: "+act+":"+actRef;
    }

    public boolean apply(State st, Planner planner, boolean isFastForwarding) {
        assert toSupport.mustBeMotivated();

        // action that will be decomposed. Either it is already in the plan or we add it now
        if(act == null) {
            act = Factory.getStandaloneAction(st.pb, abs, st.refCounter);
            st.insert(act);
        }

        // Look for the action condition with ID actRef
        Task ac = act.context().tasks().apply(actRef);

        // add equality constraint between all args
        for (int i = 0; i < ac.args().size(); i++) {
            st.addUnificationConstraint(toSupport.args().get(i), ac.args().get(i));
        }
        //enforce equality of time points and add the support in the task network
        st.addSupport(ac, toSupport);

        return true;
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof MotivatedSupport;
        MotivatedSupport o = (MotivatedSupport) e;
        assert toSupport == o.toSupport : "Comparing resolvers on different flaws.";

        if(act != null && o.act == null)
            return -1;
        if(act == null && o.act != null)
            return 1;
        if(act != null && o.act != null && act != o.act)
            return act.id().id() - o.act.id().id();

        if(abs != null && o.abs == null)
            return -1;
        if(abs == null && o.abs != null)
            return 1;
        if(abs != null && o.abs != null && abs != o.abs)
            return abs.name().compareTo(o.abs.name());

        assert actRef != o.actRef;
        return actRef.id().compareTo(o.actRef.id());
    }
}
