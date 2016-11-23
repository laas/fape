package fr.laas.fape.planning.core.planning.planner;

import fr.laas.fape.planning.core.planning.preprocessing.PreprocessorHandler;
import fr.laas.fape.planning.core.planning.search.Handler;
import fr.laas.fape.planning.core.planning.search.flaws.finders.*;

import java.util.*;

public class PlanningOptions {

    public enum ActionInsertionStrategy { DOWNWARD_ONLY, UP_OR_DOWN }

    public PlanningOptions(List<String> planSelStrategies, List<String> flawSelStrategies) {
        this.planSelStrategies = planSelStrategies;
        this.flawSelStrategies = flawSelStrategies;
    }

    public PlanningOptions() {}

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
     * Used to build comparators for flaws.
     */
    public List<String> flawSelStrategies = Arrays.asList("hier","ogf","abs","lcf","eogf");

    /**
     * Used to build comparators for partial plans.
     */
    public List<String> planSelStrategies = Collections.singletonList("soca");

    /**
     * If true, the planner will solve trivial flaws (with one resolver) before adding the plan
     * to the queue
     */
    public boolean useFastForward = true;

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
    public boolean useAEpsilon = true;
    public final float epsilon = GlobalOptions.getFloatOption("search-epsilon");

    /** Which type of dependency graph to build */
    public String depGraphStyle = "full";
    public int depGraphMaxIters = Integer.MAX_VALUE;

    public boolean displaySearch = false;

    /** the weight of weighted A*:  f = g + w * h */
    public float heuristicWeight = GlobalOptions.getFloatOption("heur-h-weight");

    /** Ratio of the time the planner should pursue an interesting solution in depth first manner vs the
     * time it should push back the frontier by exploring least cost nodes */
    public float depthShallowRatio = GlobalOptions.getFloatOption("heur-depth-shallow-ratio");
}
