package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.Threat;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.LinkedList;
import java.util.List;

public class AllThreatFinder implements FlawFinder {

    public List<Flaw> getFlaws(State st, APlanner planner) {
//        return getAllThreats(st); // non-incremental version
        return st.getAllThreats(); // incremental version
    }

    /** Finds all threats in a state, this can be used to check that the incremental threat resolution works a expected */
    public static List<Flaw> getAllThreats(State st) {
        List<Flaw> flaws = new LinkedList<>();

        List<Timeline> dbs = st.getTimelines();
        for (int i = 0; i < dbs.size(); i++) {
            Timeline db1 = dbs.get(i);
            for (int j = i + 1; j < dbs.size(); j++) {
                Timeline db2 = dbs.get(j);
                if (isThreatening(st, db1, db2)) {
                    flaws.add(new Threat(db1, db2));
                } else {
                    assert !isThreatening(st, db2, db1) : "Is threatening is not symmetrical";
                }
            }
        }

        return flaws;
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
                    !st.canAnyBeBefore(tl2.getFirstTimePoints(), tl1.getSupportTimePoint()) &&
                            !st.canAnyBeBefore(tl2.getFirstChange().start(), tl1.getLastTimePoints());

            boolean secondNecessarilyAfterFirst =
                    !st.canAnyBeBefore(tl1.getFirstTimePoints(), tl2.getSupportTimePoint()) &&
                            !st.canAnyBeBefore(tl1.getFirstChange().start(), tl2.getLastTimePoints());

            return !(firstNecessarilyAfterSecond || secondNecessarilyAfterFirst);
        }
    }
}