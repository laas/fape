package fape;

import com.martiansoftware.jsap.*;
import fape.core.planning.heuristics.temporal.DGHandler;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planner.PlannerFactory;
import fape.core.planning.planner.PlanningOptions;
import fape.core.planning.search.flaws.finders.NeededObservationsFinder;
import fape.core.planning.search.strategies.plans.tsp.HTSPHandler;
import fape.core.planning.search.flaws.flaws.mutexes.MutexesHandler;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.util.Configuration;
import fape.util.TinyLogger;
import fape.util.Utils;
import planstack.anml.model.AnmlProblem;
import planstack.constraints.stnu.Controllability;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Planning {

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
                        new Switch("dispatchable", JSAP.NO_SHORTFLAG, "dispatchable", "[experimental] FAPE will build a dispatchable Plan. "+
                                "This is step mainly involves building a dynamically controllable STNU that is used to check " +
                                "which actions can be dispatched."),
                        new Switch("salesman", 's', "salesman", "[experimental] activates TSP heuristic"),
                        new Switch("mutex", 'm', "mutex", "[experimental] Use mutex for temporal reasoning as in CPT."),
                        new FlaggedOption("plannerID")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setLongFlag("planner")
                                .setDefault("fape")
                                .setHelp("Defines which planner implementation to use. Possible values are:\n"
                                        + "  - topdown: Traditional top-down HTN planning (lacks completeness for ANML).\n"
                                        + "  - fape: Complete planner that allows going either down or up in action hierarchies.\n"),
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
                                .setLongFlag("fast-forward")
                                .setDefault("true")
                                .setHelp("If true, all trivial flaws (with one resolver) will be solved before inserting a state into the queue." +
                                        "This results in additional computations as some time is spent computing flaws and resolver for partial plans that " +
                                        "might never be expanded. On the other hand, it can result in a better heuristic information because states in " +
                                        "the queue are as advanced as possible. Also the total number of states in the queue is often reduces which means less" +
                                        " heuristics have to be computed."),
                        new FlaggedOption("a-epsilon")
                                .setStringParser(JSAP.FLOAT_PARSER)
                                .setShortFlag('e')
                                .setLongFlag("ae")
                                .setDefault("0.3")
                                .setHelp("The planner will use an A-Epsilon search with the given epsilon value. " +
                                "If set to 0, search is a standard A*."),
                        new FlaggedOption("reachability")
                                .setStringParser(JSAP.BOOLEAN_PARSER)
                                .setShortFlag('r')
                                .setLongFlag("reachability")
                                .setDefault("false")
                                .setHelp("Planner will make a reachability analysis of each expanded node. This check mainly" +
                                        "consists in an analysis on a ground version of the problem, checking both causal and" +
                                        "hierarchical properties of a partial plan."),
                        new QualifiedSwitch("dependency-graph")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('g')
                                .setLongFlag("dep-graph")
                                .setDefault("full")
                                .setHelp("[experimental] Planner will use dependency graphs to preform reachability analysis " +
                                "and compute admissible temporal heuristics. Possible parameters are `full` (complete model)," +
                                " `popf` (model with no negative edges), `base` (model with complex actions) and `maxiterXX`" +
                                " (same as full but the number of iterations is limited to XX). This option is currently not " +
                                "compatible with the 'reachability' option and the 'rplan' plan selector."),
                        new FlaggedOption("multi-supports")
                                .setStringParser(JSAP.BOOLEAN_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("multi-supports")
                                .setDefault("false")
                                .setHelp("[experimental] Allow an action to support mutliple tasks"),
                        new FlaggedOption("repetitions")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setShortFlag('n')
                                .setLongFlag(JSAP.NO_LONGFLAG)
                                .setDefault("1")
                                .setHelp("Number of times to repeat all planning activities. This might be used to (i) " +
                                        "check that FAPE indeed get the same plan/search space (ii) get realistic runtime" +
                                        " after warming up the JVM."),
                        new FlaggedOption("plan-selection")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('p')
                                .setLongFlag("plan-selection")
                                .setList(true)
                                .setListSeparator(',')
                                .setDefault("tsp,soca")
                                .setHelp("A comma separated list of plan selectors, ordered by priority." +
                                "Plan selectors assign a priority to each partial plans in the queue. The partial plan " +
                                "with the highest priority is expanded next. The main options are: \n" +
                                " - \"rplan\": evaluates the remaining search effort by building a relaxed plan\n" +
                                " - \"soca\" that simply compare the number of flaws and actions is the partial plans\n" +
                                " - \"dfs\": Deepest partial plan extracted first.\n" +
                                " - \"bfs\": Shallowest partial plan extracted first.\n" +
                                "If more than one selector is given, the " +
                                "second is used to break ties  of the first one, the third to break ties of both " +
                                "the first and second, etc. Plan selectors can be found in the package " +
                                "\"fape.core.planning.search.strategies.plans\".\n"),
                        new FlaggedOption("flaw-selection")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('f')
                                .setLongFlag("flaw-selection")
                                .setList(true)
                                .setListSeparator(',')
                                .setDefault("hf,ogf,abs,lcf,eogf")
                                .setHelp("Ordered list of flaw selectors. Each flaw selector assigns a priority to each " +
                                "flaw. The first selector has the highest weight, the last has the least weight." +
                                "The flaw with highest priority is selected to be solved. A (non-exaustive) of flaw selectors:\n" +
                                "- 'hf': Hierarchical flaws first.\n" +
                                "- 'ogf': Open goal flaws first.\n" +
                                "- 'abs': Gives higher priority to flaws high in the abstraction hierarchy of the problem.\n" +
                                "- 'lcf': Least Commiting First, select the flaw with the least number of resolvers\n" +
                                "- 'eogf': Gives higher priority to open goals that are close to the time origin.\n"),
                        new FlaggedOption("output")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('o')
                                .setLongFlag("output")
                                .setDefault("stdout")
                                .setHelp("File to which the CSV formatted output will be written."),
                        new FlaggedOption("needed-observations")
                                .setStringParser(JSAP.BOOLEAN_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("needed-observations")
                                .setDefault("false")
                                .setHelp("[experimental] This is an EXPERIMENTAL feature that currently works only on the rabbits domain. " +
                                        "Setting this option to true will make the planner assume that a contingent event is observable " +
                                        "only if an agent is in the area where this event occurs. Consequently modification to the plan might " +
                                        "be required to make sure the plan is dynamically controllable."),
                        new UnflaggedOption("anml-file")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setRequired(isAnmlFileRequired)
                                .setGreedy(true)
                                .setHelp("ANML problem files on which to run the planners. If it is set "
                                + "to a directory, all files ending with .anml will be considered. "
                                + "If a file of the form xxxxx.yy.pb.anml is given, the file xxxxx.dom.anml " +
                                "will be loaded first. If a file xxxxx.conf is present, it will be used to provide " +
                                "default options of the planner.\n")
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

        String[] configFiles = commandLineConfig.getStringArray("anml-file");
        List<String> anmlFiles = new LinkedList<>();

        for (String path : configFiles) {
            File f = new File(path);
            if (f.isDirectory()) {
                // all files ending with ".anml" except those ending with ".dom.anml"
                File[] anmls = f.listFiles((File fi) -> fi.getName().endsWith(".anml") && !fi.getName().endsWith(".dom.anml"));
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

        boolean allSolved = true;

        final int repetitions = commandLineConfig.getInt("repetitions");
        for (int i = 0; i < repetitions; i++) {

            for (String anmlFile : anmlFiles) {
                // read default conf from file and add command line options on top
                Configuration config = new Configuration(commandLineConfig, getAssociatedConfigFile(anmlFile));

                // creates the planner that will be tested for this problem
                String[] planStrat = config.getStringArray("plan-selection");
                String[] flawStrat = config.getStringArray("flaw-selection");
                String plannerID = config.getString("plannerID");

                final int maxtime = config.getInt("max-time");
                final int maxDepth = config.getInt("max-depth");
                final boolean incrementalDeepening = config.getBoolean("inc-deep");
                long planningStart = 0;

                System.gc(); // clean up previous runs to avoid impact on performance measure

                long start = System.currentTimeMillis();

                PlanningOptions options = new PlanningOptions(planStrat, flawStrat);
                options.useFastForward = config.getBoolean("fast-forward");
                options.useAEpsilon = config.getFloat("a-epsilon") > 0;
                if(options.useAEpsilon) {
                    options.epsilon = config.getFloat("a-epsilon");
                }
                options.usePlanningGraphReachability = config.getBoolean("reachability") || Arrays.asList(planStrat).contains("rplan");
                options.displaySearch = config.getBoolean("display-search");
                options.actionsSupportMultipleTasks = config.getBoolean("multi-supports");

                if(config.getBoolean("needed-observations"))
                    options.flawFinders.add(new NeededObservationsFinder());

                if(config.getBoolean("salesman"))
                    options.handlers.add(new HTSPHandler());

                if(config.getBoolean("dependency-graph") && !config.getString("dependency-graph").equals("none")) {
                    options.handlers.add(new DGHandler());
                    String degGraphOption = config.getString("dependency-graph");
                    switch (degGraphOption) {
                        case "full":
                            options.depGraphStyle = "full";
                            break;
                        case "popf":
                            options.depGraphStyle = "popf";
                            break;
                        case "base":
                            options.depGraphStyle = "base";
                            break;
                        default:
                            assert degGraphOption.startsWith("maxiter") : "Invalid parameter for the dependency graph option.";
                            options.depGraphStyle = "full";
                            options.depGraphMaxIters = Integer.parseInt(degGraphOption.replaceFirst("maxiter", ""));
                    }
                }
                if(config.getBoolean("mutex")) {
                    options.handlers.add(new MutexesHandler());
                }

                final AnmlProblem pb = new AnmlProblem();
                try {
                    pb.extendWithAnmlFile(anmlFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Problem with ANML file: " + anmlFile);
                    System.err.println((new File(anmlFile)).getAbsolutePath());
                    return;
                }
                if(APlanner.debugging)
                    System.out.println(pb.allActionsAreMotivated() ?
                            "Problem is entirely hierarchical (all actions are motivated)": "Non-hierarchical problem (some actions are not motivated)");
                final State iniState = new State(pb, Controllability.PSEUDO_CONTROLLABILITY);
                final APlanner planner = PlannerFactory.getPlannerFromInitialState(plannerID, iniState, options);

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
                    allSolved = false;
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
                    System.out.println("Expanded states: "+planner.numExpandedStates);
                    System.out.println("Generated states: "+planner.numGeneratedStates);
                    System.out.println("Fast-Forwarded states: "+planner.numFastForwardedStates);
                    System.out.println("Num ground actions after analysis:"+planner.preprocessor.getAllActions().size());
                    System.out.println();
                    System.out.println("=== Timelines === \n" + Printer.temporalDatabaseManager(sol));
                    System.out.println("\n=== Actions ===\n"+Printer.actionsInState(sol));
                }

                final String reachStr = config.getBoolean("reachability") ? "reach" : "no-reach";
                final String ffStr = config.getBoolean("fast-forward") ? "ff" : "no-ff";
                final String aeStr = config.getFloat("a-epsilon") > 0 ? "ae" : "no-ae";

                writer.write(
                        i + ", "
                                + planner.shortName() + ", "
                                + time + ", "
                                + planningTime + ", "
                                + anmlFile + ", "
                                + planner.numExpandedStates + ", "
                                + planner.numGeneratedStates + ", "
                                + planner.numFastForwardedStates + ", "
                                + (failure ? "-" : sol.getDepth()) + ", "
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

        if(!allSolved && !commandLineConfig.getBoolean("display-search"))
            System.exit(1);
    }
}
