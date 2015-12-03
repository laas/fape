package fape.core.planning.search.strategies.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnmotivatedAction;
import fape.core.planning.search.flaws.flaws.UnsupportedTaskCond;
import fape.core.planning.states.State;

public class HierarchicalFirstComp implements FlawComparator {

    public final State st;
    public final APlanner planner;

    public HierarchicalFirstComp(State st, APlanner planner) {
        this.st = st;
        this.planner = planner;
    }

    @Override
    public String shortName() {
        return "hf";
    }

    private int priority(Flaw flaw) {
        if(flaw instanceof UnsupportedTaskCond)
            return 3;
        else if(flaw instanceof UnmotivatedAction)
            return 4;
        else
            return 5;
    }


    @Override
    public int compare(Flaw o1, Flaw o2) {
        return priority(o1) - priority(o2);
    }
}
