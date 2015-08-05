package fape.core.planning.search.strategies.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;

import java.util.LinkedList;
import java.util.List;

public class FlawCompFactory {

    /**
     * Factory to creates a FlawComparator that implements the given strategy. A
     * strategy consists of a sequence of strings each one mapping to a
     * strategy.
     *
     * If more than one strategy is given, a strategy is only used if all
     * previous strategy resulted in a tie.
     *
     * @param st State for which to create the comparator.
     * @param planner The planner from which this method is invoked (used to look for options).
     * @param comparators A sequence of string describing the strategy.
     * @return A comparator for flaws issued from the state.
     */
    public static FlawComparator get(State st, APlanner planner,  String... comparators) {
        List<FlawComparator> compList = new LinkedList<>();
        for (String compID : comparators) {
            switch (compID) {
                case "abs":
                    compList.add(new AbsHierarchyComp(st));
                    break;
                case "lcf":
                    compList.add(new LeastCommitingFirst(st, planner));
                    break;
                case "rfp":
                    compList.add(new ResourceFlawPreference());
                    break;
                case "hf":
                    compList.add(new HierarchicalFirstComp(st, planner));
                    break;
                case "ogf":
                    compList.add(new OpenGoalFirst());
                    break;
                case "eogf":
                    compList.add(new EarliestOpenGoalFirst(st));
                    break;
                case "rpgog":
                    compList.add(new RPGOpenGoalComp(st, planner));
                    break;
                case "extfirst":
                    compList.add(new ExtendPlanFirst(st, planner));
                    break;
                case "threats":
                    compList.add(new ThreatsFirst());
                    break;
                default:
                    throw new FAPEException("Unrecognized flaw comparator option: " + compID);
            }
        }
        return new SeqFlawComparator(st, planner, compList);
    }
}
