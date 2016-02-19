package fape.core.planning.search.strategies.flaws;

import fape.core.planning.search.flaws.flaws.Flaw;

import java.util.Comparator;

/**
 * This is the base interface that any flaw selection strategy should implement.
 */
public interface FlawComparator extends Comparator<Flaw> {

    /**
     * @return A short (maw 15 chars) and human understandable name for the strategy.
     */
    String shortName();
}
