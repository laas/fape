package fape.core.planning.preprocessing;

import fape.core.planning.planner.Planner;
import fape.core.planning.search.Handler;
import fape.core.planning.states.State;

public class PreprocessorHandler extends Handler {

    @Override
    public void stateBindedToPlanner(State st, Planner planner) {
        assert planner.preprocessor == null;
        planner.preprocessor = new Preprocessor(planner, st);
    }
}
