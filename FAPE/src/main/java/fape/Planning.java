package fape;

import com.martiansoftware.jsap.*;
import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.PGPlanner;
import fape.core.planning.printers.Printer;
import fape.core.planning.states.State;
import fape.util.TimeAmount;
import planstack.anml.parser.ANMLFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Planning {

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                "FAPE",
                "Solves ANML problems",
                new Parameter[] {
                        new Switch("verbose", 'v', "verbose", "Requests verbose output. Every search node will be displayed."),
                        new Switch("quiet", 'q', "quiet", "Planner won't print the final plan."),
                        new Switch("debug", 'd', "debug", "Set the planner in debugging mode. " +
                                "Mainly consists intime consuming checks."),
                        new FlaggedOption("plannerID")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag('p')
                                .setLongFlag("planner")
                                .setDefault("base")
                                .setList(true)
                                .setListSeparator(',')
                                .setHelp("Defines which planner implementation to use. Possible values are:\n" +
                                        " - base: Main FAPE planner that supports any domain. Uses a fully lifted representation.\n" +
                                        " - rpg:  Planner that uses relaxed planning graphs for domain analysis.\n" +
                                        "         be aware that it does not handle all anml problems.\n" +
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

        // output format
        System.out.println("iter, planner, runtime, anml-file, opened-states, generated-states");

        int repetitions = config.getInt("repetitions");
        for(int i=0 ; i<repetitions ; i++) {
            for(String anmlFile : anmlFiles) {

                Queue<APlanner> planners = new LinkedList<APlanner>();
                String[] plannerIDs = config.getStringArray("plannerID");
                for(String plannerID : plannerIDs) {
                    switch (plannerID) {
                        case "base":
                            planners.add(new Planner());
                            break;
                        case "rpg":
                            planners.add(new PGPlanner());
                            break;
                        case "all":
                            planners.add(new Planner());
                            planners.add(new PGPlanner());
                            break;
                        default:
                            System.err.println("Accepted values for planner are: base, rpg, all");
                    }
                }

                int maxtime = config.getInt("maxtime");



                while(!planners.isEmpty()) {
                    long start = System.currentTimeMillis();

                    APlanner planner = planners.remove();

                    planner.Init();
                    try {
                        planner.ForceFact(ANMLFactory.parseAnmlFromFile(anmlFile));
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

                    System.out.println(
                            i + ", " +
                            planner.shortName() +", "+
                            time + ", " +
                            anmlFile +", "+
                            planner.OpenedStates +", "+
                            planner.GeneratedStates);

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
}
