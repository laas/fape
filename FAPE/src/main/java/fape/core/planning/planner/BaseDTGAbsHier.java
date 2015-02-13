package fape.core.planning.planner;

import fape.core.planning.preprocessing.AbstractionHierarchy;
import fape.core.planning.states.State;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.parser.ParseResult;
import planstack.constraints.stnu.Controllability;

import java.util.Map;

public class BaseDTGAbsHier extends BaseDTG {

    AbstractionHierarchy hierarchy = null;

    public BaseDTGAbsHier(State initialState, String[] planSelStrategies, String[] flawSelStrategies) {
        super(initialState, planSelStrategies, flawSelStrategies);
        this.hierarchy = new AbstractionHierarchy(this.pb);
    }

    public BaseDTGAbsHier(Controllability controllability, String[] planSelStrategies, String[] flawSelStrategies) {
        super(controllability, planSelStrategies, flawSelStrategies);
    }

    @Override
    public String shortName() {
        return "base+dtg+abs";
    }
}
