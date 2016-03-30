package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.util.Pair;
import planstack.anml.model.concrete.InstanceRef;

import java.util.*;
import java.util.stream.Collectors;

public class DTG {
    public final GStateVariable sv;

    public Map<InstanceRef, ArrayList<Pair<GAction.GTransition,GAction>>> outGoingTransitions = new HashMap<>();
    public ArrayList<Pair<GAction.GAssignment,GAction>> unconditionalTransitions = new ArrayList<>();

    public DTG(GStateVariable sv, Collection<InstanceRef> domain) {
        this.sv = sv;
        for(InstanceRef val : domain) {
            outGoingTransitions.put(val, new ArrayList<>());
        }
    }

    public void extendWith(GAction.GTransition trans, GAction container) {
        assert trans.sv == sv;
        outGoingTransitions.get(trans.from).add(Pair.pair(trans, container));
    }

    public void extendWith(GAction.GAssignment ass, GAction container) {
        assert ass.sv == sv;
        unconditionalTransitions.add(Pair.pair(ass, container));
    }

    public Iterable<GAction.GTransition> outTransitions(InstanceRef val, Set<GAction> possibleActions) {
        return outGoingTransitions.get(val).stream()
                .filter(p -> possibleActions == null || possibleActions.contains(p.value2))
                .map(p -> p.value1)
                .collect(Collectors.toList());
    }

    public Iterable<GAction.GAssignment> getAssignments(Set<GAction> possibleActions) {
        return unconditionalTransitions.stream()
                .filter(p -> possibleActions == null || possibleActions.contains(p.value2))
                .map(p -> p.value1)
                .collect(Collectors.toList());
    }


}
