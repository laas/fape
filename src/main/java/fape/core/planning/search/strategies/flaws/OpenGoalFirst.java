package fape.core.planning.search.strategies.flaws;

import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnsupportedDatabase;

public class OpenGoalFirst implements FlawComparator {
    @Override
    public String shortName() {
        return "ogf";
    }

    @Override
    public int compare(Flaw f1, Flaw f2) {
        if(f1 instanceof UnsupportedDatabase && f2 instanceof UnsupportedDatabase)
            return 0;
        else if(f1 instanceof UnsupportedDatabase)
            return -1;
        else if(f2 instanceof UnsupportedDatabase)
            return 1;
        else
            return 0;
    }
}
