package fape;

import fape.core.acting.Actor;
import fape.core.execution.Executor;
import fape.core.execution.Listener;
import fape.core.planning.Planner;
import fape.util.TinyLogger;

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

    public static boolean localTesting = true;
    private static boolean runListener;

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        Actor a = null;
        Planner p = null;
        Executor e = null;
        Listener l = null;

        try {
            a = new Actor();
            p = new Planner();
            Planner.debugging = true;
            Planner.logging = true;
            Planner.actionResolvers = true;
            FAPE.localTesting = true;
            FAPE.runListener = false;
            e = new Executor();
            if (FAPE.runListener) {
                l = new Listener("bobc1", "PR2", "FAPE", "3300");
                l.bind(e);
            }
            a.bind(e, p);
            e.bind(a, l);
        } catch (Exception ex) {
            System.out.println("FAPE setup failed.");
            throw ex;
        }

        if (FAPE.localTesting) {
            //pushing the initial event
            a.PushEvent(e.ProcessANMLfromFile("C:\\ROOT\\PROJECTS\\fape\\FAPE\\problems\\DreamAddition.anml"));
            //a.PushEvent(e.ProcessANMLfromFile("C:\\ROOT\\PROJECTS\\fape\\FAPE\\problems\\IntervalTest.anml"));
            p.Init();
            a.run();
        } else {
            //int sendMessage = l.sendMessage("(FAPE-action -1 -1 -1 (InitializeTime))");
            p.Init();
            a.run();
        }
    }
}
