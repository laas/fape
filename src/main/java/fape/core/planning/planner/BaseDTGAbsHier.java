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

    public BaseDTGAbsHier(State initialState, PlanningOptions options) {
        super(initialState, options);
        this.hierarchy = new AbstractionHierarchy(this.pb);
    }

    public BaseDTGAbsHier(Controllability controllability, PlanningOptions options) {
        super(controllability, options);
    }

    @Override
    public String shortName() {
        return "base+dtg+abs";
    }
}
