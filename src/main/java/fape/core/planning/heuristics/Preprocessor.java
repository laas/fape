package fape.core.planning.heuristics;

import fape.core.inference.HLeveledReasoner;
import fape.core.planning.grounding.*;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.planninggraph.GroundDTGs;
import fape.core.planning.states.State;

import java.util.Collection;
import java.util.Set;

public class Preprocessor {

    final APlanner planner;
    final State initialState;

    private FeasibilityReasoner fr;
    private GroundProblem gPb;
    private Set<GAction> allActions;
    private GroundDTGs dtgs;
    HLeveledReasoner<GAction, Fluent> baseCausalReasoner;

    Boolean isHierarchical = null;

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

    public GroundProblem getGroundProblem() {
        if(gPb == null) {
            gPb = new GroundProblem(initialState.pb);
        }
        return gPb;
    }

    public Set<GAction> getAllActions() {
        if(allActions == null) {
            allActions = getFeasibilityReasoner().getAllActions(initialState);
        }
        return allActions;
    }

    public GroundDTGs.DTG getDTG(GStateVariable groundStateVariable) {
        if(dtgs == null) {
            dtgs = new GroundDTGs(getAllActions(), planner.pb);
        }
        return dtgs.getDTGOf(groundStateVariable);
    }

    public boolean isHierarchical() {
        if(isHierarchical == null) {
            isHierarchical = false;
            for(GAction ga : getAllActions()) {
                if(ga.subTasks.size() > 0) {
                    isHierarchical = true;
                    break;
                }
                if(ga.abs.mustBeMotivated()) {
                    isHierarchical = true;
                    break;
                }
            }
        }
        return isHierarchical;
    }

    public Set<GAction> getAllPossibleActionFromState(State st) {
        return getFeasibilityReasoner().getAllActions(st);
    }

    public HLeveledReasoner<GAction, Fluent> getLeveledCausalReasoner(State st) {
        if(baseCausalReasoner == null) {
            baseCausalReasoner = new HLeveledReasoner<>();
            for(GAction ga : getAllActions()) {
                baseCausalReasoner.addClause(ga.pre, ga.add, ga);
            }
        }
        return new HLeveledReasoner<>(baseCausalReasoner, getAllPossibleActionFromState(st));
    }
}
