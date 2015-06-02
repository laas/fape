package fape.core.planning.planner;

import fape.core.planning.search.flaws.finders.*;
import planstack.constraints.stnu.Controllability;

public class PlanningOptions {

    public PlanningOptions(String[] planSelStrategies, String[] flawSelStrategies) {
        this.planSelStrategies = planSelStrategies;
        this.flawSelStrategies = flawSelStrategies;
    }

    /**
     * Those are used to extract all flaws from a state.
     * The getFlaws method will typically use all of those
     * to generate the flaws that need to be solved in a given state.
     */
    public FlawFinder[] flawFinders = {
            new OpenGoalFinder(),
            new UndecomposedActionFinder(),
            new UnsupportedTaskConditionFinder(),
            new UnmotivatedActionFinder(),
            new AllThreatFinder(),
            new UnboundVariableFinder()
    };

    /**
     * Used to build comparators for flaws. Default to a least commiting first.
     */
    public String[] flawSelStrategies;

    /**
     * Used to build comparators for partial plans.
     */
    public String[] planSelStrategies;

    /**
     * If true, the planner will solve trivial flaws (with one resolver) before adding the plan
     * to the queue
     */
    public boolean useFastForward = false;

    /**
     * If set to true, the choice of the flaw to solve next will be done on the command line.
     */
    public boolean chooseFlawManually = false;

    /**
     * If set to true, FAPE will check whether an open goal resolver will result in an unsolvable threat.
     * In this case, the resolver will not be included (this is beneficial in a least commitment strategy
     * where choice of the flaw is based on the number of its resolvers).
     */
    public boolean checkUnsolvableThreatsForOpenGoalsResolvers = false;

    /**
     * If true, the planner will use AE search
     */
    public boolean useAE = true;

}
