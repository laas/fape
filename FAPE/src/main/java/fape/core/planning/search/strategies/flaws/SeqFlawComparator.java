package fape.core.planning.search.strategies.flaws;

import fape.core.planning.search.Flaw;
import fape.core.planning.search.SupportOption;
import fape.util.Pair;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class SeqFlawComparator implements FlawComparator {

    List<FlawComparator> comparators;

    public SeqFlawComparator(List<FlawComparator> comparators) {
        this.comparators = new LinkedList<>(comparators);
    }

    @Override
    public int compare(Pair<Flaw, List<SupportOption>> flawListPair, Pair<Flaw, List<SupportOption>> flawListPair2) {
        for(FlawComparator comp : comparators) {
            int res = comp.compare(flawListPair, flawListPair2);
            if(res != 0) {
                return res;
            }
        }
        // no resolver could rank those flaws.
        return 0;
    }

    @Override
    public String shortName() {
        String ret = "";
        for(FlawComparator comp : comparators) {
            ret += comp.shortName() + ",";
        }
        return ret.substring(0, ret.length()-1);
    }
}
