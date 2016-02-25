package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;
import fape.core.planning.states.StateWrapper;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to use a sequence of PartialPlanComparator as one.
 *
 * The basic algorithm for comparing two partial plans is to apply the comparators in sequence until it results in an ordering
 * between the two plans. If no comparator is found, the plans are left unordered.
 */
public class SeqPlanComparator extends PartialPlanComparator {

    final List<PartialPlanComparator> comparators;

    public SeqPlanComparator(List<PartialPlanComparator> comparators) {
        for(int i=0 ; i<comparators.size() ; i++)
            assert i == comparators.get(i).id : "Comparators are not given the right index.";

        this.comparators = new LinkedList<>(comparators);
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
    public String reportOnState(State st) {
        StringBuilder sb = new StringBuilder();
        for (PartialPlanComparator ppc : comparators) {
            sb.append(ppc.reportOnState(st));
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public int compare(StateWrapper state, StateWrapper state2) {
        for(PartialPlanComparator comp : comparators) {
            int res = comp.compare(state, state2);
            if(res != 0) {
                return res;
            }
        }

        // no ranking done, use mID to make it deterministic
        // this gives a depth first with higher pirority tho the latest states
        return state2.getID() - state.getID();
    }

    @Override
    public float g(State st) {
        return comparators.get(0).g(st);
    }

    @Override
    public float h(State st) {
        return comparators.get(0).h(st);
    }

    @Override
    public float hc(State st) {
        return comparators.get(0).hc(st);
    }
}
