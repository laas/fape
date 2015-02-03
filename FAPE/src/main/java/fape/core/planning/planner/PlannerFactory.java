package fape.core.planning.planner;

import fape.core.planning.Planner;
import fape.core.planning.planninggraph.PGPlanner;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionStatus;
import planstack.anml.model.concrete.VarRef;
import planstack.constraints.stnu.Controllability;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PlannerFactory {

    public static final String defaultPlanner = "htn";
    public static final String[] defaultPlanSelStrategies = { "soca" };
    public static final String[] defaultFlawSelStrategies = { "lcf" };
    public static final Controllability defaultControllabilityStrategy = Controllability.STN_CONSISTENCY;

    public static APlanner getDefaultPlanner() {
        return getPlanner(defaultPlanner, defaultPlanSelStrategies, defaultFlawSelStrategies, defaultControllabilityStrategy);
    }

    public static APlanner getPlanner(String name, String[] planSelStrategies, String[] flawSelStrategies, Controllability controllability) {
        switch (name) {
            case "htn":
            case "base+dtg":
                return new BaseDTG(controllability, planSelStrategies, flawSelStrategies);
            case "base":
                return new Planner(controllability, planSelStrategies, flawSelStrategies);
            case "rpg":
                return new PGPlanner(controllability, planSelStrategies, flawSelStrategies);
            case "rpg_ext":
                return new PGExtPlanner(controllability, planSelStrategies, flawSelStrategies);
            case "taskcond":
                return new TaskConditionPlanner(controllability, planSelStrategies, flawSelStrategies);
            default:
                throw new FAPEException("Unknown planner name: "+name);
        }
    }

    public static APlanner getPlannerFromInitialState(String name, State state, String[] planSelStrategies, String[] flawSelStrategies) {
        // first look into state for executed and executing actions
        Map<ActRef, ActionExecution> actionExecutionMap = new HashMap<>();
        for(Action a : state.getAllActions()) {
            if(a.status() == ActionStatus.EXECUTING || a.status() == ActionStatus.EXECUTED) {
                List<String> params = new LinkedList<>();
                for(VarRef arg : a.args()) {
                    List<String> possibleValues = new LinkedList<>(state.domainOf(arg));
                    assert possibleValues.size() == 1 : "Argument "+arg+" of action "+a+" has more than one possible value.";
                    params.add(possibleValues.get(0));
                }
                assert state.getEarliestStartTime(a.start()) == state.getLatestStartTime(a.start()) :
                        "Error: executed or executing action has a non fixed start time.";
                ActionExecution ae = new ActionExecution(a, params, state.getEarliestStartTime(a.start()));
                if(a.status() == ActionStatus.EXECUTED) {
                    assert state.getEarliestStartTime(a.end()) == state.getLatestStartTime(a.end()) :
                            "Error: executed action has a non fixed end time.";
                    ae.setSuccess(state.getEarliestStartTime(a.end()));
                }
                actionExecutionMap.put(a.id(), ae);
            } else {
                assert a.status() == ActionStatus.FAILED || a.status() == ActionStatus.PENDING : "Not handled action status.";
            }
        }
        switch (name) {
            case "htn":
            case "base+dtg":
                return new BaseDTG(state, planSelStrategies, flawSelStrategies, actionExecutionMap);
            case "base":
                return new Planner(state, planSelStrategies, flawSelStrategies, actionExecutionMap);
            case "rpg":
                return new PGPlanner(state, planSelStrategies, flawSelStrategies, actionExecutionMap);
            case "rpg_ext":
                return new PGExtPlanner(state, planSelStrategies, flawSelStrategies, actionExecutionMap);
            case "taskcond":
                return new TaskConditionPlanner(state, planSelStrategies, flawSelStrategies, actionExecutionMap);
            default:
                throw new FAPEException("Unknown planner name: "+name);
        }
    }

    public static APlanner getPlanner(String name) {
        return getPlanner(name, defaultPlanSelStrategies, defaultFlawSelStrategies, defaultControllabilityStrategy);
    }

    public static APlanner getPlanner(String name, State state) {
        throw new UnsupportedOperationException(); //TODO
    }
}
