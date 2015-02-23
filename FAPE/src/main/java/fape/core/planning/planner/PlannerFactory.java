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
         switch (name) {
            case "htn":
            case "base+dtg":
                return new BaseDTG(state, planSelStrategies, flawSelStrategies);
            case "base":
                return new Planner(state, planSelStrategies, flawSelStrategies);
            case "rpg":
                return new PGPlanner(state, planSelStrategies, flawSelStrategies);
            case "rpg_ext":
                return new PGExtPlanner(state, planSelStrategies, flawSelStrategies);
            case "taskcond":
                return new TaskConditionPlanner(state, planSelStrategies, flawSelStrategies);
            default:
                throw new FAPEException("Unknown planner name: "+name);
        }
    }

    public static APlanner getPlanner(String name) {
        return getPlanner(name, defaultPlanSelStrategies, defaultFlawSelStrategies, defaultControllabilityStrategy);
    }

    public static APlanner getPlanner(String name, State state) {
        return getPlannerFromInitialState(name, state, defaultPlanSelStrategies, defaultFlawSelStrategies);
    }
}
