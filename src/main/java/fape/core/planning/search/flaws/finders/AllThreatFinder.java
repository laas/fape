package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.Threat;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.LinkedList;
import java.util.List;

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
                } else if (isThreatening(st, db2, db1)){
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


       /* if (  !db1.HasSinglePersistence() ) {
            for (ChainComponent db1Component : db1.chain) {
                if ( db1Component.change ) {
                    boolean db1AfterDB2 = true;
                    boolean db2AfterDB1 = true;
                    for (LogStatement db2statement : db2.chain.getLast().contents){
                        if (st.canBeBefore(db1Component.contents.getFirst().start(), db2statement.end())) {
                            db1AfterDB2 = false;
                        }
                    }
                    for (LogStatement db2statement : db2.chain.getFirst().contents) {
                        if (st.canBeBefore(db2statement.start(), db1Component.contents.getLast().end())) {
                            db2AfterDB1 = false;
                        }
                    }
                    return !(db1AfterDB2 || db2AfterDB1);
                }
            }
        }*/
        if ( !db1.HasSinglePersistence() & !db2.HasSinglePersistence()){
            boolean db1AfterDB2 = true;
            for (TPRef start1 : db1.getFirstTimePoints()) {
                for (TPRef end2 : db2.getLastTimePoints()) {
                    if (st.canBeBefore(start1, end2)) {
                        db1AfterDB2 = false;
                        break;
                    }
                }
            }
            LinkedList<TPRef> a = new LinkedList<>();
            for (int i = db1.chain.size() - 1; i >= 0; i--) {
                if (db1.chain.get(i).change) {
                    for(LogStatement s : db1.chain.getFirst().contents) {
                        a.add(s.start());
                    }
                }
            }
            LinkedList<TPRef> b = new LinkedList<>();
            for (int i = 0; i < db2.chain.size() - 1; i++) {
                if (db2.chain.get(i).change) {
                    for(LogStatement s : db2.chain.getLast().contents) {
                        b.add(s.end());
                    }
                }
            }
            boolean db2AfterDB1 = true;
            for (TPRef end1 : a)
                for (TPRef start2 : b)
                    if (st.canBeBefore(start2, end1)) {
                        db2AfterDB1 = false;
                        break;
                    }

            // true if they can overlap
            return !(db1AfterDB2 || db2AfterDB1);
        }
        return false;
    }
}