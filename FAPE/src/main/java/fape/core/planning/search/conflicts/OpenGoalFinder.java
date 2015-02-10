package fape.core.planning.search.conflicts;

import fape.core.planning.search.Flaw;
import fape.core.planning.search.UnsupportedDatabase;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;

import java.util.LinkedList;
import java.util.List;

public class OpenGoalFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(State st) {
        List<Flaw> flaws = new LinkedList<>();

        for(TemporalDatabase consumer : st.consumers)
            flaws.add(new UnsupportedDatabase(consumer));

        return flaws;
    }
}
