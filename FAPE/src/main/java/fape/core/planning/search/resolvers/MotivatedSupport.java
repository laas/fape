package fape.core.planning.search.resolvers;

import planstack.anml.model.LActRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;

public class MotivatedSupport extends Resolver {

    public final AbstractAction abs;
    public final Action act;

    /**
     * Id of the decomposition in which the action reference appears. If set to -1, it means that it appears
     * in the main body of the action.
     */
    public final int decID;
    public final LActRef actRef;

    public final Action toSupport;

    public MotivatedSupport(Action toSupport, Action act, int decID, LActRef actRef) {
        this.toSupport = toSupport;
        this.act = act;
        this.abs = null;
        this.decID = decID;
        this.actRef = actRef;
    }

    public MotivatedSupport(Action toSupport, AbstractAction abs, int decID, LActRef actRef) {
        this.toSupport = toSupport;
        this.act = null;
        this.abs = abs;
        this.decID = decID;
        this.actRef = actRef;
    }

    @Override
    public String toString() {
        return abs.toString()+":"+decID+":"+actRef;
    }
}
