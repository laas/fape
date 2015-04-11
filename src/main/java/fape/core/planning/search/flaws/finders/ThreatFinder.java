package fape.core.planning.search.flaws.finders;

import fape.core.planning.search.flaws.flaws.Threat;
import fape.core.planning.timelines.Timeline;
import fape.core.planning.states.State;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.planner.APlanner;
import planstack.anml.model.concrete.TPRef;

import java.util.LinkedList;
import java.util.List;

public class ThreatFinder implements FlawFinder {
    public List<Flaw> getFlaws(State st, APlanner planner) {
        List<Flaw> flaws = new LinkedList<>();

        List<Timeline> dbs = st.getTimelines();
        for (int i = 0; i < dbs.size(); i++) {
            Timeline db1 = dbs.get(i);
            for (int j = i + 1; j < dbs.size(); j++) {
                Timeline db2 = dbs.get(j);
                if (isThreatening(st, db1, db2)) {
                    flaws.add(new Threat(db1, db2));
                }
            }
        }

        return flaws;
    }


    protected boolean isThreatening(State st, Timeline db1, Timeline db2) {
        // if they are not both consumers, it is dealt by open goal reasoning
        if (db1.isConsumer() || db2.isConsumer())
            return false;

        // if their state variables are not unifiable
        if (!st.unifiable(db1, db2))
            return false;

        // if db1 cannot start before db2 ends
        boolean db1AfterDB2 = true;
        for (TPRef start1 : db1.getFirstTimePoints()) {
            for (TPRef end2 : db2.getLastTimePoints()) {
                if (st.canBeBefore(start1, end2)) {
                    db1AfterDB2 = false;
                    break;
                }
            }
        }
        // if db2 cannot start before db1 ends
        boolean db2AfterDB1 = true;
        for (TPRef end1 : db1.getLastTimePoints())
            for (TPRef start2 : db2.getFirstTimePoints())
                if (st.canBeBefore(start2, end1)) {
                    db2AfterDB1 = false;
                    break;
                }

        // true if they can overlap
        return !(db1AfterDB2 || db2AfterDB1);

    }
}