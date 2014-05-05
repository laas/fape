package fape.core.planning.search.strategies.flaws;

import fape.core.planning.search.Flaw;
import fape.core.planning.search.SupportOption;
import fape.util.Pair;

import java.util.Comparator;
import java.util.List;

public interface FlawComparator extends Comparator<Pair<Flaw, List<SupportOption>>> {

    public abstract String shortName();
}
