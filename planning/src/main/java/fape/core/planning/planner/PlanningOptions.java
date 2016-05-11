package fape.core.planning.planner;

import fape.core.planning.preprocessing.PreprocessorHandler;
import fape.core.planning.search.Handler;
import fape.core.planning.search.flaws.finders.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlanningOptions {

    public enum ActionInsertionStrategy { DOWNWARD_ONLY, UP_OR_DOWN }

    public PlanningOptions(String[] planSelStrategies, String[] flawSelStrategies) {
        this.planSelStrategies = planSelStrategies;
        this.flawSelStrategies = flawSelStrategies;
    }

    /**
     * Those are used to extract all flaws from a state.
     * The getFlaws method will typically use all of those
     * to generate the flaws that need to be solved in a given state.
     */
    public final List<FlawFinder> flawFinders = new ArrayList<>(Arrays.asList(
            new OpenGoalFinder(),
            new UnrefinedTaskFinder(),
            new UnmotivatedActionFinder(),
            new AllThreatFinder(),
            new UnboundVariableFinder()
    ));

    public final List<Handler> handlers = new ArrayList<>(Collections.singletonList(
            new PreprocessorHandler())
    );

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

    public ActionInsertionStrategy actionInsertionStrategy = ActionInsertionStrategy.DOWNWARD_ONLY;

    /**
     * If set to true, the choice of the flaw to solve next will be done on the command line.
     */
    public boolean chooseFlawManually = false;

    public boolean actionsSupportMultipleTasks = false;

    /**
     * If set to true, FAPE will check whether an open goal resolver will result in an unsolvable threat.
     * In this case, the resolver will not be included (this is beneficial in a least commitment strategy
     * where choice of the flaw is based on the number of its resolvers).
     */
    public boolean checkUnsolvableThreatsForOpenGoalsResolvers = true;

    /**
     * If true, the planner will use A-Epsilon for search
     */
    public boolean useAEpsilon = false;
    public float epsilon = 0.3f;

    /** Which type of dependency graph to build */
    public String depGraphStyle = "base";
    public int depGraphMaxIters = Integer.MAX_VALUE;

    public boolean displaySearch = true;


}
