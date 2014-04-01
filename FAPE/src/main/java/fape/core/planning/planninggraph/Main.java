package fape.core.planning.planninggraph;

import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.util.TimeAmount;
import planstack.anml.model.AnmlProblem;
import planstack.anml.parser.ANMLFactory;

import java.util.LinkedList;
import java.util.List;


public class Main {

    public static void main(String[] args) {
        String pbFile;
        if(args.length > 0)
            pbFile = args[0];
        else
            pbFile = "../fape/FAPE/problems/handover.anml";

        /*
        AnmlProblem pb = new AnmlProblem();
        pb.addAnml(ANMLFactory.parseAnmlFromFile(pbFile));

        GroundProblem gpb = new GroundProblem(pb);

        for(GroundAction a : gpb.allActions()) {
            if(gpb.initState.applicable(a)) {
                System.out.println("Applied: "+a);
                GroundState next = gpb.initState.apply(a);

                for(GroundAction b : gpb.allActions()) {
                    if(next.applicable(b)) {
                        System.out.println("  Applicable: " + b);
                    }
                }
            }
        }

        RelaxedPlanningGraph rpg = new RelaxedPlanningGraph(gpb);
        rpg.graph.exportToDotFile("graphplan.dot", new PGPrinter(gpb, rpg));



        List<DisjunctiveAction> enablers = new LinkedList<>();
        for(Fluent goal : gpb.goalState.fluents) {
            enablers.add(rpg.enablers(goal));
        }

        List options = new LinkedList();
        for(DisjunctiveAction da : enablers) {
            options.add(da.actionsAndParams(gpb));
        }
        */

        PGPlanner planner = new PGPlanner();
        Planner.logging = false;
        Planner.debugging = false;
        planner.Init();
        planner.ForceFact(ANMLFactory.parseAnmlFromFile(pbFile));

        if(planner.Repair(new TimeAmount(1000000))) {
            Plan plan = new Plan(planner.GetCurrentState());
        }


        int x = 0;
    }
}
