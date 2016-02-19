package fape.core.planning.search.strategies.plans.tsp;


import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planner.APlanner;
import fape.core.planning.search.Handler;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.InstanceRef;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HTSPHandler implements Handler {

    @Override
    public void apply(State st, StateLifeTime time, APlanner planner) {
        Htsp salesman = new Htsp();
        salesman.hc(st);
    }

    @Override
    public void stateBindedToPlanner(State st, APlanner pl) {
        CausalGraph cg = CausalGraph.getCausalGraph(pl);
        cg.getStronglyConnectedComponents();
        System.out.println(cg);
    }

}
