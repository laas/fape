package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.util.TinyLogger;
import planstack.anml.model.LStatementRef;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Decomposition;
import planstack.anml.model.concrete.Factory;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Statement;

import java.util.Collection;
import java.util.Map;

/**
 * A resolver for an open goal. The supporting statement is brought by a new action to be inserted.
 * Optionally, a decomposition ID might be provided (decID != -1). If this is the case,
 * the supporting statement will be taken from the statements of the decomposition.
 */
public class SupportingAction extends Resolver {

    public final AbstractAction act;
    public final int consumerID;
    public final int decID;
    /** id of the statement used for support */
    public final LStatementRef statementRef;

    public SupportingAction(AbstractAction act, int decID, LStatementRef statementRef, Timeline consumer) {
        this.act = act;
        this.consumerID = consumer.mID;
        this.decID = decID;
        this.statementRef = statementRef;
    }

    @Override
    public String toString() {
        return "Supporting action: "+act+ (decID == -1 ? "" : " with decomposition: "+decID);
    }

    public boolean hasActionInsertion() {
        return true;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        final Timeline consumer = st.getTimeline(consumerID);

        assert consumer != null : "Consumer was not found.";

        Action action = Factory.getStandaloneAction(st.pb, act);
        st.insert(action);

        Decomposition dec = null;
        if(decID != -1) { // supporter is in a decomposition
            // Abstract version of the decomposition
            AbstractDecomposition absDec = action.decompositions().get(decID);

            // Decomposition (ie implementing StateModifier) containing all changes to be made to a search state.
            dec = Factory.getDecomposition(st.pb, action, absDec);
            st.applyDecomposition(dec);
        }
        assert decID == -1 || dec != null;

        // statement that should be supporting our consumer
        Statement supporter;
        if(decID == -1)
            supporter = action.context().getStatement(statementRef);
        else
            supporter = dec.context().getStatement(statementRef);
        assert supporter != null && supporter instanceof LogStatement;

        if(st.canBeEnabler((LogStatement) supporter, consumer)) {
            final Timeline supportingDatabase = st.getDBContaining((LogStatement) supporter);
            // add the causal link
            Resolver opt = new SupportingTimeline(supportingDatabase.mID, supportingDatabase.numChanges()-1, consumer);
            TinyLogger.LogInfo(st, "     [%s] Adding %s", st.mID, opt);
            return opt.apply(st, planner);
        } else {
            // turns out this statement cannot support our database.
            return false;
        }

    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof SupportingAction;
        SupportingAction o = (SupportingAction) e;
        if(act != o.act)
            return act.name().compareTo(o.act.name());

        if(consumerID != o.consumerID)
            return consumerID - o.consumerID;

        if(decID != o.decID)
            return decID - o.decID;

        assert !statementRef.equals(o.statementRef) : "Error: trying to compare to identical resolvers";
        return statementRef.id().compareTo(o.statementRef.id());
    }
}
