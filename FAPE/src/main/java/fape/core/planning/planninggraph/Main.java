package fape.core.planning.planninggraph;

import planstack.anml.model.AnmlProblem;
import planstack.anml.parser.ANMLFactory;


public class Main {



    public static void main(String[] args) {
        String pbFile;
        if(args.length > 0)
            pbFile = args[0];
        else
            pbFile = "../fape/FAPE/problems/Dream3.anml";

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
        rpg.graph.exportToDotFile("graphplan.dot", new PGPrinter(gpb));

        int x = 0;
    }
}
