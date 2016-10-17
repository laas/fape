package fr.laas.fape.planning.core.planning.search.strategies.flaws;

import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnboundVariable;

public class UnboundFirst implements FlawComparator {
    @Override
    public String shortName() {
        return "unbound";
    }

    @Override
    public int compare(Flaw f1, Flaw f2) {
        if(f1 instanceof UnboundVariable && f2 instanceof UnboundVariable)
            return 0;
        else if(f1 instanceof UnboundVariable)
            return -1;
        else if(f2 instanceof UnboundVariable)
            return 1;
        else
            return 0;
    }
}

