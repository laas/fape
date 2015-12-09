package fape.core.planning.preprocessing;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.Handler;
import fape.core.planning.states.State;

public class PreprocessorHandler implements Handler {

    @Override
    public void stateBindedToPlanner(State st, APlanner planner) {
        assert planner.preprocessor == null;
        planner.preprocessor = new Preprocessor(planner, st);
    }
}
