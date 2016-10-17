package fr.laas.fape.planning.core.planning.search.strategies.flaws;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.exceptions.FAPEException;

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
    final Planner planner;
    final State st;

    public SeqFlawComparator(State st, Planner planner, List<FlawComparator> comparators) {
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
        // Flaws must be totally ordered to make sure we can rebuild a State.
        throw new FAPEException("Unable to totally order those flaws: "+f1+" "+f2);
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
