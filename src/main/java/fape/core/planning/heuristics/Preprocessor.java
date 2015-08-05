package fape.core.planning.heuristics;

import fape.core.inference.HLeveledReasoner;
import fape.core.planning.grounding.*;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.planninggraph.GroundDTGs;
import fape.core.planning.states.State;
import planstack.anml.model.Function;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.VarRef;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Preprocessor {

    final APlanner planner;
    final State initialState;

    private FeasibilityReasoner fr;
    private GroundProblem gPb;
    private Set<GAction> allActions;
    private GroundDTGs dtgs;
    HLeveledReasoner<GAction, Fluent> baseCausalReasoner;
    Map<GStateVariable, Map<InstanceRef, Fluent>> fluents;
    int nextFluentID = 0;

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
            gPb = new GroundProblem(initialState.pb, planner);
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
            dtgs = new GroundDTGs(getAllActions(), planner.pb, planner);
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

    public Fluent getFluent(GStateVariable sv, InstanceRef value) {
        if(fluents == null) {
            fluents = new HashMap<>();
        }
        if(!fluents.containsKey(sv)) {
            fluents.put(sv, new HashMap<>());
        }
        if(!fluents.get(sv).containsKey(value)) {
            fluents.get(sv).put(value, new Fluent(sv, value, nextFluentID++));
        }
        return fluents.get(sv).get(value);
    }

    public GStateVariable getStateVariable(Function f, VarRef[] params) {
        InstanceRef[] castParams = new InstanceRef[params.length];
        for (int i = 0; i < params.length; i++)
            castParams[i] = (InstanceRef) params[i];
        return new GStateVariable(f, castParams);
    }
}
