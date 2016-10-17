package fr.laas.fape.planning.util;

import fr.laas.fape.planning.core.planning.states.Printer;
import fr.laas.fape.planning.core.planning.states.State;

public class TinyLogger {
    public static boolean logging = false;

    public static void LogInfo(State st, String toFormat, Object... objects) {
        if(!logging)
            return;

        Object[] printables = new Object[objects.length];
        for(int i=0 ; i<objects.length ; i++) {
            printables[i] = Printer.stateDependentPrint(st, objects[i]);
        }

        System.out.println(String.format(toFormat, printables));
    }

    //public static boolean logging = false;
    public static void LogInfo(String st) {
        if (logging) {
            System.out.println(st);
        }
    }

    public static void LogInfo(Reporter o) {
        if(logging) {
            System.out.println("Logger: "+o.report());
        }
    }
}
