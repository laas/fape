/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2011 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using of this program is prohibited
 * without previous written permission of author. Publishing or providing further
 * of the contents of this file is prohibited without previous written permission
 * of the author.
 */
package fape.util;

//import fape.core.planning.Planner;

import fape.core.planning.Planner;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;

/**
 *
 * @author Filip Dvořák
 */
public class TinyLogger {

    public static void LogInfo(State st, String toFormat, Object... objects) {
        if(!Planner.logging)
            return;

        String inFormating = toFormat;

        Object[] printables = new Object[objects.length];
        for(int i=0 ; i<objects.length ; i++) {
            printables[i] = Printer.stateDependentPrint(st, objects[i]);
        }

        System.out.println(String.format(toFormat, printables));
    }

    //public static boolean logging = false;
    public static void LogInfo(String st) {
        if (Planner.logging) {
            System.out.println("Logger:" + st);
        }
    }

    public static void LogInfo(Reporter o) {
        if(Planner.logging) {
            System.out.println("Logger: "+o.Report());
        }
    }
}
