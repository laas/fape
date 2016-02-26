package fape.core.planning.search.strategies.plans.tsp;


import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planner.APlanner;
import fape.core.planning.search.Handler;
import fape.core.planning.states.State;

import java.util.*;

public class HTSPHandler implements Handler {

    @Override
    public void apply(State st, StateLifeTime time, APlanner planner) {
        Htsp salesman = new Htsp();
        salesman.hc(st);
    }

    @Override
    public void stateBindedToPlanner(State st, APlanner pl) {
        CausalGraph cg = CausalGraph.getCausalGraph(pl);
        List<Set<GStateVariable>> comps = cg.getStronglyConnectedComponents();

        cg.makeAcyclic();
        List<Set<GStateVariable>> comps2 = cg.getStronglyConnectedComponents();
        cg.getTopologicalLevels();
    }

}
