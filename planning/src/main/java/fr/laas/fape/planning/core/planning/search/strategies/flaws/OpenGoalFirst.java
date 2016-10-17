package fr.laas.fape.planning.core.planning.search.strategies.flaws;

import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnsupportedTimeline;

public class OpenGoalFirst implements FlawComparator {
    @Override
    public String shortName() {
        return "ogf";
    }

    @Override
    public int compare(Flaw f1, Flaw f2) {
        if(f1 instanceof UnsupportedTimeline && f2 instanceof UnsupportedTimeline)
            return 0;
        else if(f1 instanceof UnsupportedTimeline)
            return -1;
        else if(f2 instanceof UnsupportedTimeline)
            return 1;
        else
            return 0;
    }
}
