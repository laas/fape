package fape;

import com.martiansoftware.jsap.*;
import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planner.BaseDTG;
import fape.core.planning.planner.BaseDTGAbsHier;
import fape.core.planning.planner.PGExtPlanner;
import fape.core.planning.planninggraph.PGPlanner;
import fape.core.planning.printers.Printer;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import fape.util.TimeAmount;
import fape.util.Utils;
import planstack.anml.parser.ANMLFactory;

import java.io.*;
import java.util.*;

public class Planning {

    /**
     * Tries to infer which file contains the domain definition of this problem.
     * If the problem takes a form "domainName.xxxx.pb.anml", then the corresponding domain file
     * would be "domainName.dom.anml"
     * @param problemFile
     * @return
     */
    public static String domainFile(String problemFile) {
        File f = new File(problemFile);
        String name = f.getName();
        if(name.endsWith(".pb.anml")) {
            String[] nameParts = name.split("\\.");

            if(nameParts.length != 4) {
                throw new FAPEException("File name "+name+ " is not correctly formatted. It should be in the form "+
                " domainName.xxx.pb.anml and have an associated domainName.dom.anml file.");
            }

            File domain = new File(f.getParentFile(), nameParts[0]+".dom.anml");
            if(!domain.exists()) {
                throw new FAPEException("File "+domain+" does not exists (name derived from "+problemFile+")");
            }

            return domain.getPath();
        } else {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                "FAPE",
                "Solves ANML problems",
                new Parameter[] {
                        new Switch("verbose", 'v', "verbose", "Requests verbose output. Every search node will be displayed."),
                        new Switch("quiet", 'q', "quiet", "Planner won't print the final plan."),
                        new Switch("debug", 'd', "debug", "Set the planner in debugging mode. " +
                                "Mainly consists in time consuming checks."),
                        new FlaggedOption("plannerID")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('p')
                                .setLongFlag("planner")
                                .setDefault("base")
                                .setList(true)
                                .setListSeparator(',')
                                .setHelp("Defines which planner implementation to use. Possible values are:\n" +
                                        " - base: Main FAPE planner that supports any domain. Uses a fully lifted representation.\n" +
                                        " - base+dtg: Same as base but uses DTG to select action resolvers.\n" +
                                        " - base+dtg+abs: Same as base+dtg but uses abstraction hierarchies to order flaws.\n" +
                                        " - rpg:  Planner that uses relaxed planning graphs for domain analysis.\n" +
                                        "         be aware that it does not handle all anml problems.\n" +
                                        " - rpg_ext: Extension of rpg that add constraint on every causal link whose supporter \n" +
                                        "            is provided by an action. It cheks (using RPG) for every ground action\n" +
                                        "            supporting the consmer and enforce a n-ary constraint on the action's\n" +
                                        "            parameters to make sure they fit at least one of the ground action.\n" +
                                        " - all:  will run every possible planner.\n"),

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
                                .setHelp("This is used to define search strategies. A search strategy consists of " +
                                        "one or more flaw selection strategy and one or more state selection strategy. " +
                                        "Ex: the argument 'lcf>lfr|dfs' would give for flaws: lcf (LeastCommitingFirst), using " +
                                        "lfr (LeastFlawRatio) to handle ties. States would be selected using dfs (DepthFirstSearch). " +
                                        "For more information on search strategies, look at the fape.core.planning.search.strategies " +
                                        "package. If several strategies are given (separated by commas), they will all be attempted."),
                        new FlaggedOption("output")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('o')
                                .setLongFlag("output")
                                .setDefault("stdout")
                                .setHelp("File to which the CSV formatted output will be written"),
                        new UnflaggedOption("anml-file")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setDefault("problems/handover.anml")
                                .setRequired(false)
                                .setGreedy(true)
                                .setHelp("ANML problem files on which to run the planners. If it is set " +
                                        "to a directory, all files ending with .anml will be considered.")

                }
        );

        JSAPResult config = jsap.parse(args);
        if(jsap.messagePrinted())
            System.exit(0);

        Writer writer;
        if(config.getString("output").equals("stdout"))
            writer = new OutputStreamWriter(System.out);
        else
            writer = new FileWriter(config.getString("output"));

        APlanner.logging = config.getBoolean("verbose");
        APlanner.debugging = config.getBoolean("debug");

        String[] configFiles = config.getStringArray("anml-file");
        List<String> anmlFiles = new LinkedList<>();

        for(String path : configFiles) {
            File f = new File(path);
            if(f.isDirectory()) {
                File[] anmls = f.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File fi) {
                        return fi.getName().endsWith(".anml");
                    }
                });
                for(File anmlFile : anmls)
                    anmlFiles.add(anmlFile.getPath());
            } else {
                anmlFiles.add(path);
            }
        }

        Collections.sort(anmlFiles);

        // output format
        writer.write("iter, planner, runtime, anml-file, opened-states, generated-states, sol-depth, flaw-sel, plan-sel\n");

        int repetitions = config.getInt("repetitions");
        for(int i=0 ; i<repetitions ; i++) {

            for(String anmlFile : anmlFiles) {

                for(String strategy : config.getStringArray("strategies")) {
                    assert strategy.contains("|") : "Strategy is not well formed see help.";
                    String flawStrat = strategy.substring(0, strategy.indexOf('|'));
                    String planStrat = strategy.substring(strategy.indexOf('|')+1);

                    Queue<APlanner> planners = new LinkedList<APlanner>();
                    String[] plannerIDs = config.getStringArray("plannerID");
                    for(String plannerID : plannerIDs) {
                        switch (plannerID) {
                            case "base":
                                planners.add(new Planner());
                                break;
                            case "base+dtg":
                                planners.add(new BaseDTG());
                                break;
                            case "rpg":
                                planners.add(new PGPlanner());
                                break;
                            case "rpg_ext":
                                planners.add(new PGExtPlanner());
                                break;
                            case "all":
                                planners.add(new Planner());
                                planners.add(new BaseDTG());
                                planners.add(new PGPlanner());
                                planners.add(new PGExtPlanner());
                                break;
                            default:
                                System.err.println("Accepted values for planner are: base, base+dtg, rpg, rpg-ext, all");
                        }
                    }

                    int maxtime = config.getInt("maxtime");



                    while(!planners.isEmpty()) {
                        long start = System.currentTimeMillis();

                        APlanner planner = planners.remove();

                        if(!flawStrat.isEmpty())
                            planner.flawSelStrategies = flawStrat.split(">");
                        if(!planStrat.isEmpty())
                            planner.planSelStrategies = planStrat.split(">");

                        planner.Init();
                        try {
                            // if the anml has a corresponding domain definition, load it first
                            if(Planning.domainFile(anmlFile) != null) {
                                planner.ForceFact(ANMLFactory.parseAnmlFromFile(domainFile(anmlFile)));
                            }
                            boolean isPlannerUsable = planner.ForceFact(ANMLFactory.parseAnmlFromFile(anmlFile));
                            if(!isPlannerUsable) {
                                writer.write(
                                        i + ", " +
                                        planner.shortName() +", "+
                                        "unusable, " +
                                        anmlFile +", "+
                                        "-, -, -, "+
                                        Utils.print(planner.flawSelStrategies,">")+", "+
                                        Utils.print(planner.planSelStrategies, ">") +"\n");
                                writer.flush();
                                continue;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.err.println("Problem with ANML file: "+anmlFile);
                            System.exit(1);
                        }

                        boolean timeOut = false;
                        try {
                            timeOut = !planner.Repair(new TimeAmount(1000 * maxtime));
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.err.println("Planning finished for " + anmlFile + " with failure.");
                            //throw new FAPEException("Repair failure.");
                        }
                        long end = System.currentTimeMillis();
                        float total = (end - start) / 1000f;
                        String time;
                        if(timeOut)
                            time = "timeout";
                        else
                            time = Float.toString(total);

                        writer.write(
                                i + ", " +
                                planner.shortName() +", "+
                                time + ", " +
                                anmlFile +", "+
                                planner.OpenedStates +", "+
                                planner.GeneratedStates +", "+
                                (timeOut ? "-" : planner.GetCurrentState().depth) +", "+
                                Utils.print(planner.flawSelStrategies,">")+", "+
                                Utils.print(planner.planSelStrategies, ">")+
                                "\n");
                        writer.flush();

                        if(!timeOut && !config.getBoolean("quiet")) {
                            State sol = planner.GetCurrentState();

                            System.out.println("=== Temporal databases === \n"+ Printer.temporalDatabaseManager(sol, sol.tdb));

                            Plan plan = new Plan(sol);
                            plan.exportToDot("plan.dot");
                            System.out.println("Look at plan.dot for a complete plan.");
                        }
                    }
                }
            }
        }
        writer.close();
    }
}
