package fape.core.planning.search.strategies.plans.tsp;


import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planner.APlanner;
import fape.core.planning.search.Handler;
import fape.core.planning.states.SearchNode;
import fape.core.planning.states.State;

import java.util.*;

public class HTSPHandler extends Handler {

    @Override
    public void stateBindedToPlanner(State st, APlanner pl) {
        /*
        CausalGraph cg = CausalGraph.getCausalGraph(pl);
        List<Set<GStateVariable>> comps = cg.getStronglyConnectedComponents();

        cg.makeAcyclic();
        List<Set<GStateVariable>> comps2 = cg.getStronglyConnectedComponents();
        cg.getTopologicalLevels();
        */
    }

}
