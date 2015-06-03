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
                case "fex":
                    compList.add(new Fex());
                case "soca":
                    compList.add(new SOCA(planner));
                    break;
//                case "metric":
//                    compList.add(new Metric());
//                    break;
                case "psaoca":
                    compList.add(new RootsOCA());
                    break;
                case "lfr":
                    compList.add(new LeastFlawRatio());
                    break;
                case "lmc":
                    compList.add(new LMC());
                    break;
                case "actions-10-cons-3":
                    compList.add(new Actions10Consumers3());
                    break;
                case "hcl":
                    compList.add(new HierarchicalCausalLinks());
                    break;
                case "rplan":
                    compList.add(new RPGComp());
                    break;
                case "base":
                    compList.add(new Basic(planner));
                    break;
                default:
                    throw new FAPEException("Unrecognized flaw comparator option: " + compID);
            }
        }
        return new SeqPlanComparator(compList);
    }
}
