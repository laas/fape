package fape.core.planning.planner;

import fape.core.planning.Planner;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.LiftedDTG;
import fape.core.planning.states.State;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.parser.ParseResult;
import planstack.constraints.stnu.Controllability;

import java.util.Map;

public class BaseDTG extends Planner {

    LiftedDTG dtg = null;

    public BaseDTG(State initialState, String[] planSelStrategies, String[] flawSelStrategies, Map<ActRef, ActionExecution> actionsExecutions) {
        super(initialState, planSelStrategies, flawSelStrategies, actionsExecutions);
        dtg = new LiftedDTG(pb);
    }

    public BaseDTG(Controllability controllability, String[] planSelStrategies, String[] flawSelStrategies) {
        super(controllability, planSelStrategies, flawSelStrategies);
    }

    @Override
    public String shortName() {
        return "htn";
    }

    @Override
    public ActionSupporterFinder getActionSupporterFinder() {
        return dtg;
    }
}
