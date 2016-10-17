package fr.laas.fape.planning.core.planning.preprocessing;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.Handler;
import fr.laas.fape.planning.core.planning.states.State;

public class PreprocessorHandler extends Handler {

    @Override
    public void stateBindedToPlanner(State st, Planner planner) {
        assert planner.preprocessor == null;
        planner.preprocessor = new Preprocessor(planner, st);
    }
}
