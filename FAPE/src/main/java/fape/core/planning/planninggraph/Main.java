package fape.core.planning.planninggraph;

import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.core.planning.preprocessing.AbstractionHierarchy;
import fape.core.planning.preprocessing.ActionLandmarksFinder;
import fape.core.planning.search.ActionWithBindings;
import fape.core.planning.search.SupportOption;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.util.Pair;
import fape.util.TimeAmount;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.parser.ANMLFactory;

import java.util.List;
import java.util.Set;


public class Main {

    public static void main(String[] args) {
        String pbFile;
        if(args.length > 0)
            pbFile = args[0];
        else
            pbFile = "../fape/FAPE/problems/handover.anml";

        int numIter = 1;
        if(args.length > 1) {
            numIter = Integer.parseInt(args[1]);
        }

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
        *
        for(int i=0 ; i<numIter ; i++) {
            long start, end;

            PGPlanner planner = new PGPlanner();
            Planner.logging = false;
            Planner.debugging = false;
            planner.Init();
            start = System.currentTimeMillis();
            planner.ForceFact(ANMLFactory.parseAnmlFromFile("problems/handover.domain.anml"));
            planner.ForceFact(ANMLFactory.parseAnmlFromFile("problems/handover.pb.2.anml"));
            end  = System.currentTimeMillis();
            long init = end -start;
            start = System.currentTimeMillis();
            if(planner.Repair(new TimeAmount(1000000))) {
                end = System.currentTimeMillis();
                long planning = end-start;
                System.out.print("Time: "+init+" - "+planning);
                Plan plan = new Plan(planner.GetCurrentState());
                plan.exportToDot("plan.dot");
            }

            AbstractionHierarchy hie = new AbstractionHierarchy(planner.pb);
            hie.exportToDot("abs-hie.dot");

            System.out.println("  Opened: "+planner.OpenedStates+"   Generated: "+planner.GeneratedStates);
        }
        */

        PGPlanner planner = new PGPlanner();
        Planner.logging = false;
        Planner.debugging = false;
        planner.Init();
        // long start = System.currentTimeMillis();
        planner.ForceFact(ANMLFactory.parseAnmlFromFile("problems/handover.dom.anml"));
        planner.ForceFact(ANMLFactory.parseAnmlFromFile("problems/handover.2.pb.anml"));

        ActionLandmarksFinder l = new ActionLandmarksFinder(planner.groundPB);
        for(TemporalDatabase db : planner.GetCurrentState().consumers) {
            DisjunctiveFluent df = new DisjunctiveFluent(db.stateVariable, db.GetGlobalConsumeValue(), planner.GetCurrentState().conNet.domains, planner.groundPB);
/*
            DisjunctiveAction da = l.getActionLandmarks(df);
            for(DisjunctiveFluent disPrecond : l.preconditions(da)) {
                System.out.println(disPrecond);
            }
            System.out.println();*/

            l.addGoal(df);
        }

        l.getLandmarks();
        l.removeRedundantLandmarks();
        for(DisjunctiveAction da : l.landmarks) {
            List<Pair<AbstractAction, List<Set<String>>>> toInsert = da.actionsAndParams(planner.groundPB);
            if(toInsert.size() == 1) {
                Pair<AbstractAction, List<Set<String>>> abstractAct = toInsert.get(0);
                ActionWithBindings opt = new ActionWithBindings();
                opt.act = abstractAct.value1;//Factory.getStandaloneAction(groundPB.liftedPb, supporter.value1);

                assert opt.act.args().size() == abstractAct.value2.size() : "Problem: different number of parameters";

                for(int i=0 ; i<opt.act.args().size() ; i++) {
                    opt.values.put(opt.act.args().get(i), abstractAct.value2.get(i));
                }
                SupportOption supportOption = new SupportOption();
                supportOption.actionWithBindings = opt;
                planner.ApplyOption(planner.GetCurrentState(), supportOption, null);
            }
        }

        State res = planner.search(new TimeAmount(90000));
        if(res != null) {
            Plan plan = new Plan(res);
            plan.exportToDot("plan.dot");

            System.out.println("Opened: "+planner.OpenedStates+" - Generated: "+planner.GeneratedStates);
        }

        System.out.println(l.report());
        l.exportToDot("landmarks.dot");

        int xx=0;
    }
}
