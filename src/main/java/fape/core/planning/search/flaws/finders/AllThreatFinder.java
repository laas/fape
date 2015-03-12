package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.Threat;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.concrete.TPRef;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by lagleize on 06/03/15.
 */
public class AllThreatFinder implements FlawFinder {
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
     // if their state variables are not unifiable
        if (!st.Unifiable(db1, db2))
            return false;

        boolean db1AfterDB2 = true;
        boolean db2AfterDB1 = true;
        if (  !(db1.HasSinglePersistence())  &&   ! (db2.HasSinglePersistence() )) {
            // if db1 cannot start before db2 ends
            if (st.canBeBefore(db1.getFirstChange().start(),db2.getSupportingComponent().contents.getLast().end())) {
                db1AfterDB2 = false;
            }
            // if db2 cannot start before db1 ends
            if (st.canBeBefore(db2.getSupportingComponent().contents.getLast().start(),db1.getFirstChange().end())) {
                db2AfterDB1 = false;
                // true if they can overlap
            }
            return !(db1AfterDB2 || db2AfterDB1);
        }
        return false;
    }
}