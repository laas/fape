package fape.core.planning.preprocessing;

import fape.core.inference.HLeveledReasoner;
import fape.core.planning.grounding.*;
import fape.core.planning.heuristics.DefaultIntRepresentation;
import fape.core.planning.heuristics.IntRepresentation;
import fape.core.planning.heuristics.relaxed.DTGImpl;
import fape.core.planning.heuristics.temporal.DeleteFreeActionsFactory;
import fape.core.planning.heuristics.temporal.GStore;
import fape.core.planning.heuristics.temporal.RAct;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.planninggraph.GroundDTGs;
import fape.core.planning.search.strategies.plans.tsp.DTG;
import fape.core.planning.states.State;
import fape.util.EffSet;
import planstack.anml.model.Function;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.VarRef;

import java.util.*;
import java.util.stream.Collectors;

public class Preprocessor {

    final APlanner planner;
    final State initialState;

    public final GStore store = new GStore();

    private FeasibilityReasoner fr;
    private GroundProblem gPb;
    private EffSet<GAction> allActions;
    private Collection<RAct> relaxedActions;
    private GroundDTGs oldDTGs;
    private Map<GStateVariable, DTG> dtgs;
    private GAction[] groundActions = new GAction[1000];
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
            gPb = new GroundProblem(initialState.pb, planner);
            for(GAction ga : gPb.allActions()) {
                if(ga.id >= groundActions.length)
                    groundActions = Arrays.copyOf(groundActions, Math.max(ga.id+1, groundActions.length*2));
                assert groundActions[ga.id] == null;
                groundActions[ga.id] = ga;
            }
        }
        return gPb;
    }

    public EffSet<GAction> getAllActions() {
        if(allActions == null) {
            allActions = new EffSet<GAction>(groundActionIntRepresentation());
            allActions.addAll(getGroundProblem().allActions());
        }
        return allActions;
    }

    public GAction getGroundAction(int groundActionID) {
        if(groundActionID == -1)
            return null;
        assert groundActionID < groundActions.length && groundActions[groundActionID] != null : "No recorded ground action with ID: "+groundActionID;
        return groundActions[groundActionID];
    }

    public Collection<RAct> getRelaxedActions() {
        if(relaxedActions == null) {
            relaxedActions = new ArrayList<>();
            for(AbstractAction aa : planner.pb.abstractActions()) {
                DeleteFreeActionsFactory f = new DeleteFreeActionsFactory();
                List<GAction> gacts = this.getAllActions().stream().filter(ga -> ga.abs == aa).collect(Collectors.toList());
                relaxedActions.addAll(f.getDeleteFrees(aa, gacts, planner));
            }
        }
        return relaxedActions;
    }

    public IntRepresentation<GAction> groundActionIntRepresentation() {
        return new IntRepresentation<GAction>() {
            @Override public final int asInt(GAction gAction) { return gAction.id; }
            @Override public final GAction fromInt(int id) { return getGroundAction(id); }
            @Override public boolean hasRepresentation(GAction gAction) { assert groundActions[gAction.id] == gAction; return true; }
        };
    }



    public DTGImpl getOldDTG(GStateVariable groundStateVariable) {
        if(oldDTGs == null) {
            oldDTGs = new GroundDTGs(getAllActions(), planner.pb, planner);
        }
        return oldDTGs.getDTGOf(groundStateVariable);
    }

    /** Generates a DTG for the given state variables that contains all node bu no edges */
    private DTG initDTGForStateVariable(GStateVariable missingSV) {
        Collection<InstanceRef> dom = planner.pb.instances().instancesOfType(missingSV.f.valueType()).stream()
                .map(str -> planner.pb.instance(str))
                .collect(Collectors.toList());
        return new DTG(missingSV, dom);
    }

    public DTG getDTG(GStateVariable gStateVariable) {
        if (dtgs == null) {
            dtgs = new HashMap<>();

            // Create all DTG and populate their edges with the
            // transitions and assignment in the actions.
            for(GAction ga : getAllActions()) {
                for(GAction.GLogStatement s : ga.gStatements.stream().map(p -> p.value2).collect(Collectors.toList())) {
                    if(!dtgs.containsKey(s.sv)) {
                        dtgs.put(s.sv, initDTGForStateVariable(s.sv));
                    }
                    if(s instanceof GAction.GAssignment)
                        dtgs.get(s.sv).extendWith((GAction.GAssignment) s, ga);
                    else if(s instanceof GAction.GTransition)
                        dtgs.get(s.sv).extendWith((GAction.GTransition) s, ga);
                }
            }
        }

        if(!dtgs.containsKey(gStateVariable))
            // State variable probably does not appear in any action, just give it an empty DTG
            dtgs.put(gStateVariable, initDTGForStateVariable(gStateVariable));

        return dtgs.get(gStateVariable);
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

    @Deprecated
    public EffSet<GAction> getAllPossibleActionFromState(State st) {
        return getFeasibilityReasoner().getAllActions(st);
    }

    public HLeveledReasoner<GAction, Fluent> getRestrictedCausalReasoner(EffSet<GAction> allowedActions) {
        if(baseCausalReasoner == null) {
            baseCausalReasoner = new HLeveledReasoner<>(this.groundActionIntRepresentation(), this.fluentIntRepresentation());
            for(GAction ga : getAllActions()) {
                baseCausalReasoner.addClause(ga.pre, ga.add, ga);
            }
        }
        return baseCausalReasoner.cloneWithRestriction(allowedActions);
    }

    public HLeveledReasoner<GAction, Fluent> getLeveledCausalReasoner(State st) {
        return getRestrictedCausalReasoner(getAllPossibleActionFromState(st));
    }

    HLeveledReasoner<GAction, GTask> baseDecomposabilityReasoner = null;
    /** initial "facts" are actions with no subtasks */
    public HLeveledReasoner<GAction, GTask> getRestrictedDecomposabilityReasoner(EffSet<GAction> allowedActions) {
        if(baseDecomposabilityReasoner == null) {
            baseDecomposabilityReasoner = new HLeveledReasoner<>(planner.preprocessor.groundActionIntRepresentation(), new DefaultIntRepresentation<>());
            for (GAction ga : this.getAllActions()) {
                GTask[] effect = new GTask[1];
                effect[0] = ga.task;
                baseDecomposabilityReasoner.addClause(ga.subTasks.toArray(new GTask[ga.subTasks.size()]), effect, ga);
            }
        }
        return baseDecomposabilityReasoner.cloneWithRestriction(allowedActions);
    }

    HLeveledReasoner<GAction, GTask> baseDerivabilityReasoner = null;

    /** initial facts opened tasks and initial clauses are non-motivated actions*/
    public HLeveledReasoner<GAction, GTask> getRestrictedDerivabilityReasoner(EffSet<GAction> allowedActions) {
        if(baseDerivabilityReasoner == null) {
            baseDerivabilityReasoner = new HLeveledReasoner<>(planner.preprocessor.groundActionIntRepresentation(), new DefaultIntRepresentation<>());
            for (GAction ga : getAllActions()) {
                if (ga.abs.motivated()) {
                    GTask[] condition = new GTask[1];
                    condition[0] = ga.task;
                    baseDerivabilityReasoner.addClause(condition, ga.subTasks.toArray(new GTask[ga.subTasks.size()]), ga);
                } else {
                    baseDerivabilityReasoner.addClause(new GTask[0], ga.subTasks.toArray(new GTask[ga.subTasks.size()]), ga);
                }
            }
        }
        return baseDerivabilityReasoner.cloneWithRestriction(allowedActions);
    }

    public Fluent getFluent(GStateVariable sv, InstanceRef value) {
        return store.getFluent(sv, value);
    }

    public Fluent getFluent(int fluentID) {
        return store.getFluentByID(fluentID);
    }

    public int getApproximateNumFluents() { return store.getHigherID(Fluent.class); }

    public IntRepresentation<Fluent> fluentIntRepresentation() {
        return new IntRepresentation<Fluent>() {
            @Override public final int asInt(Fluent fluent) { return fluent.getID(); }
            @Override public final Fluent fromInt(int id) { return getFluent(id); }
            @Override public boolean hasRepresentation(Fluent fluent) { return true; }
        };
    }

    public GStateVariable getStateVariable(Function f, VarRef[] params) {
        InstanceRef[] castParams = new InstanceRef[params.length];
        for (int i = 0; i < params.length; i++)
            castParams[i] = (InstanceRef) params[i];
        return store.getGStateVariable(f, Arrays.asList(castParams));
    }
}
