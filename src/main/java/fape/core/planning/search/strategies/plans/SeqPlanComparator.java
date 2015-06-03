package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to use a sequence of PartialPlanComparator as one.
 *
 * The basic algorithm for comparing two partial plans is to apply the comparators in sequence until it results in an ordering
 * between the two plans. If no comparator is found, the plans are left unordered.
 */
public class SeqPlanComparator implements PartialPlanComparator, Heuristic {

    final List<PartialPlanComparator> comparators;
    final Heuristic heuristic;

    public SeqPlanComparator(List<PartialPlanComparator> comparators) {
        this.comparators = new LinkedList<>(comparators);
        if(comparators.get(0) instanceof Heuristic)
            heuristic = (Heuristic) comparators.get(0);
        else
            heuristic = null;

    }

    @Override
    public String shortName() {
        String ret = "";
        for(PartialPlanComparator comp : comparators) {
            ret += comp.shortName() + ",";
        }
        return ret.substring(0, ret.length()-1);
    }

    @Override
    public int compare(State state, State state2) {
        for(PartialPlanComparator comp : comparators) {
            int res = comp.compare(state, state2);
            if(res != 0) {
                return res;
            }
        }

        // tie breaker: makespan
        int diffMakespan = state.getEarliestStartTime(state.pb.end()) - state2.getEarliestStartTime(state2.pb.end());
        if(diffMakespan != 0)
            return diffMakespan;

        // no ranking done, use mID to make deterministic
        return state.mID - state2.mID;
    }

    @Override
    public float g(State st) {
        assert heuristic != null : "Error: the first plan comparator does not implement heuristic.";
        return heuristic.g(st);
    }

    @Override
    public float h(State st) {
        assert heuristic != null : "Error: the first plan comparator does not implement heuristic.";
        return heuristic.h(st);
    }

    @Override
    public float hc(State st) {
        assert heuristic != null : "Error: the first plan comparator does not implement heuristic.";
        return heuristic.hc(st);
    }
}
