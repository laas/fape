package fape.core.planning.search.strategies.flaws;

import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.TPRef;

/**
 * Given two open goals, give a higher priority to the one being the closest to the problem start.
 */
public class EarliestOpenGoalFirst implements FlawComparator {

    final State st;

    public EarliestOpenGoalFirst(State st) {
        this.st = st;
    }
    @Override
    public String shortName() {
        return "eogf";
    }

    int priority(UnsupportedTimeline db) {
        int start = 99999999;
        for(TPRef tp : db.consumer.getFirstTimePoints()) {
            start = start < st.getEarliestStartTime(tp) ? start : st.getEarliestStartTime(tp);
        }
        return start;
    }

    @Override
    public int compare(Flaw f1, Flaw f2) {
        if(f1 instanceof UnsupportedTimeline && f2 instanceof UnsupportedTimeline)
            return priority((UnsupportedTimeline) f1) - priority((UnsupportedTimeline) f2);
        else
            return 0;
    }
}
