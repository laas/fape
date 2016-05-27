package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.search.strategies.plans.PartialPlanComparator;
import fape.core.planning.states.State;

public class MinSpanTreeComp extends PartialPlanComparator {
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

    @Override
    public double hc(State st) {
        return getExt(st).getCostToGo()+ st.tdb.getConsumers().size() + st.getAllThreats().size();
    }

    @Override
    public String reportOnState(State st) {
        return shortName()+"f: "+(g(st)+h(st))+" g:"+g(st)+" h:"+h(st)+" hc: "+hc(st)+" = "+getExt(st).getCostToGo()+" + "+ st.tdb.getConsumers().size()+" + "+st.getAllThreats().size();
    }
}
