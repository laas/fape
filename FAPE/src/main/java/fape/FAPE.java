package fape;

import com.martiansoftware.jsap.*;
import fape.core.acting.Actor;
import fape.core.execution.Executor;
import fape.core.execution.ExecutorPRS;
import fape.core.execution.ExecutorSim;
import fape.core.execution.Listener;
import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planner.PlannerFactory;
import planstack.constraints.stnu.Controllability;

/*
* Author:  Filip Dvořák <filip.dvorak@runbox.com>
*
* Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
*
* Publishing, providing further or using this program is prohibited
* without previous written permission of the author. Publishing or providing
* further the contents of this file is prohibited without previous written
* permission of the author.
*/
/**
* starts up all the components and binds them together
*
* @author FD
*/
public class FAPE {

    public static final String OPRS_MANIP = "PR2";
    public static final String CLIENT_NAME = "FAPE";

    /** True if FAPE should log about the execution details. THis is the default, hence logging
     * should remain short.
     */
    public static boolean execLogging;

    public static boolean localSim;

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                "FAPE",
                "Solves ANML problems",
                new Parameter[] {
                        //new Switch("verbose", 'v', "verbose", "Requests verbose output. Every search node will be displayed."),
                        new Switch("verbose-planner", 'v', "verbose-planner", "Requests verbose output " +
                                "for the planner. Every search node will be displayed."),
                        new Switch("quiet", 'q', "quiet", "FAPE won't detail the execution."),
                        new Switch("debug", 'd', "debug", "Set the planner in debugging mode. " +
                                "Mainly consists in time consuming checks."),
                        new Switch("local-sim", JSAP.NO_SHORTFLAG, "sim", "Use built-in simulator. (doesn't use OpenPRS)"),
                        new FlaggedOption("host")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("host")
                                .setDefault("localhost"),
                        new FlaggedOption("port")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("port")
                                .setDefault("3300"),
                        new UnflaggedOption("anml-file")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setRequired(true)
                                .setGreedy(true)
                                .setHelp("ANML problem files on which to run the planners.")

                }
        );

        JSAPResult config = jsap.parse(args);
        if(jsap.messagePrinted())
            System.exit(0);

        String host = config.getString("host");
        int port = config.getInt("port");

        Planner.debugging = config.getBoolean("debug");
        Planner.logging = config.getBoolean("verbose-planner");
        Plan.makeDispatchable = true;
        execLogging = !config.getBoolean("quiet");

        Actor a = null;
        APlanner p = null;
        Executor e = null;
        Listener l = null;
        String problemFile = "";
        try {
            problemFile = args[0];
            a = new Actor();
            p = PlannerFactory.getPlanner(
                    PlannerFactory.defaultPlanner,
                    PlannerFactory.defaultPlanSelStrategies,
                    PlannerFactory.defaultFlawSelStrategies,
                    Controllability.PSEUDO_CONTROLLABILITY);

            Planner.actionResolvers = true;
            if(config.getBoolean("local-sim"))
                e = new ExecutorSim();
            else
                e = new ExecutorPRS(host, OPRS_MANIP, CLIENT_NAME, port);

            a.bind(e, p);
            e.bind(a);

            if (Planning.domainFile(problemFile) != null) {
                // add the domain and do not propagate since the problem is still incomplete
                a.PushEvent(Executor.ProcessANMLfromFile(Planning.domainFile(problemFile)));
            }
            a.PushEvent(Executor.ProcessANMLfromFile(problemFile));
            a.pbName = problemFile;
            a.run();

        } catch (Exception ex) {
            System.err.println("FAPE setup failed.");
            throw ex;
        } finally {
            // ask executor to abort (to make sure any socket/thread is closed)
            if(e != null) e.abort();
        }
    }
}
