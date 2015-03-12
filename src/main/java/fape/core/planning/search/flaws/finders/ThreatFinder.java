package fape.core.planning.search.flaws.finders;

import fape.core.planning.search.flaws.flaws.Threat;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.states.State;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.planner.APlanner;
import planstack.anml.model.concrete.TPRef;

import java.util.LinkedList;
import java.util.List;

public class ThreatFinder implements FlawFinder {
    public List<Flaw> getFlaws(State st, APlanner planner) {
        List<Flaw> flaws = new LinkedList<>();

        List<TemporalDatabase> dbs = st.getDatabases();
        for (int i = 0; i < dbs.size(); i++) {
            TemporalDatabase db1 = dbs.get(i);
            for (int j = i + 1; j < dbs.size(); j++) {
                TemporalDatabase db2 = dbs.get(j);
                if (isThreatening(st, db1, db2)) {
                    flaws.add(new Threat(db1, db2));
                }
            }
        }

        return flaws;
    }


    protected boolean isThreatening(State st, TemporalDatabase db1, TemporalDatabase db2) {
        // if they are not both consumers, it is dealt by open goal reasoning
        if (db1.isConsumer() || db2.isConsumer())
            return false;

        // if their state variables are not unifiable
        if (!st.Unifiable(db1, db2))
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