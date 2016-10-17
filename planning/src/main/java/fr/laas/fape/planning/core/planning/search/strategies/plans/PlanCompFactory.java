package fr.laas.fape.planning.core.planning.search.strategies.plans;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.strategies.plans.tsp.Htsp;
import fr.laas.fape.planning.core.planning.search.strategies.plans.tsp.MinSpanTreeComp;
import fr.laas.fape.planning.exceptions.FAPEException;

import java.util.LinkedList;
import java.util.List;

public class PlanCompFactory {

    public static SeqPlanComparator get(Planner planner, List<String> comparators) {
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
                case "opengoals":
                    compList.add(new OpenGoals());
                    break;
                case "soca":
                    compList.add(new SOCA(planner));
                    break;
                case "lfr":
                    compList.add(new LeastFlawRatio());
                    break;
                case "ord-dec":
                    compList.add(new OrderedDecompositions());
                    break;
                case "tsp":
                    compList.add(new Htsp(Htsp.DistanceEvaluationMethod.valueOf("tdtg")));
                    break;
                case "minspan":
                    compList.add(new MinSpanTreeComp());
                    break;
                case "makespan":
                    compList.add(new MakespanComp());
                    break;
                default:
                    if(compID.startsWith("tsp-"))
                        compList.add(new Htsp(Htsp.DistanceEvaluationMethod.valueOf(compID.replace("tsp-",""))));
                    else
                        throw new FAPEException("Unrecognized plan comparator option: " + compID);
            }
        }
        return new SeqPlanComparator(compList);
    }
}
