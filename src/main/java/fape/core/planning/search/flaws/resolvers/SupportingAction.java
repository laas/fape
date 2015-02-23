package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.util.TinyLogger;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Factory;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Statement;

import java.util.Collection;
import java.util.Map;

public class SupportingAction extends Resolver {

    public final AbstractAction act;
    public final Map<LVarRef, Collection<String>> values;
    public final int consumerID;

    public SupportingAction(AbstractAction act, TemporalDatabase consumer) {
        this.act = act;
        values = null;
        this.consumerID = consumer.mID;
    }

    public SupportingAction(AbstractAction act, Map<LVarRef, Collection<String>> values, TemporalDatabase consumer) {
        this.act = act;
        this.values = values;
        this.consumerID = consumer.mID;
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

        if(values != null)
            // restrict domain of given variables to the given set of variables.
            for (LVarRef lvar : values.keySet()) {
                st.restrictDomain(action.context().getGlobalVar(lvar), values.get(lvar));
            }

        // create the binding between consumer and the new statement in the action that supports it
        TemporalDatabase supportingDatabase = null;
        for (Statement s : action.statements()) {
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
