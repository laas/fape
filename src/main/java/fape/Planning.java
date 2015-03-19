package fape;

import com.martiansoftware.jsap.*;
import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.core.planning.planner.*;
import fape.core.planning.planninggraph.PGPlanner;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import fape.util.Utils;
import planstack.anml.model.AnmlProblem;
import planstack.anml.parser.ANMLFactory;
import planstack.constraints.stnu.Controllability;

import java.io.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Planning {

    static class PlannerConf {
        final String plannerID;
        final PlanningOptions options;
        final Controllability controllability;

        public PlannerConf(String plannerID, String[] planStrat, String[] flawStrat, Controllability controllability, boolean fastForward) {
            this.plannerID = plannerID;
            this.options = new PlanningOptions(planStrat, flawStrat);
            this.options.useFastForward = fastForward;
            this.controllability = controllability;
        }

        public boolean usesActionConditions() {
            return this.plannerID.equals("taskcond");
        }
    }

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                "FAPE planners",
                "Solves ANML problems",
                new Parameter[]{
                        new Switch("verbose", 'v', "verbose", "Requests verbose output. Every search node will be displayed."),
                        new Switch("quiet", 'q', "quiet", "Planner won't print the final plan."),
                        new Switch("debug", 'd', "debug", "Set the planner in debugging mode. "
                                + "Mainly consists in time consuming checks."),
                        new Switch("actions-chart", JSAP.NO_SHORTFLAG, "gui", "Planner will show the actions on a time chart."),
                        new Switch("dispatchable", JSAP.NO_SHORTFLAG, "dispatchable", "FAPE will build a dispatchable Plan. "+
                                "This is step mainly involves building a dynamically controllable STNU that is used to check " +
                                "which actions can be dispatched."),
                        new FlaggedOption("plannerID")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('p')
                                .setLongFlag("planner")
                                .setDefault("taskcond")
                                .setList(true)
                                .setListSeparator(',')
                                .setHelp("Defines which planner implementation to use. Possible values are:\n"
                                        + "  - htn: subtasks will be replaced with a matching action directly inserted in the plan\n"
                                        + "  - taskcond: The actions in decompositions are replaced with task conditions that "
                                        + "are fulfilled through search be linking with other actions in the plan\n"
                                        + "  \n"
                                        + " The following options are EXPERIMENTAL, they won't work on every domain:\n"
                                        + "   - base: Most basic planner that supports any domain. Uses a fully lifted representation.\n"
                                        + "   - rpg:  Planner that uses relaxed planning graphs for domain analysis. "
                                        + "Be aware that (i) it does not handle all anml problems (ii) rpg is miles away from being optimized.\n"
                                        + "   - rpg_ext: Extension of rpg that add constraint on every causal link whose supporter "
                                        + "is provided by an action. It checks (using RPG) for every ground action "
                                        + "supporting the consumer and enforce a n-ary constraint on the action's "
                                        + "parameters to make sure they fit at least one of the ground action.\n"),
                        new FlaggedOption("max-time")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setShortFlag('t')
                                .setLongFlag("timeout")
                                .setDefault("60")
                                .setHelp("Time in seconds after which a planner times out."),
                        new FlaggedOption("max-depth")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("max-depth")
                                .setDefault("999999")
                                .setHelp("Maximum depth of a solution plan. Any partial plan beyond this depth "
                                        +"will be ignored."),
                        new Switch("inc-deep",
                                JSAP.NO_SHORTFLAG,
                                "inc-deep",
                                "Planner will use incremental deepening."),
                        new Switch("fast-forward", JSAP.NO_SHORTFLAG, "ff", "All trivial flaws are solved before inserting a "+
                                "state in the queue"),
                        new FlaggedOption("repetitions")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setShortFlag('n')
                                .setLongFlag(JSAP.NO_LONGFLAG)
                                .setDefault("1")
                                .setHelp("Number of times to repeat all planning activities"),
                        new FlaggedOption("strategies")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("strats")
                                .setList(true)
                                .setListSeparator(',')
                                .setDefault("%")
                                .setHelp("This is used to define search strategies. A search strategy consists of "
                                        + "one or more flaw selection strategy and one or more state selection strategy. "
                                        + "Ex: the argument 'lcf:lfr%dfs' would give for flaws: lcf (LeastCommitingFirst), using "
                                        + "lfr (LeastFlawRatio) to handle ties. States would be selected using dfs (DepthFirstSearch). "
                                        + "For more information on search strategies, look at the fape.core.planning.search.strategies "
                                        + "package. If several strategies are given (separated by commas), they will all be attempted."),
                        new FlaggedOption("output")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('o')
                                .setLongFlag("output")
                                .setDefault("stdout")
                                .setHelp("File to which the CSV formatted output will be written"),
                        new FlaggedOption("stnu-consistency")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("stnu")
                                .setRequired(false)
                                .setDefault("pseudo")
                                .setHelp("Selects which type of STNU controllability should be checked while searching for a solution. "
                                        +"Note that dynamic controllability will be checked when a plan is found regardless of this option. "
                                        +"This is simply used to define which algorithm is used while searching, with an impact on earliness "
                                        +"of failures and computation time. Accepted options are:\n"
                                        +"  - 'stn': simply enforces requirement constraints.\n"
                                        +"  - 'pseudo': enforces pseudo controllability.\n"
                                        +"  - 'dynamic': [experimental] enforces dynamic controllability.\n"),
                        new UnflaggedOption("anml-file")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setRequired(true)
                                .setGreedy(true)
                                .setHelp("ANML problem files on which to run the planners. If it is set "
                                        + "to a directory, all files ending with .anml will be considered. "
                                        + "If a file of the form xxxxx.yy.pb.anml is given, the file xxxxx.dom.anml will be loaded first.\n")

                }
        );

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) {
            System.exit(0);
        }

        Writer writer;
        if (config.getString("output").equals("stdout")) {
            writer = new OutputStreamWriter(System.out);
        } else {
            writer = new FileWriter(config.getString("output"));
        }

        APlanner.logging = config.getBoolean("verbose");
        APlanner.debugging = config.getBoolean("debug");
        Plan.showChart = config.getBoolean("actions-chart");
        Plan.makeDispatchable = config.getBoolean("dispatchable");

        String[] configFiles = config.getStringArray("anml-file");
        List<String> anmlFiles = new LinkedList<>();

        Controllability controllability;
        switch (config.getString("stnu-consistency")) {
            case "stn": controllability = Controllability.STN_CONSISTENCY; break;
            case "pseudo": controllability = Controllability.PSEUDO_CONTROLLABILITY; break;
            case "dynamic": controllability = Controllability.DYNAMIC_CONTROLLABILITY; break;
            default: throw new RuntimeException("Unsupport option for stnu consistency: "+config.getString("stnu-consistency"));
        }
        final boolean fastForward = config.getBoolean("fast-forward");

        for (String path : configFiles) {
            File f = new File(path);
            if (f.isDirectory()) {
                File[] anmls = f.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File fi) {
                        return fi.getName().endsWith(".anml") && !fi.getName().endsWith(".dom.anml");
                    }
                });
                for (File anmlFile : anmls) {
                    anmlFiles.add(anmlFile.getPath());
                }
            } else {
                anmlFiles.add(path);
            }
        }

        Collections.sort(anmlFiles);

        // output format
        writer.write("iter, planner, runtime, planning-time, anml-file, opened-states, generated-states, fast-forwarded, sol-depth, flaw-sel, plan-sel\n");

        final int repetitions = config.getInt("repetitions");
        for (int i = 0; i < repetitions; i++) {

            for (String anmlFile : anmlFiles) {

                Queue<PlannerConf> planners = new LinkedList<>();

                // creates all planners that will be tested for this problem
                for (String strategy : config.getStringArray("strategies")) {
                    assert strategy.contains("%") : "Strategy \"" + strategy + "\" is not well formed see help.";
                    String rawFlawStrat = strategy.substring(0, strategy.indexOf('%'));
                    String rawPlanStrat = strategy.substring(strategy.indexOf('%') + 1);
                    String[] planStrat;
                    if (rawPlanStrat.isEmpty())
                        planStrat = PlannerFactory.defaultPlanSelStrategies;
                    else
                        planStrat = rawPlanStrat.split(":");
                    String[] flawStrat;
                    if (rawFlawStrat.isEmpty())
                        flawStrat = PlannerFactory.defaultFlawSelStrategies;
                    else
                        flawStrat = rawFlawStrat.split(":");

                    String[] plannerIDs = config.getStringArray("plannerID");
                    for (String plannerID : plannerIDs)
                        planners.add(new PlannerConf(plannerID, planStrat, flawStrat, controllability, fastForward));
                }

                final int maxtime = config.getInt("max-time");
                final int maxDepth = config.getInt("max-depth");
                final boolean incrementalDeepening = config.getBoolean("inc-deep");
                long planningStart = 0;

                while(!planners.isEmpty()) {
                    System.gc(); // clean up previous runs to avoid impact on performance measure

                    long start = System.currentTimeMillis();

                    PlannerConf conf = planners.remove();

                    final AnmlProblem pb = new AnmlProblem(conf.usesActionConditions());
                    try {
                        pb.extendWithAnmlFile(anmlFile);

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("Problem with ANML file: " + anmlFile);
                        System.exit(1);
                    }
                    final State iniState = new State(pb, conf.controllability);
                    final APlanner planner = PlannerFactory.getPlannerFromInitialState(conf.plannerID, iniState, conf.options);
                    APlanner.currentPlanner = planner; // this is ugly and comes from a hack from filip

                    boolean failure;
                    State sol;
                    try {
                        planningStart = System.currentTimeMillis();
                        sol = planner.search(planningStart + 1000 * maxtime, maxDepth, incrementalDeepening);
                        failure = sol == null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("Planning finished for " + anmlFile + " with failure: "+e);
                        continue;
                    }
                    long end = System.currentTimeMillis();
                    float total = (end - start) / 1000f;
                    String time;
                    String planningTime = Float.toString((end - planningStart) / 1000f);
                    if (failure) {
                        if(planner.planState == APlanner.EPlanState.TIMEOUT)
                            time = "TIMEOUT";
                        else if(planner.planState == APlanner.EPlanState.INFEASIBLE)
                            time = "INFEASIBLE";
                        else
                            time = "UNKNOWN PROBLEM";
                    } else {
                        time = Float.toString(total);
                    }

                    if (!failure && !config.getBoolean("quiet")) {
                        System.out.println("=== Temporal databases === \n" + Printer.temporalDatabaseManager(sol));
                        System.out.println("\n=== Actions ===\n"+Printer.actionsInState(sol));

                        Plan plan = new Plan(sol);
                        plan.exportToDot("plan.dot");
                        sol.exportTemporalNetwork("stn.dot");
                        sol.exportTaskNetwork("task-network.dot");
                        System.out.println("Look at stn.dot and task-network.dot for more details.");
                    }

                    writer.write(
                            i + ", "
                                    + planner.shortName() + ", "
                                    + time + ", "
                                    + planningTime + ", "
                                    + anmlFile + ", "
                                    + planner.OpenedStates + ", "
                                    + planner.GeneratedStates + ", "
                                    + planner.numFastForwarded + ", "
                                    + (failure ? "-" : sol.depth) + ", "
                                    + Utils.print(planner.options.flawSelStrategies, ":") + ", "
                                    + Utils.print(planner.options.planSelStrategies, ":")
                                    + "\n");
                    writer.flush();
                }

            }
        }
        if (!config.getString("output").equals("stdout"))
            writer.close();
    }
}
