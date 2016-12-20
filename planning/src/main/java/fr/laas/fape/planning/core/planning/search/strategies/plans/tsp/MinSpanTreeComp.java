package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.planning.core.planning.planner.GlobalOptions;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Threat;
import fr.laas.fape.planning.core.planning.search.strategies.plans.PartialPlanComparator;
import fr.laas.fape.planning.core.planning.states.PartialPlan;

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

    private MinSpanTreeExtFull getExt(PartialPlan st) {
        if(!st.hasExtension(MinSpanTreeExtFull.class))
            st.addExtension(new MinSpanTreeExtFull(st));
        return st.getExtension(MinSpanTreeExtFull.class);
    }

    @Override
    public double g(PartialPlan plan) {
        return existingStatementsWeight * getExt(plan).getCurrentCost();
    }

    @Override
    public double h(PartialPlan plan) {
        return hc(plan);
    }

    private int threatsCost(PartialPlan st) {
        if(USE_ALL_THREATS) {
            return st.getAllThreats().size();
        } else {
            return (int) Stream.concat(
                    st.getAllThreats().stream().map(t -> ((Threat) t).db1),
                    st.getAllThreats().stream().map(t -> ((Threat) t).db2)).distinct().count();
        }
    }

    @Override
    public double hc(PartialPlan plan) {
        return pendingStatementsWeight * getExt(plan).getCostToGo()
                + plan.tdb.getConsumers().size()
                + threatsWeight * threatsCost(plan)
                + unrefinedTasksWeight * plan.taskNet.getNumOpenTasks()
                + unbindedVarsWeight * plan.getUnboundVariables().size();
    }

    @Override
    public String reportOnState(PartialPlan plan) {
        return shortName()+"f: "+(g(plan)+h(plan))+" g:"+g(plan)+" h:"+h(plan)+" hc: "+hc(plan)+" = "+getExt(plan).getCostToGo()+" + "+ plan.tdb.getConsumers().size()+" + "+threatsCost(plan);
    }
}
