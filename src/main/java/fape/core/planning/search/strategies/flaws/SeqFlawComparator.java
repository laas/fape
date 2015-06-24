package fape.core.planning.search.strategies.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.states.State;

import java.util.LinkedList;
import java.util.List;


/**
 * Used to use a sequence of FlawComparator as one.
 *
 * The basic algorithm for comparing two flaws is to apply the comparators in sequence until it results in an ordering
 * between the two flaws. If no comparator is found, the flaws are left unordered.
 */
public class SeqFlawComparator implements FlawComparator {

    final List<FlawComparator> comparators;
    final APlanner planner;
    final State st;

    public SeqFlawComparator(State st, APlanner planner, List<FlawComparator> comparators) {
        this.comparators = new LinkedList<>(comparators);
        this.comparators.add(new TieBreaker());
        this.st = st;
        this.planner = planner;
    }

    private int priority(Flaw f) {
        if(f.getNumResolvers(st, planner) == 0)
            return 0;
        else if(f.getNumResolvers(st, planner) == 1)
            return 1;
        else
            return 2;
    }

    @Override
    public int compare(Flaw f1, Flaw f2) {
        // just to make flaws with no resolver come first and flaws with one resolver come second
        int order = priority(f1) - priority(f2);
        if(order != 0)
            return order;
        for(FlawComparator comp : comparators) {
            int res = comp.compare(f1, f2);
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
