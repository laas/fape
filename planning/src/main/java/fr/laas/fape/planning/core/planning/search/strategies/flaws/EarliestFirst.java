package fr.laas.fape.planning.core.planning.search.strategies.flaws;

import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Threat;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnrefinedTask;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fr.laas.fape.planning.core.planning.states.State;
import lombok.Value;

@Value
public class EarliestFirst implements FlawComparator {

    private final State st;

    @Override
    public String shortName() {
        return  "earliest";
    }

    private float priority(Flaw f) {
        if(f instanceof UnrefinedTask)
            return st.getEarliestStartTime(((UnrefinedTask) f).task.start());
        else if(f instanceof UnsupportedTimeline)
            return st.getEarliestStartTime(((UnsupportedTimeline) f).consumer.getConsumeTimePoint());
        else if(f instanceof Threat) {
            return Math.max(
                    st.getEarliestStartTime(((Threat) f).db1.getConsumeTimePoint()),
                    st.getEarliestStartTime(((Threat) f).db2.getConsumeTimePoint()));
        }
        else
            return Integer.MAX_VALUE /2;
    }

    @Override
    public int compare(Flaw f1, Flaw f2) {
        return (int) Math.signum(priority(f1) - priority(f2));
    }
}
