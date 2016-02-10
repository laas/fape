package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import planstack.anml.model.concrete.InstanceRef;

import java.util.*;

public class DTG {
    public final GStateVariable sv;

    public Map<InstanceRef, ArrayList<GAction.GTransition>> outGoingTransitions = new HashMap<>();
    public ArrayList<GAction.GAssignment> unconditionalTransitions = new ArrayList<>();

    public DTG(GStateVariable sv, Collection<InstanceRef> domain) {
        this.sv = sv;
        for(InstanceRef val : domain) {
            outGoingTransitions.put(val, new ArrayList<>());
        }
    }

    public void extendWith(GAction.GTransition trans, GAction container) {
        assert trans.sv == sv;
        outGoingTransitions.get(trans.from).add(trans);
    }

    public void extendWith(GAction.GAssignment ass, GAction container) {
        assert ass.sv == sv;
        unconditionalTransitions.add(ass);
    }
}
