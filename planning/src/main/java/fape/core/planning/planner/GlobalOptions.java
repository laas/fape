package fape.core.planning.planner;

import lombok.Value;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GlobalOptions {

    @Value
    public static class Option {
        public final String key;
        public final String type;
        public final String def;
        public final String description;
    }

    static Map<String,Option> opts = Arrays.asList(
            new Option("heur-weight-existing-statements", "real", "1", ""),
            new Option("heur-weight-pending-statements", "real", "1", ""),
            new Option("heur-weight-threats", "real", "1", ""),
            new Option("heur-all-threats", "boolean", "true", "Use all threats in the cost of threats rather than than the numer of statement involved in threats"),
            new Option("heur-additive-pending-cost", "boolean", "false", "If true, will add the pending cost on all timelines instead of doing a max."),
            new Option("heur-depth-shallow-ratio", "real", "1", "Ratio of the time the planner should pursue an interesting solution in depth first manner vs the time it should push back the frontier by exploring least cost nodes"),
            new Option("heur-h-weight", "real", "1", "Weight of the heuristic function in weighted A*"),
            new Option("heur-weight-unrefined-tasks", "real", "0", ""),
            new Option("heur-weight-unbinded-variables", "real", "0", ""),
            new Option("search-epsilon", "real", "0.3", "Epsilon value of the A-Epsilon algorithm."),
            new Option("use-causal-network", "boolean", "true", ""),
            new Option("use-decomposition-variables", "boolean", "true", ""),
            new Option("check-delay-from-task-to-og", "boolean", "true", ""),
            new Option("reachability-instrumentation", "boolean", "true", "")
    ).stream().collect(Collectors.toMap(Option::getKey, Function.identity()));

    private static Map<String,String> overriddenValues = new HashMap<>();

    public static void setOption(String key, String value) {
        assert opts.containsKey(key) : "Unknown option: "+key;
        overriddenValues.put(key, value);
    }

    private static String getValue(String key) {
        assert opts.containsKey(key) : "Unknown option: "+key;
        if(overriddenValues.containsKey(key))
            return overriddenValues.get(key);
        else
            return opts.get(key).getDef();
    }

    public static String getStringOption(String key) {
        assert opts.containsKey(key) : "Unknown option: "+key;
        assert opts.get(key).getType().equals("string");
        return getValue(key);
    }

    public static float getFloatOption(String key) {
        assert opts.containsKey(key) : "Unknown option: "+key;
        assert opts.get(key).getType().equals("real");
        return Float.parseFloat(getValue(key));
    }

    public static int getIntOption(String key) {
        assert opts.containsKey(key) : "Unknown option: "+key;
        assert opts.get(key).getType().equals("int");
        return Integer.parseInt(getValue(key));
    }

    public static boolean getBooleanOption(String key) {
        assert opts.containsKey(key) : "Unknown option: "+key;
        assert opts.get(key).getType().equals("boolean");
        return Boolean.parseBoolean(getValue(key));
    }
}
