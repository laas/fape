package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;

public class TemporalSeparation extends Resolver {

    public final int firstDbID;
    public final int secondDbID;

    public TemporalSeparation(Timeline first, Timeline second) {
        this.firstDbID = first.mID;
        this.secondDbID = second.mID;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        final Timeline firstDB = st.getTimeline(firstDbID);
        final Timeline secondDB = st.getTimeline(secondDbID);
        assert firstDB != null && secondDB != null;
        assert !firstDB.hasSinglePersistence() && !secondDB.hasSinglePersistence();

        st.enforceBefore(
                firstDB.getSupportTimePoint(),
                secondDB.getFirstTimePoints()
        );
        st.enforceBefore(
                firstDB.getLastTimePoints(),
                secondDB.getFirstChange().start()
        );
        return st.checkConsistency();
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof TemporalSeparation;
        TemporalSeparation o = (TemporalSeparation) e;
        if(firstDbID != o.firstDbID)
            return firstDbID - o.firstDbID;
        assert secondDbID != o.secondDbID : "Comparing two identical resolvers.";
        return secondDbID - o.secondDbID;
    }
}
