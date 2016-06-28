package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.search.flaws.flaws.Threat;
import fape.core.planning.search.strategies.plans.PartialPlanComparator;
import fape.core.planning.states.State;

import java.util.stream.Stream;

public class MinSpanTreeComp extends PartialPlanComparator {
    public static boolean USE_SUM = false;
    @Override
    public String shortName() {
        return "minspan";
    }

    private MinSpanTreeExtFull getExt(State st) {
        if(!st.hasExtension(MinSpanTreeExtFull.class))
            st.addExtension(new MinSpanTreeExtFull(st, true));
        return st.getExtension(MinSpanTreeExtFull.class);
    }

    @Override
    public double g(State st) {
        return getExt(st).getCurrentCost();
    }

    @Override
    public double h(State st) {
        return hc(st);
    }

    private int threatsCost(State st) {
        return (int) Stream.concat(
                st.getAllThreats().stream().map(t -> ((Threat) t).db1),
                st.getAllThreats().stream().map(t -> ((Threat) t).db2)).distinct().count();
    }

    @Override
    public double hc(State st) {
        return getExt(st).getCostToGo()+ st.tdb.getConsumers().size() + threatsCost(st);
    }

    @Override
    public String reportOnState(State st) {
        return shortName()+"f: "+(g(st)+h(st))+" g:"+g(st)+" h:"+h(st)+" hc: "+hc(st)+" = "+getExt(st).getCostToGo()+" + "+ st.tdb.getConsumers().size()+" + "+threatsCost(st);
    }
}
