package fape.core.planning.search.strategies.flaws;

import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.Threat;
import fape.core.planning.search.flaws.flaws.UnsupportedTimeline;

public class ThreatsFirst implements FlawComparator {
    @Override
    public String shortName() {
        return "threats";
    }

    @Override
    public int compare(Flaw f1, Flaw f2) {
        if(f1 instanceof Threat && f2 instanceof Threat)
            return 0;
        else if(f1 instanceof Threat)
            return -1;
        else if(f2 instanceof Threat)
            return 1;
        else
            return 0;
    }
}
