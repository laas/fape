package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import planstack.anml.model.concrete.Action;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ActionUsageTracker {

    public final HashMap<GAction, HashMap<GStateVariable, Integer>> occurrencesPerStateVariable = new HashMap<>();
    public final HashMap<GAction, Integer> maxOccurrences = new HashMap<>();
    public final HashMap<Action, Set<GAction>> instantiations = new HashMap<>();
    public final HashMap<GAction, Integer> occurrencesAsInstantiations = new HashMap<>();

    public final HashSet<GAction> independentlyUsed = new HashSet<>();


    public void addActionOccurrence(GAction ga, GStateVariable sv) {
        if(!maxOccurrences.containsKey(ga)) {
            maxOccurrences.put(ga, 0);
            assert !occurrencesPerStateVariable.containsKey(ga);
            occurrencesPerStateVariable.put(ga, new HashMap<>());
        }
        int newCount = 1 + occurrencesPerStateVariable.get(ga).getOrDefault(sv, 0);
        assert newCount-1 <= maxOccurrences.get(ga) : "A number of occurrences is greater than the precomputed maximum.";

        occurrencesPerStateVariable.get(ga).put(sv, newCount);
        if(newCount > maxOccurrences.get(ga))
            maxOccurrences.put(ga, newCount);
    }

    public int occurencesForStateVariable(GAction ga, GStateVariable sv) {
        if(!occurrencesPerStateVariable.containsKey(ga))
            return 0;
        else
            return occurrencesPerStateVariable.get(ga).getOrDefault(sv, 0);
    }

    public int maxAdditionalOccurrences(GAction ga) {
        return maxOccurrences.getOrDefault(ga, 0);
    }

    public int totalOccurrences(GAction ga) {
        int add = maxOccurrences.getOrDefault(ga, 0);
        int inst = occurrencesAsInstantiations.getOrDefault(ga, 0);
        int inde = independentlyUsed.contains(ga) ? 1 : 0;
        return add + inst + inde;
    }
    public void addActionInstantiation(Action lifted, GAction ga) {
        instantiations.putIfAbsent(lifted, new HashSet<>());

        if(!instantiations.get(lifted).contains(ga)) {
            // this instantiation did not exists, add it
            int prev = occurrencesAsInstantiations.containsKey(ga) ? occurrencesAsInstantiations.get(ga) : 0;
            occurrencesAsInstantiations.put(ga, prev + 1);
            instantiations.get(lifted).add(ga);
        }
    }

    public void addIndependentUsage(GAction ga) {
        assert !independentlyUsed.contains(ga);
        independentlyUsed.add(ga);
    }

    public Collection<GAction> getAllUsedAction() {
        Set<GAction> acts = new HashSet<>();
        for(GAction ga : maxOccurrences.keySet())
            acts.add(ga);
        for(GAction ga : occurrencesAsInstantiations.keySet())
            acts.add(ga);
        acts.addAll(independentlyUsed);
        for(GAction ga : acts)
            assert totalOccurrences(ga) > 0;
        return acts;
    }
}
