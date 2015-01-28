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
package fape.scripting;

import fape.core.execution.Executor;
import fape.core.planning.Planner;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planner.PlannerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.PrintStream;


public class ExperimentRunner {

    public static void main(String[] args) {
        String path = "problems/generated"; //default problems
        int maxTime = 1000 * 60;            // default timeout: 1min
        PrintStream out = new PrintStream(System.out); //print out standard output

        try {
            if(args.length > 0)
                path = args[0];
            if(args.length > 1)
                maxTime = Integer.parseInt(args[1]) * 1000;
            if(args.length > 2)
                out = new PrintStream(args[2]);
        } catch (Exception e) {
            System.out.println("Argument format: [directory-with-anml-files [timeout-in-secs [output-file]]]");
        }

        try {
            run(path, maxTime, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void run(String path) throws InterruptedException {
        // using a default timeout of 1 minute
        run(path, 1000 * 10, new PrintStream(System.out));
    }

    public static void run(String path, int maxRuntime, PrintStream out) throws InterruptedException {
        File f = new File(path);
        File[] anmls = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File fi) {
                return fi.getName().contains(".anml");
            }
        });

        Planner.debugging = false;
        Planner.logging = false;
        //Planner.actionResolvers = true;

        out.println("Problem\tTime\t");// + Planner.DomainTableReportFormat() + Planner.PlanTableReportFormat());

        for (File a : anmls) {



            String anml = a.getAbsolutePath();

            APlanner p = PlannerFactory.getDefaultPlanner();
            p.ForceFact(Executor.ProcessANMLfromFile(anml), true);

            long start = System.currentTimeMillis();

            boolean timeOut = false;
            try {
                timeOut = ! p.Repair(start + maxRuntime);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Planning finished for " + anml + " with failure.");
                //throw new FAPEException("Repair failure.");
            }
            long end = System.currentTimeMillis();
            float total = (end - start) / 1000f;

            String report = "";
            if(timeOut)
                report = a.getName() +"\t" + "timeout\t" ;
            else
                report = a.getName() + "\t"+total+"\t";

            //report = report + p.DomainTableReport() + p.PlanTableReport();
            out.println(report);
        }
    }
}
