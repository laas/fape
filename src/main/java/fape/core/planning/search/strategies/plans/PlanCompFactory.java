package fape.core.planning.search.strategies.plans;

import fape.core.planning.planner.APlanner;
import fape.exceptions.FAPEException;

import java.util.LinkedList;
import java.util.List;

public class PlanCompFactory {

    public static SeqPlanComparator get(APlanner planner, String... comparators) {
        List<PartialPlanComparator> compList = new LinkedList<>();
        for (String compID : comparators) {
            switch (compID) {
                case "bfs":
                    compList.add(new BreadthFirst());
                    break;
                case "dfs":
                    compList.add(new DepthFirst());
                    break;
                case "unbound":
                    compList.add(new NumUnboundVariables());
                    break;
                case "threats":
                    compList.add(new Threats());
                    break;
                case "soca":
                    compList.add(new SOCA(planner));
                    break;
                case "lfr":
                    compList.add(new LeastFlawRatio());
                    break;
                case "hcl":
                    compList.add(new HierarchicalCausalLinks());
                    break;
                case "rplan":
                    compList.add(new RPGComp(planner));
                    break;
                default:
                    throw new FAPEException("Unrecognized flaw comparator option: " + compID);
            }
        }
        return new SeqPlanComparator(compList);
    }
}
