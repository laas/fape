package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.concrete.statements.LogStatement;

public class TemporalSeparation extends Resolver {

    public final int firstDbID;
    public final int secondDbID;

    public TemporalSeparation(TemporalDatabase first, TemporalDatabase second) {
        this.firstDbID = first.mID;
        this.secondDbID = second.mID;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        final TemporalDatabase firstDB = st.getDatabase(firstDbID);
        final TemporalDatabase secondDB = st.getDatabase(secondDbID);
        assert firstDB != null && secondDB != null;

        for (LogStatement first : firstDB.chain.getLast().contents) {
            for (LogStatement second : secondDB.chain.getFirst().contents) {
                st.enforceStrictlyBefore(first.end(), second.start());
            }
        }
        return true;
    }
}
