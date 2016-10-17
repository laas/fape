package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.planning.core.planning.planner.GlobalOptions;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Threat;
import fr.laas.fape.planning.core.planning.search.strategies.plans.PartialPlanComparator;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.stream.Stream;

public class MinSpanTreeComp extends PartialPlanComparator {
    private boolean USE_ALL_THREATS = GlobalOptions.getBooleanOption("heur-all-threats");
    private float threatsWeight = GlobalOptions.getFloatOption("heur-weight-threats");
    private float existingStatementsWeight = GlobalOptions.getFloatOption("heur-weight-existing-statements");
    private float pendingStatementsWeight = GlobalOptions.getFloatOption("heur-weight-pending-statements");
    private float unrefinedTasksWeight = GlobalOptions.getFloatOption("heur-weight-unrefined-tasks");
    private float unbindedVarsWeight = GlobalOptions.getFloatOption("heur-weight-unbinded-variables");
    @Override
    public String shortName() {
        return "minspan";
    }

    private MinSpanTreeExtFull getExt(State st) {
        if(!st.hasExtension(MinSpanTreeExtFull.class))
            st.addExtension(new MinSpanTreeExtFull(st));
        return st.getExtension(MinSpanTreeExtFull.class);
    }

    @Override
    public double g(State st) {
        return existingStatementsWeight * getExt(st).getCurrentCost();
    }

    @Override
    public double h(State st) {
        return hc(st);
    }

    private int threatsCost(State st) {
        if(USE_ALL_THREATS) {
            return st.getAllThreats().size();
        } else {
            return (int) Stream.concat(
                    st.getAllThreats().stream().map(t -> ((Threat) t).db1),
                    st.getAllThreats().stream().map(t -> ((Threat) t).db2)).distinct().count();
        }
    }

    @Override
    public double hc(State st) {
        return pendingStatementsWeight * getExt(st).getCostToGo()
                + st.tdb.getConsumers().size()
                + threatsWeight * threatsCost(st)
                + unrefinedTasksWeight * st.taskNet.getNumOpenTasks()
                + unbindedVarsWeight * st.getUnboundVariables().size();
    }

    @Override
    public String reportOnState(State st) {
        return shortName()+"f: "+(g(st)+h(st))+" g:"+g(st)+" h:"+h(st)+" hc: "+hc(st)+" = "+getExt(st).getCostToGo()+" + "+ st.tdb.getConsumers().size()+" + "+threatsCost(st);
    }
}
