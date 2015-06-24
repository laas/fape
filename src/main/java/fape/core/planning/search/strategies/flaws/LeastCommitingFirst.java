package fape.core.planning.search.strategies.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.states.State;


/**
 * This strategies orders flaws by increasing number of resolvers.
 */
public class LeastCommitingFirst implements FlawComparator {

    public final State st;
    public final APlanner planner;

    public LeastCommitingFirst(State st, APlanner planner) {
        this.st = st;
        this.planner = planner;
    }

    @Override
    public int compare(Flaw f1, Flaw f2) {
        return f1.getNumResolvers(st, planner) - f2.getNumResolvers(st, planner);
    }

    @Override
    public String shortName() {
        return "lcf";
    }
}
