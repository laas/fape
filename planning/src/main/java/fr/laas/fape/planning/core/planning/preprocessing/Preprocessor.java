package fr.laas.fape.planning.core.planning.preprocessing;

import fr.laas.fape.anml.model.Function;
import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.planning.core.inference.HLeveledReasoner;
import fr.laas.fape.planning.core.planning.grounding.*;
import fr.laas.fape.planning.core.planning.heuristics.DefaultIntRepresentation;
import fr.laas.fape.planning.core.planning.heuristics.IntRepresentation;
import fr.laas.fape.planning.core.planning.heuristics.temporal.DeleteFreeActionsFactory;
import fr.laas.fape.planning.core.planning.heuristics.temporal.GStore;
import fr.laas.fape.planning.core.planning.heuristics.temporal.RAct;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.preprocessing.dtg.TemporalDTG;
import fr.laas.fape.planning.core.planning.search.strategies.plans.tsp.DTG;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.util.EffSet;
import fr.laas.fape.planning.util.Pair;
import fr.laas.fape.structures.IRSet;
import fr.laas.fape.anml.model.abs.AbstractAction;

import java.util.*;
import java.util.stream.Collectors;

public class Preprocessor {

    private final Planner planner;
    private final State initialState;

    public int nextGActionID = 0;

    public final GStore store = new GStore();

    private GroundProblem gPb;
    private EffSet<GAction> allActions;
    private IRSet<Fluent> allFluents;
    private IRSet<GStateVariable> allStateVariables;
    private Collection<RAct> relaxedActions;
    private Map<GStateVariable, DTG> dtgs;
    private Map<GStateVariable, TemporalDTG> temporalDTGs = new HashMap<>();
    private GAction[] groundActions = new GAction[1000];
    private HLeveledReasoner<GAction, Fluent> baseCausalReasoner;
    private Map<GStateVariable, Set<GAction>> actionUsingStateVariable;
    private HierarchicalEffects hierarchicalEffects;

    private Boolean isHierarchical = null;

    public int nextTemporalDTGNodeID = 0;

    public Map<Pair<Fluent, Set<GStateVariable>>, Set<AbstractAction>> authorizedSupportersCache = new HashMap<>();

    public Preprocessor(Planner container, State initialState) {
        this.planner = container;
        this.initialState = initialState;
    }

    public TaskDecompositionsReasoner getTaskDecompositionsReasoner() {
        return new TaskDecompositionsReasoner(planner.pb);
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
            allActions = new EffSet<>(groundActionIntRepresentation());
            allActions.addAll(getGroundProblem().allActions());
        }
        return allActions;
    }

    public void restrictPossibleActions(EffSet<GAction> actions) {
        assert allActions != null;
        allActions = actions.clone();
    }

    public HierarchicalEffects getHierarchicalEffects() {
        if(hierarchicalEffects == null) {
            hierarchicalEffects = new HierarchicalEffects(planner.pb);
        }
        return hierarchicalEffects;
    }

    public boolean fluentsInitialized() { return allFluents != null; }

    public void setPossibleFluents(IRSet<Fluent> fluents) {
        assert !fluentsInitialized() : "Possible fluents were already set.";
        allFluents = fluents;
        allStateVariables = new IRSet<>(store.getIntRep(GStateVariable.class));
        for(Fluent f : fluents) {
            allStateVariables.add(f.sv);
        }
    }

    public IRSet<Fluent> getAllFluents() {
        assert fluentsInitialized() : "Possible fluents have not been initialized yet;";
        return allFluents;
    }

    public IRSet<GStateVariable> getAllStateVariables() {
        assert allStateVariables != null : "State variable were nt initialized.";
        return allStateVariables;
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

    public Set<GAction> getActionsInvolving(GStateVariable sv) {
        if(actionUsingStateVariable == null) {
            actionUsingStateVariable = new HashMap<>();
            for(GAction ga : getAllActions()) {
                for(GAction.GLogStatement statement : ga.getStatements()) {
                    actionUsingStateVariable.putIfAbsent(statement.getStateVariable(), new HashSet<>());
                    actionUsingStateVariable.get(statement.getStateVariable()).add(ga);
                }
            }
        }
        if(!actionUsingStateVariable.containsKey(sv))
            return Collections.emptySet();
        else
            return actionUsingStateVariable.get(sv);
    }

    /** Generates a DTG for the given state variables that contains all node but no edges */
    private DTG initDTGForStateVariable(GStateVariable missingSV) {
        return new DTG(missingSV, missingSV.f.valueType().jInstances());
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

    public TemporalDTG getTemporalDTG(GStateVariable sv) {
        if(!temporalDTGs.containsKey(sv)) {
            TemporalDTG dtg = new TemporalDTG(sv, sv.f.valueType().jInstances(), planner);
            for(GAction ga : getActionsInvolving(sv))
                dtg.extendWith(ga);
            dtg.postProcess();
            temporalDTGs.put(sv, dtg);
        }
        return temporalDTGs.get(sv);
    }

    public boolean isHierarchical() {
        if(isHierarchical == null) {
            isHierarchical = false;
            for(GAction ga : getAllActions()) {
                if(ga.subTasks.size() > 0) {
                    isHierarchical = true;
                    break;
                }
                if(ga.abs.isTaskDependent()) {
                    isHierarchical = true;
                    break;
                }
            }
        }
        return isHierarchical;
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
                if (ga.abs.isTaskDependent()) {
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
