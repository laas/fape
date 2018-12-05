package fr.laas.fape.planning.core.planning.planner;

import java.util.HashMap;
import java.util.Map;

public class Counters {

    public static boolean isActive = GlobalOptions.getBooleanOption("counters-on");
    private static Map<String, Integer> counts = new HashMap<>();

    public static void inc(String name) {
        if(isActive) {
            if (!counts.containsKey(name))
                counts.put(name, 1);
            else
                counts.put(name, counts.get(name) + 1);
        }
    }

    public static void reset() {
        counts.clear();
    }

    public static void echo() {
        if(isActive) {
            System.out.println("-- counters --");
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                System.out.println("  " + e.getKey() + ": " + e.getValue());
            }
        }
    }
}
