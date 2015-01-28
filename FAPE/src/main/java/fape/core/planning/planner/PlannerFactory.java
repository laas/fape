package fape.core.planning.planner;

import fape.core.planning.Planner;
import fape.core.planning.planninggraph.PGPlanner;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import planstack.constraints.stnu.Controllability;

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

    public static APlanner getPlanner(String name) {
        return getPlanner(name, defaultPlanSelStrategies, defaultFlawSelStrategies, defaultControllabilityStrategy);
    }

    public static APlanner getPlanner(String name, State state) {
        throw new UnsupportedOperationException(); //TODO
    }
}
