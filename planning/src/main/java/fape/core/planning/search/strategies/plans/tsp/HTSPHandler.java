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
        GoalNetwork gn = Htsp.goalNetwork(st);
        System.out.println(gn);

        planner.preprocessor.getOldDTG(gn.getActiveGoals().iterator().next().goals.iterator().next().sv);
        Htsp salesman = new Htsp();
        salesman.hc(st);
    }

}
