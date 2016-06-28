package fape.core.planning.search.strategies.flaws;

import fape.core.planning.planner.Planner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.resolvers.*;
import fape.core.planning.states.State;

public class ExtendPlanFirst implements FlawComparator {

    public final State st;
    public final Planner planner;

    public ExtendPlanFirst(State st, Planner planner) {
        this.st = st;
        this.planner = planner;
    }

    public boolean willExtendPlan(Flaw f) {
        for(Resolver r : f.getResolvers(st, planner)) {
            if(r instanceof BindingSeparation ||
                    r instanceof MotivatedSupport ||
                    r instanceof ExistingTaskSupporter ||
                    r instanceof StateVariableBinding ||
                    r instanceof SupportingTimeline ||
                    r instanceof TemporalConstraint ||
                    r instanceof TemporalSeparation ||
                    r instanceof VarBinding) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int compare(Flaw f1, Flaw f2) { //f1 better => ret < 0
        if(willExtendPlan(f1) && !willExtendPlan(f2))
            return -1;
        else if(!willExtendPlan(f1) && willExtendPlan(f2))
            return 1;
        else return 0;
    }

    @Override
    public String shortName() {
        return "extfirst";
    }
}
