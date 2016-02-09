package fape.core.planning.search.strategies.plans.tsp;


import fape.core.planning.planner.APlanner;
import fape.core.planning.search.Handler;
import fape.core.planning.states.State;

public class HTSPHandler implements Handler {

    @Override
    public void apply(State st, StateLifeTime time, APlanner planner) {
        GoalNetwork gn = Htsp.goalNetwork(st);
        System.out.println(gn);
    }
}
