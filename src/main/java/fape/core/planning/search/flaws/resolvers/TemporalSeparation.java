package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.concrete.statements.LogStatement;

public class TemporalSeparation extends Resolver {

    public final int firstDbID;
    public final int secondDbID;

    public TemporalSeparation(Timeline first, Timeline second) {
        this.firstDbID = first.mID;
        this.secondDbID = second.mID;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        final Timeline firstDB = st.getDatabase(firstDbID);
        final Timeline secondDB = st.getDatabase(secondDbID);
        assert firstDB != null && secondDB != null;
        assert !firstDB.hasSinglePersistence() && !secondDB.hasSinglePersistence();

        st.enforceStrictlyBefore(
                firstDB.getSupportTimePoint(),
                secondDB.getFirstTimePoints()
        );
        st.enforceStrictlyBefore(
                firstDB.getLastTimePoints(),
                secondDB.getFirstChange().start()
        );
        return st.isConsistent();
    }
}
