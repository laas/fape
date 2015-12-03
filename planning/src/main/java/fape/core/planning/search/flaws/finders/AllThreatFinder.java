package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;

import java.util.List;

public class AllThreatFinder implements FlawFinder {

    public List<Flaw> getFlaws(State st, APlanner planner) {
        return st.getAllThreats(); // incremental version
    }

    public static boolean isThreatening(State st, Timeline tl1, Timeline tl2) {
        if(tl1 == tl2)
            return false;

        if(!st.unifiable(tl1, tl2))
            return false;

        else if(tl1.hasSinglePersistence() && tl2.hasSinglePersistence())
            return false;

        else if(tl1.hasSinglePersistence())
            return false;

        else if(tl2.hasSinglePersistence())
            return false;

        else {
            boolean firstNecessarilyAfterSecond =
                    !st.canAnyBeStrictlyBefore(tl2.getFirstTimePoints(), tl1.getSupportTimePoint()) &&
                            !st.canAnyBeStrictlyBefore(tl2.getFirstChange().start(), tl1.getLastTimePoints());

            boolean secondNecessarilyAfterFirst =
                    !st.canAnyBeStrictlyBefore(tl1.getFirstTimePoints(), tl2.getSupportTimePoint()) &&
                            !st.canAnyBeStrictlyBefore(tl1.getFirstChange().start(), tl2.getLastTimePoints());

            return !(firstNecessarilyAfterSecond || secondNecessarilyAfterFirst);
        }
    }
}