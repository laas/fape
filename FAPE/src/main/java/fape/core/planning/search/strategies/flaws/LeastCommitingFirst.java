package fape.core.planning.search.strategies.flaws;

import fape.core.planning.search.Flaw;
import fape.core.planning.search.SupportOption;
import fape.util.Pair;

import java.util.List;


/**
 * This strategies orders flaws by increasing number of resolvers.
 */
public class LeastCommitingFirst implements FlawComparator {


    @Override
    public int compare(Pair<Flaw, List<SupportOption>> f1, Pair<Flaw, List<SupportOption>> f2) {
        return f1.value2.size() - f2.value2.size();
    }

    @Override
    public String shortName() {
        return "lcf";
    }
}
