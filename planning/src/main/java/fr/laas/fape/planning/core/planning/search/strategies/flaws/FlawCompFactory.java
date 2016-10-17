package fr.laas.fape.planning.core.planning.search.strategies.flaws;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.exceptions.FAPEException;

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
    public static FlawComparator get(State st, Planner planner, List<String> comparators) {
        List<FlawComparator> compList = new LinkedList<>();
        for (String compID : comparators) {
            switch (compID) {
                case "abs":
                    compList.add(new AbsHierarchyComp(st));
                    break;
                case "lcf":
                    compList.add(new LeastCommitingFirst(st, planner));
                    break;
                case "hier":
                    compList.add(new HierarchicalFirstComp(st, planner));
                    break;
                case "hier-fifo":
                    compList.add(new HierFIFO(st, planner));
                    break;
                case "ogf":
                    compList.add(new OpenGoalFirst());
                    break;
                case "eogf":
                    compList.add(new EarliestOpenGoalFirst(st));
                    break;
                case "extfirst":
                    compList.add(new ExtendPlanFirst(st, planner));
                    break;
                case "threats":
                    compList.add(new ThreatsFirst());
                    break;
                case "unbound":
                    compList.add(new UnboundFirst());
                    break;
                case "earliest":
                    compList.add(new EarliestFirst(st));
                    break;
                case "minspan":
                    compList.add(new MinSpanFailFirst(st));
                    break;
                default:
                    throw new FAPEException("Unrecognized flaw comparator option: " + compID);
            }
        }
        return new SeqFlawComparator(st, planner, compList);
    }
}
