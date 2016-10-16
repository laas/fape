package fape;

import com.martiansoftware.jsap.*;
import fape.core.planning.heuristics.temporal.DGHandler;
import fape.core.planning.planner.GlobalOptions;
import fape.core.planning.planner.Planner;
import fape.core.planning.planner.PlanningOptions;
import fape.core.planning.search.flaws.finders.NeededObservationsFinder;
import fape.core.planning.search.flaws.flaws.mutexes.MutexesHandler;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import fape.util.Configuration;
import fape.util.TinyLogger;
import fape.util.Utils;
import fr.laas.fape.exceptions.InconsistencyException;
import planstack.anml.model.AnmlProblem;
import planstack.constraints.stnu.Controllability;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Planning {

    public static boolean quiet = false;
    public static boolean verbose = false;

    final private static List<String> flat_plan_sel = Collections.singletonList("minspan");
    final private static List<String> hier_plan_sel = Arrays.asList("dfs","ord-dec","soca");
    final private static List<String> flat_flaw_sel = Arrays.asList("hier","ogf","abs","lcf","eogf");
    final private static List<String> hier_flaw_sel = Arrays.asList("earliest", "threats","hier-fifo","ogf","abs","lcf");
    final private static boolean flat_use_epsilon = true;
    final private static boolean hier_use_epsilon = false;

    public static SimpleJSAP getCommandLineParser(boolean isAnmlFileRequired) throws JSAPException {
        return new SimpleJSAP(
                "FAPE",
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
                        new Switch("mutex", 'm', "mutex", "[experimental] Use mutex for temporal reasoning as in CPT."),
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
                        new FlaggedOption("threats-early-check")
                                .setStringParser(JSAP.BOOLEAN_PARSER)
                                .setLongFlag("threats-early-check")
                                .setDefault("true")
                                .setHelp("If true, the planner will check if a resolver will necessarily result in a threat. " +
                                "This allows an early filtering of resolvers."),
                        new FlaggedOption("a-epsilon")
                                .setStringParser(JSAP.BOOLEAN_PARSER)
                                .setShortFlag('e')
                                .setLongFlag("ae")
                                .setHelp("The planner will use an A-Epsilon search. " +
                                "The default is "+ hier_use_epsilon +" for entirely hierarchical " +
                                "domains and "+ flat_use_epsilon +" for others."),
                        new QualifiedSwitch("action-insertion")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('i')
                                .setLongFlag("action-insertion")
                                .setDefault("dec")
                                .setHelp("Defines which strategy to use for action insertion. \"dec\" will systematically decompose " +
                                "the existing task network and only consider inserting action that are not task dependent. " +
                                "\"local\" will insert actions locally as supporters for open goals before linking them to the task network."),
                        new QualifiedSwitch("reachability-graph")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('g')
                                .setLongFlag("reach-graph")
                                .setDefault("full")
                                .setHelp("Planner will use dependency graphs to preform reachability analysis " +
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
                                .setHelp("A comma separated list of plan selectors, ordered by priority." +
                                "Plan selectors assign a priority to each partial plans in the queue. The partial plan " +
                                "with the highest priority is expanded next. The main options are: \n" +
                                " - \"soca\" that simply compare the number of flaws and actions is the partial plans\n" +
                                " - \"dfs\": Deepest partial plan extracted first.\n" +
                                " - \"bfs\": Shallowest partial plan extracted first.\n" +
                                " - \"ord-dec\": Tries method in the order they are defined (should be combined with dfs)\n" +
                                "If more than one selector is given, the " +
                                "second is used to break ties  of the first one, the third to break ties of both " +
                                "the first and second, etc. Plan selectors can be found in the package " +
                                "\"fape.core.planning.search.strategies.plans\".\n" +
                                "The default is "+hier_plan_sel+" for entirely hierarachical domains and "+flat_plan_sel+" " +
                                "for others."),
                        new FlaggedOption("flaw-selection")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('f')
                                .setLongFlag("flaw-selection")
                                .setList(true)
                                .setListSeparator(',')
                                .setHelp("Ordered list of flaw selectors. Each flaw selector assigns a priority to each " +
                                "flaw. The first selector has the highest weight, the last has the least weight." +
                                "The flaw with highest priority is selected to be solved. A (non-exaustive) of flaw selectors:\n" +
                                "- 'hier': For hierarchical planning: the planner will first select unrefined tasks by increasing earliest appearance," +
                                " then unmotivated dependent actions. In the case where all actions are task dependent (i.e. fully hierarchical domain)," +
                                " threats are given the highest priority.\n" +
                                "- 'ogf': Open goal flaws first.\n" +
                                "- 'abs': Gives higher priority to flaws high in the abstraction hierarchy of the problem.\n" +
                                "- 'lcf': Least Commiting First, select the flaw with the least number of resolvers\n" +
                                "- 'eogf': Gives higher priority to open goals that are close to the time origin.\n" +
                                "- 'hier-fifo': always selects the unrefined task that has been pending for the longest time.\n" +
                                "The default is "+hier_flaw_sel+" for entirely hierarchical domains and "+flat_flaw_sel+" for others."),
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
                        new FlaggedOption("bind")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setLongFlag("bind")
                                .setList(true)
                                .setListSeparator(',')
                                .setHelp("bind a hidden parameter to a given value. For instance '--bind key1=xxx,key2=3.7"),
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
        Planning.quiet = commandLineConfig.getBoolean("quiet");
        Planning.verbose = commandLineConfig.getBoolean("verbose");
        Planner.debugging = commandLineConfig.getBoolean("debug");

        String[] configFiles = commandLineConfig.getStringArray("anml-file");
        List<String> anmlFiles = new LinkedList<>();

        Arrays.stream(commandLineConfig.getStringArray("bind")).forEach(kv -> {
            String[] arr = kv.split("=");
            GlobalOptions.setOption(arr[0], arr[1]);
        });

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
        writer.write("iter, runtime, planning-time, anml-file, opened-states, generated-states, " +
                "fast-forwarded, sol-depth, flaw-sel, plan-sel, reachability, fast-forward, ae\n");

        boolean allSolved = true;

        final int repetitions = commandLineConfig.getInt("repetitions");
        for (int i = 0; i < repetitions; i++) {

            for (String anmlFile : anmlFiles) {

                final AnmlProblem pb = new AnmlProblem();
                try {
                    pb.extendWithAnmlFile(anmlFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Problem with ANML file: " + anmlFile);
                    System.err.println((new File(anmlFile)).getAbsolutePath());
                    return;
                }


                // read default conf from file and add command line options on top
                Configuration config = new Configuration(commandLineConfig, getAssociatedConfigFile(anmlFile));



                // creates the planner that will be tested for this problem
                List<String> planStrat =
                        config.specified("plan-selection") ?
                                Arrays.asList(config.getStringArray("plan-selection")) :
                                pb.allActionsAreMotivated() ?
                                        hier_plan_sel : flat_plan_sel;
                List<String> flawStrat =
                        config.specified("flaw-selection") ?
                                Arrays.asList(config.getStringArray("flaw-selection")) :
                                pb.allActionsAreMotivated() ?
                                        hier_flaw_sel : flat_flaw_sel;

                if(!quiet) {
                    System.out.println(pb.allActionsAreMotivated() ?
                            "Problem is entirely hierarchical (all actions are motivated)": "Non-hierarchical problem (some actions are not motivated)");
                    System.out.println("Plan selection strategy: "+planStrat);
                    System.out.println("Flaw selection strategy: "+flawStrat);
                }

                final int maxtime = config.getInt("max-time");
                final int maxDepth = config.getInt("max-depth");
                final boolean incrementalDeepening = config.getBoolean("inc-deep");
                long planningStart;

                System.gc(); // clean up previous runs to avoid impact on performance measure

                long start = System.currentTimeMillis();

                PlanningOptions options = new PlanningOptions(planStrat, flawStrat);
                options.useFastForward = config.getBoolean("fast-forward");

                options.useAEpsilon = config.specified("a-epsilon") ?
                        config.getBoolean("a-epsilon") :
                        pb.allActionsAreMotivated() ?
                                hier_use_epsilon : flat_use_epsilon;

                options.displaySearch = config.getBoolean("display-search");
                options.actionsSupportMultipleTasks = config.getBoolean("multi-supports");
                options.checkUnsolvableThreatsForOpenGoalsResolvers = config.getBoolean("threats-early-check");

                if(config.getBoolean("needed-observations"))
                    options.flawFinders.add(new NeededObservationsFinder());

                switch(config.getString("action-insertion")) {
                    case "local":
                        options.actionInsertionStrategy = PlanningOptions.ActionInsertionStrategy.UP_OR_DOWN;
                        break;
                    case "dec":
                        options.actionInsertionStrategy = PlanningOptions.ActionInsertionStrategy.DOWNWARD_ONLY;
                        break;
                    default:
                        throw new FAPEException("Invalid option for action insertion: "+config.getString("action-insertion"));
                }

                if(config.getBoolean("reachability-graph") && !config.getString("reachability-graph").equals("none")) {
                    options.handlers.add(new DGHandler());
                    String degGraphOption = config.getString("reachability-graph");
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
                            assert degGraphOption.startsWith("maxiter") : "Invalid parameter for the dependency graph option: "+degGraphOption;
                            options.depGraphStyle = "full";
                            options.depGraphMaxIters = Integer.parseInt(degGraphOption.replaceFirst("maxiter", ""));
                    }
                }
                if(config.getBoolean("mutex")) {
                    options.handlers.add(new MutexesHandler());
                }
                State iniState = null;
                Planner planner = null;
                try {
                    iniState = new State(pb, Controllability.PSEUDO_CONTROLLABILITY);
                    planner = new Planner(iniState, options);
                } catch (InconsistencyException e) {
                    System.out.println("Inconsistency in the problem definition");
                    e.printStackTrace();
                    e.getCause().printStackTrace();
                    System.exit(1);
                }
                boolean failure;
                State sol;
                try {
                    planningStart = System.currentTimeMillis();
                    sol = planner.search(planningStart + 1000 * maxtime, maxDepth, incrementalDeepening);
                    failure = sol == null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.err.println("Planning finished for " + anmlFile + " with failure: "+ex);
                    continue;
                }
                long end = System.currentTimeMillis();
                float total = (end - start) / 1000f;
                String time;
                String planningTime = Float.toString((end - planningStart) / 1000f);
                if (failure) {
                    allSolved = false;
                    if(planner.planState == Planner.EPlanState.TIMEOUT)
                        time = "TIMEOUT";
                    else if(planner.planState == Planner.EPlanState.INFEASIBLE)
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
                    System.out.println("Makespan: "+sol.getMakespan());
                    System.out.println("Num actions: "+sol.getAllActions().size());
                    System.out.println();
                    System.out.println("=== Timelines === \n" + Printer.timelines(sol));
                    System.out.println("\n=== Actions ===\n"+Printer.actionsInState(sol));
                }

                final String reachStr = config.getString("reachability-graph");
                final String ffStr = config.getBoolean("fast-forward") ? "ff" : "no-ff";
                final String aeStr = options.useAEpsilon ? "ae" : "no-ae";

                writer.write(
                        i + ", "
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
