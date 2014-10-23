package fape;

import com.martiansoftware.jsap.*;
import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planner.BaseDTG;
import fape.core.planning.planner.PGExtPlanner;
import fape.core.planning.planner.TaskConditionPlanner;
import fape.core.planning.planninggraph.PGPlanner;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import fape.util.Utils;
import planstack.anml.parser.ANMLFactory;
import planstack.constraints.stnu.Controllability;

import java.io.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Planning {

    public static String currentPlanner = "";

    /**
     * Tries to infer which file contains the domain definition of this problem.
     * If the problem takes a form "domainName.xxxx.pb.anml", then the
     * corresponding domain file would be "domainName.dom.anml"
     */
    public static String domainFile(String problemFile) {
        File f = new File(problemFile);
        String name = f.getName();
        if (name.endsWith(".pb.anml")) {
            String[] nameParts = name.split("\\.");

            if (nameParts.length != 4) {
                throw new FAPEException("File name " + name + " is not correctly formatted. It should be in the form "
                        + " domainName.xxx.pb.anml and have an associated domainName.dom.anml file.");
            }

            File domain = new File(f.getParentFile(), nameParts[0] + ".dom.anml");
            if (!domain.exists()) {
                throw new FAPEException("File " + domain + " does not exists (name derived from " + problemFile + ")");
            }

            return domain.getPath();
        } else {
            return null;
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
                                .setDefault("htn")
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
                                        + "parameters to make sure they fit at least one of the ground action.\n"
                                        + "   - all:  will run every possible planner.\n"),
                        new FlaggedOption("maxtime")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setShortFlag('t')
                                .setLongFlag("timeout")
                                .setDefault("60")
                                .setHelp("Time in seconds after which a planner times out."),
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
                                .setDefault("|")
                                .setHelp("This is used to define search strategies. A search strategy consists of "
                                        + "one or more flaw selection strategy and one or more state selection strategy. "
                                        + "Ex: the argument 'lcf>lfr|dfs' would give for flaws: lcf (LeastCommitingFirst), using "
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
                                .setDefault("stn")
                                .setHelp("Selects which type of STNU controllability should be checked while searching for a solution. "
                                        +"Note that dynamic controllability will be checked when a plan is found regardless of this option. "
                                        +"This is simply used to define which algorithm is used while searching, with an impact on earliness "
                                        +"of failures and computation time. Accepted options are:\n"
                                        +"  - 'stn': simply enforces requirement constraints.\n"
                                        +"  - 'pseudo': enforces pseudo controllability.\n"
                                        +"  - 'dynamic': [experimental] enforces dynamic controllability."),
                        new UnflaggedOption("anml-file")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setRequired(true)
                                .setGreedy(true)
                                .setHelp("ANML problem files on which to run the planners. If it is set "
                                        + "to a directory, all files ending with .anml will be considered. "
                                        + "If a file of the form xxxxx.yy.pb.anmlis given, the file xxxxx.dom.anml will be loaded first.")

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

        for (String path : configFiles) {
            File f = new File(path);
            if (f.isDirectory()) {
                File[] anmls = f.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File fi) {
                        return fi.getName().endsWith(".anml");
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
        writer.write("iter, planner, runtime, planning-time, anml-file, opened-states, generated-states, sol-depth, flaw-sel, plan-sel\n");

        int repetitions = config.getInt("repetitions");
        for (int i = 0; i < repetitions; i++) {

            for (String anmlFile : anmlFiles) {

                for (String strategy : config.getStringArray("strategies")) {
                    assert strategy.contains("|") : "Strategy is not well formed see help.";
                    String flawStrat = strategy.substring(0, strategy.indexOf('|'));
                    String planStrat = strategy.substring(strategy.indexOf('|') + 1);

                    Queue<APlanner> planners = new LinkedList<APlanner>();
                    String[] plannerIDs = config.getStringArray("plannerID");
                    for (String plannerID : plannerIDs) {
                        switch (plannerID) {
                            case "base":
                                planners.add(new Planner());
                                break;
                            case "htn":
                            case "base+dtg":
                                planners.add(new BaseDTG());
                                break;
                            case "rpg":
                                planners.add(new PGPlanner());
                                break;
                            case "rpg_ext":
                                planners.add(new PGExtPlanner());
                                break;
                            case "taskcond":
                                planners.add(new TaskConditionPlanner());
                                break;
                            case "all":
                                planners.add(new Planner());
                                planners.add(new BaseDTG());
                                planners.add(new PGPlanner());
                                planners.add(new PGExtPlanner());
                                planners.add(new TaskConditionPlanner());
                                break;
                            default:
                                System.err.println("Accepted values for planner are: base, base+dtg, rpg, rpg-ext, taskcond, all");
                        }
                    }
                    APlanner.currentPlanner = planners.peek();
                    int maxtime = config.getInt("maxtime");
                    long planningStart = 0;

                    while(!planners.isEmpty()) {
                        System.gc(); // clean up previous runs to avoid impact on performance measure

                        long start = System.currentTimeMillis();

                        APlanner planner = planners.remove();
                        currentPlanner = planner.shortName();

                        if (!flawStrat.isEmpty()) {
                            planner.flawSelStrategies = flawStrat.split(">");
                        }
                        if (!planStrat.isEmpty()) {
                            planner.planSelStrategies = planStrat.split(">");
                        }
                        planner.controllability = controllability;

                        planner.Init();
                        try {
                            // if the anml has a corresponding domain definition, load it first
                            if (Planning.domainFile(anmlFile) != null) {
                                // add the domain and do not propagate since the problem is still incomplete
                                planner.ForceFact(ANMLFactory.parseAnmlFromFile(domainFile(anmlFile)), false);
                            }

                            boolean isPlannerUsable = planner.ForceFact(ANMLFactory.parseAnmlFromFile(anmlFile), true);
                            if (!isPlannerUsable) {
                                writer.write(
                                        i + ", "
                                        + planner.shortName() + ", "
                                        + "unusable, "
                                        + anmlFile + ", "
                                        + "-, -, -, "
                                        + Utils.print(planner.flawSelStrategies, ">") + ", "
                                        + Utils.print(planner.planSelStrategies, ">") + "\n");
                                writer.flush();
                                continue;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.err.println("Problem with ANML file: " + anmlFile);
                            System.exit(1);
                        }

                        boolean failure = false;
                        try {
                            planningStart = System.currentTimeMillis();
                            failure = !planner.Repair(planningStart + 1000 * maxtime);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.err.println("Planning finished for " + anmlFile + " with failure: "+e);
                        }
                        long end = System.currentTimeMillis();
                        float total = (end - start) / 1000f;
                        String time;
                        String planningTime = Float.toString((end - planningStart) / 1000f);
                        if (failure) {
                            if(planner.planState == APlanner.EPlanState.TIMEOUT)
                                time = "timeout";
                            else if(planner.planState == APlanner.EPlanState.INFEASIBLE)
                                time = "infeasible";
                            else
                                time = "unknown problem";
                        } else {
                            time = Float.toString(total);
                        }

                        writer.write(
                                i + ", "
                                + planner.shortName() + ", "
                                + time + ", "
                                + planningTime + ", "
                                + anmlFile + ", "
                                + planner.OpenedStates + ", "
                                + planner.GeneratedStates + ", "
                                + (failure ? "-" : planner.GetCurrentState().depth) + ", "
                                + Utils.print(planner.flawSelStrategies, ">") + ", "
                                + Utils.print(planner.planSelStrategies, ">")
                                + "\n");
                        writer.flush();

                        if (!failure && !config.getBoolean("quiet")) {
                            State sol = planner.GetCurrentState();

                            System.out.println("=== Temporal databases === \n" + Printer.temporalDatabaseManager(sol));
                            System.out.println("\n=== Actions ===\n"+Printer.actionsInState(sol));

                            Plan plan = new Plan(sol);
                            sol.exportTemporalNetwork("stn.dot");
                            sol.exportTaskNetwork("task-network.dot");
                            System.out.println("Look at stn.dot and task-network.dot for more details.");
                        }
                    }
                }
            }
        }
        if (!config.getString("output").equals("stdout"))
            writer.close();
    }
}
