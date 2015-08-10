package fape.core.planning.heuristics;

import fape.core.inference.HLeveledReasoner;
import fape.core.planning.grounding.*;
import fape.core.planning.heuristics.relaxed.DTGImpl;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.planninggraph.GroundDTGs;
import fape.core.planning.states.State;
import org.apache.batik.gvt.text.GVTAttributedCharacterIterator;
import planstack.anml.model.Function;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.VarRef;

import java.util.*;

public class Preprocessor {

    final APlanner planner;
    final State initialState;

    private FeasibilityReasoner fr;
    private GroundProblem gPb;
    private Set<GAction> allActions;
    private GroundDTGs dtgs;
    private Fluent[] fluents = new Fluent[1000];
    private GAction[] groundActions = new GAction[1000];
    HLeveledReasoner<GAction, Fluent> baseCausalReasoner;
    Map<GStateVariable, Map<InstanceRef, Fluent>> fluentsMap;
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
            for(GAction ga : allActions) {
                if(ga.id >= groundActions.length)
                    groundActions = Arrays.copyOf(groundActions, Math.max(ga.id+1, groundActions.length*2));
                assert groundActions[ga.id] == null;
                groundActions[ga.id] = ga;
            }
        }
        return allActions;
    }

    public GAction getGroundAction(int groundActionID) {
        if(groundActionID == -1)
            return null;
        assert groundActionID < groundActions.length && groundActions[groundActionID] != null : "No recorded ground action with ID: "+groundActionID;
        return groundActions[groundActionID];
    }

    public IntRepresentation<GAction> groundActionIntRepresentation() {
        return new IntRepresentation<GAction>() {
            @Override public final int asInt(GAction gAction) { return gAction.id; }
            @Override public final GAction fromInt(int id) { return getGroundAction(id); }
            @Override public boolean hasRepresentation(GAction gAction) { assert groundActions[gAction.id] == gAction; return true; }
        };
    }

    public DTGImpl getDTG(GStateVariable groundStateVariable) {
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
        if(fluentsMap == null) {
            fluentsMap = new HashMap<>();
        }
        if(!fluentsMap.containsKey(sv)) {
            fluentsMap.put(sv, new HashMap<>());
        }
        if(!fluentsMap.get(sv).containsKey(value)) {
            final Fluent f = new Fluent(sv, value, nextFluentID++);
            fluentsMap.get(sv).put(value, f);
            if(f.ID >= fluents.length)
                fluents = Arrays.copyOf(fluents, fluents.length*2);
            assert fluents[f.ID] == null : "Error recording to fluents with same ID.";
            fluents[f.ID] = f;
        }
        return fluentsMap.get(sv).get(value);
    }

    public Fluent getFluent(int fluentID) {
        assert fluentID < fluents.length && fluents[fluentID] != null : "No fluent with ID "+fluentID+" recorded.";
        return fluents[fluentID];
    }

    public IntRepresentation<Fluent> fluentIntRepresentation() {
        return new IntRepresentation<Fluent>() {
            @Override public final int asInt(Fluent fluent) { return fluent.ID; }
            @Override public final Fluent fromInt(int id) { return getFluent(id); }
            @Override public boolean hasRepresentation(Fluent fluent) { assert fluents[fluent.ID] == fluent; return true; }
        };
    }

    public GStateVariable getStateVariable(Function f, VarRef[] params) {
        InstanceRef[] castParams = new InstanceRef[params.length];
        for (int i = 0; i < params.length; i++)
            castParams[i] = (InstanceRef) params[i];
        return new GStateVariable(f, castParams);
    }
}
