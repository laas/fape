package fape.core.planning.planner;

import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import planstack.constraints.stnu.Controllability;

public class PlannerFactory {

    public static final String defaultPlanner = "fape";
    public static final String[] defaultPlanSelStrategies = { "soca" }; //TODO: put back rplan?
    public static final String[] defaultFlawSelStrategies = { "hf", "ogf", "abs", "lcf", "eogf" };
    public static final Controllability defaultControllabilityStrategy = Controllability.PSEUDO_CONTROLLABILITY;

    public static PlanningOptions defaultOptions() {
        PlanningOptions options = new PlanningOptions(defaultPlanSelStrategies, defaultFlawSelStrategies);
        return options;
    }

    public static APlanner getDefaultPlanner() {
        return getPlanner(defaultPlanner, defaultOptions(), defaultControllabilityStrategy);
    }

    public static APlanner getPlanner(String name, PlanningOptions options, Controllability controllability) {
        switch (name) {
            case "topdown":
                return new TopDownPlanner(controllability, options);
            case "fape":
                return new FAPEPlanner(controllability,options);
            default:
                throw new FAPEException("Unknown planner name: "+name);
        }
    }

    public static APlanner getPlannerFromInitialState(String name, State state, PlanningOptions options) {
         switch (name) {
            case "topdown":
                return new TopDownPlanner(state, options);
            case "fape":
                return new FAPEPlanner(state, options);
            default:
                throw new FAPEException("Unknown planner name: "+name);
        }
    }



    public static APlanner getPlanner(String name) {
        return getPlanner(name, defaultOptions(), defaultControllabilityStrategy);
    }

    public static APlanner getPlanner(String name, State state) {
        return getPlannerFromInitialState(name, state, defaultOptions());
    }
}
