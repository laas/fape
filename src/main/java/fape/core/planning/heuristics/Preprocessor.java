package fape.core.planning.heuristics;

import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.states.State;

public class Preprocessor {

    final APlanner planner;
    final State initialState;

    private FeasibilityReasoner fr;

    public Preprocessor(APlanner container, State initialState) {
        this.planner = container;
        this.initialState = initialState;
    }

    public FeasibilityReasoner getFeasibilityReasoner() {
        if(fr == null) {
            fr = new FeasibilityReasoner(planner, initialState);
        }

        return fr;
    }
}
