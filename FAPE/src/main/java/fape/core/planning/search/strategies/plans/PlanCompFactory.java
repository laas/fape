package fape.core.planning.search.strategies.plans;

import fape.exceptions.FAPEException;

import java.util.LinkedList;
import java.util.List;

public class PlanCompFactory {

    public static PartialPlanComparator get(String... comparators) {
        List<PartialPlanComparator> compList = new LinkedList<>();
        for (String compID : comparators) {
            switch (compID) {
                case "bfs":
                    compList.add(new BreadthFirst());
                    break;
                case "dfs":
                    compList.add(new DepthFirst());
                    break;
                case "soca":
                    compList.add(new SOCA());
                    break;
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
                default:
                    throw new FAPEException("Unrecognized flaw comparator option: " + compID);
            }
        }
        return new SeqPlanComparator(compList);
    }
}
