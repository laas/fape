package fr.laas.fape.planning.core.planning.search.strategies.flaws;

import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Threat;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fr.laas.fape.planning.core.planning.search.strategies.plans.tsp.MinSpanTreeExtFull;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import lombok.Value;

import java.util.Collections;

@Value
public class MinSpanFailFirst implements FlawComparator {

    private final State st;

    @Override
    public String shortName() {
        return "minspan";
    }

    private MinSpanTreeExtFull getExt() {
        if(!st.hasExtension(MinSpanTreeExtFull.class))
            st.addExtension(new MinSpanTreeExtFull(st));
        return st.getExtension(MinSpanTreeExtFull.class);
    }

    public int associatedCost(Timeline tl) {
//        if(true)
//            return 0;
//        if(!getExt().hasBeenProcessed())
//            return 0;
//        else
            return getExt().allCosts
                    .computeIfAbsent(tl, (x) -> Collections.singletonList(0))
                    .stream()
                    .mapToInt(x -> x)
                    .sum();
    }

    private float priority(Flaw f) {
//        if(f instanceof UnrefinedTask)
//            return 9999;//st.getEarliestStartTime(((UnrefinedTask) f).task.start());
        if(f instanceof UnsupportedTimeline)
            return - associatedCost(((UnsupportedTimeline) f).getConsumer());
        else if(f instanceof Threat) {
            return - Math.max(associatedCost(((Threat) f).db1), associatedCost(((Threat) f).db2));
        }
        else
            return Integer.MAX_VALUE /2;
    }

    @Override
    public int compare(Flaw f1, Flaw f2) {
        return (int) Math.signum(priority(f1) - priority(f2));
    }
}
