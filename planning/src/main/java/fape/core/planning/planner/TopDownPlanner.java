package fape.core.planning.planner;

import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.LiftedDTG;
import fape.core.planning.states.State;
import planstack.constraints.stnu.Controllability;

public class TopDownPlanner extends APlanner {

    LiftedDTG dtg = null;

    public TopDownPlanner(State initialState, PlanningOptions options) {
        super(initialState, options);
        dtg = new LiftedDTG(pb);
    }

    public TopDownPlanner(Controllability controllability, PlanningOptions options) {
        super(controllability, options);
    }

    @Override
    public String shortName() {
        return "htn";
    }

    @Override
    public ActionSupporterFinder getActionSupporterFinder() {
        return dtg;
    }

    @Override
    public boolean isTopDownOnly() {
        return true;
    }
}
