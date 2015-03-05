package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.util.TinyLogger;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Decomposition;
import planstack.anml.model.concrete.Factory;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Statement;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A resolver for an open goal. The supporting statement is in a new action to be inserted.
 * Optionally, a decomposition ID might be provided (decID != -1). If this is the case,
 * the supporting statement will be taken from the statements of the decomposition.
 */
public class SupportingAction extends Resolver {

    public final AbstractAction act;
    public final Map<LVarRef, Collection<String>> values;
    public final int consumerID;
    public final int decID;

    public SupportingAction(AbstractAction act, int decID, TemporalDatabase consumer) {
        this.act = act;
        values = null;
        this.consumerID = consumer.mID;
        this.decID = decID;
    }

    public SupportingAction(AbstractAction act, int decID, Map<LVarRef, Collection<String>> values, TemporalDatabase consumer) {
        this.act = act;
        this.values = values;
        this.consumerID = consumer.mID;
        this.decID = decID;
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
        final TemporalDatabase consumer = st.GetDatabase(consumerID);

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

        if(values != null)
            // restrict domain of given variables to the given set of variables.
            for (LVarRef lvar : values.keySet()) {
                st.restrictDomain(action.context().getGlobalVar(lvar), values.get(lvar));
            }

        // create the binding between consumer and the new statement in the action that supports it
        TemporalDatabase supportingDatabase = null;

        // get potentialy supporting statements in the action or in the decomposition (if the
        // action was marked to be decomposed)
        List<Statement> potentialSuporters;
        if(decID == -1)
            potentialSuporters = action.statements();
        else
            potentialSuporters = dec.statements();

        for (Statement s : potentialSuporters) {
            if (s instanceof LogStatement && st.canBeEnabler((LogStatement) s, consumer)) {
                assert supportingDatabase == null : "Error: several statements might support the database";
                supportingDatabase = st.getDBContaining((LogStatement) s);
            }
        }
        if (supportingDatabase == null) {
            // did not find any database that could support consumer, resolver was not successful
            return false;
        } else {
            // add the causal link
            Resolver opt = new SupportingDatabase(supportingDatabase.mID, consumer);
            TinyLogger.LogInfo(st, "     [%s] Adding %s", st.mID, opt);
            return opt.apply(st, planner);
        }

    }
}
