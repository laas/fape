package fape;

import com.martiansoftware.jsap.*;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planner.PlannerFactory;
import fape.core.planning.planner.PlanningOptions;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.util.Configuration;
import fape.util.TinyLogger;
import fape.util.Utils;
import planstack.anml.model.AnmlProblem;
import planstack.constraints.stnu.Controllability;

import java.io.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Planning {

    static class PlannerConf {
        final String plannerID;
        final PlanningOptions options;
        final Controllability controllability;

        public PlannerConf(String plannerID, String[] planStrat, String[] flawStrat, Controllability controllability, boolean fastForward, boolean aEpsilon) {
            this.plannerID = plannerID;
            this.options = new PlanningOptions(planStrat, flawStrat);
            this.options.useFastForward = fastForward;
            this.controllability = controllability;
            this.options.useAEpsilon = aEpsilon;
        }

        public boolean usesActionConditions() {
            return this.plannerID.equals("taskcond") || this.plannerID.equals("pgr");
        }
    }

    public static SimpleJSAP getCommandLineParser(boolean isAnmlFileRequired) throws JSAPException {
        return new SimpleJSAP(
                "FAPE planners",
                "Solves ANML problems",
                new Parameter[]{
                        new Switch("verbose", 'v', "verbose", "Requests verbose output. Every search node will be displayed."),
                        new Switch("quiet", 'q', "quiet", "Planner won't print the final plan."),
                        new Switch("debug", 'd', "debug", "Set the planner in debugging mode. "
                                + "Mainly consists in time consuming checks."),
                        new Switch("actions-chart", JSAP.NO_SHORTFLAG, "gui", "Planner will show the actions on a time chart."),
                        new Switch("display-search", JSAP.NO_SHORTFLAG, "display-search", "Search will be displayed online."),
                        new Switch("dispatchable", JSAP.NO_SHORTFLAG, "dispatchable", "FAPE will build a dispatchable Plan. "+
                                "This is step mainly involves building a dynamically controllable STNU that is used to check " +
                                "which actions can be dispatched."),
                        new FlaggedOption("plannerID")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('p')
                                .setLongFlag("planner")
                                .setDefault("fape")
                                .setHelp("Defines which planner implementation to use. Possible values are:\n"
                                        + "  - topdown: Traditional top-down HTN planning.\n"
                                        + "  - fape: Cool planning \\o/.\n"),
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
                        new FlaggedOption("inc-deep")
                                .setStringParser(JSAP.BOOLEAN_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("inc-deep")
                                .setDefault("false")
                                .setHelp("The planner will use incremental deepening until a solution is found or the maximum depth is reached. " +
                                        "To be efficient this option should probably be used with \"--strats lcf%dfs\", which will reduce the branching " +
                                        "factor and search in depth first (reducing memory consumption)."),
                        new FlaggedOption("fast-forward")
                                .setStringParser(JSAP.BOOLEAN_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("fast-forward")
                                .setDefault("true")
                                .setHelp("If true, all trivial flaws (with one resolver) will be solved before inserting a state into the queue." +
                                        "This results in additional computations as some time is spent computing flaws and resolver for partial plans that " +
                                        "might never be expanded. On the other hand, it can result in a better heuristic information because states in " +
                                        "the queue are as advanced as possible. Also the total number of states in the queue is often reduces which means less" +
                                        " heuristics have to be computed."),
                        new FlaggedOption("A-Epsilon")
                                .setStringParser(JSAP.BOOLEAN_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("ae")
                                .setDefault("true")
                                .setHelp("The planner will use an A-Epsilon search with epsilon = 0.3. The epsilon can not be " +
                                        "parameterized through command line yet."),
                        new FlaggedOption("reachability")
                                .setStringParser(JSAP.BOOLEAN_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("reachability")
                                .setDefault("true")
                                .setHelp("Planner will make a reachability analysis of each expanded node. This check mainly" +
                                        "consists in an analysis on a ground version of the problem, checking both causal and" +
                                        "hierarchical properties of a partial plan."),
                        new FlaggedOption("repetitions")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setShortFlag('n')
                                .setLongFlag(JSAP.NO_LONGFLAG)
                                .setDefault("1")
                                .setHelp("Number of times to repeat all planning activities. This might be used to (i) " +
                                        "check that FAPE indeed get the same plan/search space (ii) get realistic runtime" +
                                        " after warming up the JVM."),
                        new FlaggedOption("strategies")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("strats")
                                .setDefault("%")
                                .setHelp("This is used to define search strategies. A search strategy consists of "
                                        + "one or more flaw selection strategy and one or more state selection strategy. "
                                        + "Ex: the argument 'lcf:lfr%dfs' would give for flaws: lcf (LeastCommitingFirst), using "
                                        + "lfr (LeastFlawRatio) to handle ties. States would be selected using dfs (DepthFirstSearch). "
                                        + "For more information on search strategies, look at the fape.core.planning.search.strategies "
                                        + "package."),
                        new FlaggedOption("output")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('o')
                                .setLongFlag("output")
                                .setDefault("stdout")
                                .setHelp("File to which the CSV formatted output will be written"),
//                        new FlaggedOption("stnu-consistency")
//                                .setStringParser(JSAP.STRING_PARSER)
//                                .setShortFlag(JSAP.NO_SHORTFLAG)
//                                .setLongFlag("stnu")
//                                .setRequired(false)
//                                .setDefault("pseudo")
//                                .setHelp("Selects which type of STNU controllability should be checked while searching for a solution. "
//                                        +"Note that dynamic controllability will be checked when a plan is found regardless of this option. "
//                                        +"This is simply used to define which algorithm is used while searching, with an impact on earliness "
//                                        +"of failures and computation time. Accepted options are:\n"
//                                        +"  - 'stn': simply enforces requirement constraints.\n"
//                                        +"  - 'pseudo': enforces pseudo controllability.\n"
//                                        +"  - 'dynamic': [experimental] enforces dynamic controllability.\n"),
                        new UnflaggedOption("anml-file")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setRequired(isAnmlFileRequired)
                                .setGreedy(true)
                                .setHelp("ANML problem files on which to run the planners. If it is set "
                                        + "to a directory, all files ending with .anml will be considered. "
                                        + "If a file of the form xxxxx.yy.pb.anml is given, the file xxxxx.dom.anml will be loaded first."
                                        + "If a file xxxxx.conf is present, it will be used to provide default options of the planner.\n")

                }
        );
    }

    public static String getAssociatedConfigFile(String anmlFile) {
        assert anmlFile.endsWith(".anml") : anmlFile+" is not a valid problem file.";
        File f = new File(anmlFile);
        String domainName = f.getName().substring(0, f.getName().indexOf("."));
        File confFile = new File(f.getParentFile(), domainName+".conf");
        return confFile.getPath();
    }

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = getCommandLineParser(true);

        JSAPResult commandLineConfig = jsap.parse(args);
        if (jsap.messagePrinted()) {
            return ;
        }

        Writer writer;
        if (commandLineConfig.getString("output").equals("stdout")) {
            writer = new OutputStreamWriter(System.out);
        } else {
            writer = new FileWriter(commandLineConfig.getString("output"));
        }

        TinyLogger.logging = commandLineConfig.getBoolean("verbose");
        APlanner.debugging = commandLineConfig.getBoolean("debug");
//        Plan.makeDispatchable = commandLineConfig.getBoolean("dispatchable");

        String[] configFiles = commandLineConfig.getStringArray("anml-file");
        List<String> anmlFiles = new LinkedList<>();

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
                    if(anmlFile.getName().endsWith(".anml") && !anmlFile.getName().endsWith(".dom.anml"))
                    anmlFiles.add(anmlFile.getPath());
                }
            } else {
                if(path.endsWith(".anml") && !path.endsWith(".dom.anml"))
                    anmlFiles.add(path);
            }
        }

        Collections.sort(anmlFiles);

        // output format
        writer.write("iter, planner, runtime, planning-time, anml-file, opened-states, generated-states, " +
                "fast-forwarded, sol-depth, flaw-sel, plan-sel, reachability, fast-forward, ae\n");

        final int repetitions = commandLineConfig.getInt("repetitions");
        for (int i = 0; i < repetitions; i++) {

            for (String anmlFile : anmlFiles) {

                Configuration config = new Configuration(commandLineConfig, getAssociatedConfigFile(anmlFile));

                Controllability controllability = Controllability.PSEUDO_CONTROLLABILITY;

                // creates all planners that will be tested for this problem
                String strategy = config.getString("strategies");
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

                String plannerID = config.getString("plannerID");

                final boolean fastForward = config.getBoolean("fast-forward");
                final boolean aEpsilon = config.getBoolean("A-Epsilon");
                final int maxtime = config.getInt("max-time");
                final int maxDepth = config.getInt("max-depth");
                final boolean incrementalDeepening = config.getBoolean("inc-deep");
                long planningStart = 0;

                PlannerConf conf = new PlannerConf(plannerID, planStrat, flawStrat, controllability, fastForward, aEpsilon);

                System.gc(); // clean up previous runs to avoid impact on performance measure

                long start = System.currentTimeMillis();

                conf.options.usePlanningGraphReachability = config.getBoolean("reachability");
                conf.options.displaySearch = config.getBoolean("display-search");

                final AnmlProblem pb = new AnmlProblem();
                try {
                    pb.extendWithAnmlFile(anmlFile);

                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Problem with ANML file: " + anmlFile);
                    System.err.println((new File(anmlFile)).getAbsolutePath());
                    return;
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
                    if(config.getBoolean("actions-chart"))
                        planner.drawState(sol);
                }

                if (!failure && !config.getBoolean("quiet")) {
                    System.out.println("=== Temporal databases === \n" + Printer.temporalDatabaseManager(sol));
                    System.out.println("\n=== Actions ===\n"+Printer.actionsInState(sol));
                }

                final String reachStr = config.getBoolean("reachability") ? "reach" : "no-reach";
                final String ffStr = fastForward ? "ff" : "no-ff";
                final String aeStr = aEpsilon ? "ae" : "no-ae";

                writer.write(
                        i + ", "
                                + planner.shortName() + ", "
                                + time + ", "
                                + planningTime + ", "
                                + anmlFile + ", "
                                + planner.expandedStates + ", "
                                + planner.GeneratedStates + ", "
                                + planner.numFastForwarded + ", "
                                + (failure ? "-" : sol.depth) + ", "
                                + Utils.print(planner.options.flawSelStrategies, ":") + ", "
                                + Utils.print(planner.options.planSelStrategies, ":") + ", "
                                + reachStr + ", "
                                + ffStr + ", "
                                + aeStr
                                + "\n");
                writer.flush();

            }
        }
        if (!commandLineConfig.getString("output").equals("stdout"))
            writer.close();
    }
}
