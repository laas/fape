package fape.core.planning.planner;

import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.states.State;
import fape.util.TimeAmount;

public class ConstantReasonning extends BaseDTG {
    @Override
    public String shortName() {
        return "const";
    }

}
