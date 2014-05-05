package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

import java.util.LinkedList;
import java.util.List;

public class SeqPlanComparator implements PartialPlanComparator {

    List<PartialPlanComparator> comparators;

    public SeqPlanComparator(List<PartialPlanComparator> comparators) {
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
    public int compare(State state, State state2) {
        for(PartialPlanComparator comp : comparators) {
            int res = comp.compare(state, state2);
            if(res != 0) {
                return res;
            }
        }
        // no resolver could rank those flaws.
        return 0;
    }
}
