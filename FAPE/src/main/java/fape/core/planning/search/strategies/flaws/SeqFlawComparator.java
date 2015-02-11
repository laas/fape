package fape.core.planning.search.strategies.flaws;

import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.util.Pair;

import java.util.LinkedList;
import java.util.List;


/**
 * Used to use a sequence of FlawComparator as one.
 *
 * The basic algorithm for comparing two flaws is to apply the comparators in sequence until it results in an ordering
 * between the two flaws. If no comparator is found, the flaws are left unordered.
 */
public class SeqFlawComparator implements FlawComparator {

    List<FlawComparator> comparators;

    public SeqFlawComparator(List<FlawComparator> comparators) {
        this.comparators = new LinkedList<>(comparators);
    }

    @Override
    public int compare(Pair<Flaw, List<Resolver>> flawListPair, Pair<Flaw, List<Resolver>> flawListPair2) {
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
