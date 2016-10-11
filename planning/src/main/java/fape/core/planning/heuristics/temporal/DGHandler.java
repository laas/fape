package fape.core.planning.heuristics.temporal;


import fape.Planning;
import fape.core.planning.grounding.*;
import fape.core.planning.planner.GlobalOptions;
import fape.core.planning.planner.Planner;
import fape.core.planning.preprocessing.Preprocessor;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.util.EffSet;
import fape.util.TinyLogger;
import fr.laas.fape.structures.IRSet;
import fr.laas.fape.structures.IntRep;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;
import planstack.anml.model.concrete.VarRef;
import planstack.constraints.bindings.Domain;

import java.util.*;
import java.util.stream.Collectors;

public class DGHandler extends fape.core.planning.search.Handler {
    private final boolean USE_DECOMPOSITION_VARIABLES = GlobalOptions.getBooleanOption("use-decomposition-variables");

    @Override
    public void stateBindedToPlanner(State st, Planner pl) {
        assert !st.hasExtension(DepGraphCore.StateExt.class);

        // init the core of the dependency graph
        DepGraphCore core = new DepGraphCore(pl.preprocessor.getRelaxedActions(), false, pl.preprocessor.store);
        st.addExtension(new DepGraphCore.StateExt(core));

        // Record ground action ids as possible values for variables in the CSP
        for (GAction ga : pl.preprocessor.getAllActions()) {
            st.csp.bindings().addPossibleValue(ga.id);
        }

        // record all n ary constraints (action instantiations and task supporters)
        Set<String> recordedTask = new HashSet<>();
        for (AbstractAction aa : pl.pb.abstractActions()) {
            st.csp.bindings().recordEmptyNAryConstraint(aa.name(), true, aa.allVars().length + 1);
            st.csp.bindings().addPossibleValue(aa.name());
            if (!recordedTask.contains(aa.taskName())) {
                st.csp.bindings().recordEmptyNAryConstraint(aa.taskName(), true, aa.args().size() + 2);
                recordedTask.add(aa.taskName());
            }
        }

        for (GAction ga : pl.preprocessor.getAllActions()) {
            // values for all variables of this action
            List<String> values = new LinkedList<>();
            for (LVarRef var : ga.variables)
                values.add(ga.valueOf(var).instance());
            // add possible tuple to instantiation constraints
            st.csp.bindings().addAllowedTupleToNAryConstraint(ga.abs.name(), values, ga.id);

            // values for arguments of this action
            List<String> argValues = new LinkedList<>();
            for (LVarRef var : ga.abs.args())
                argValues.add(ga.valueOf(var).instance());
            argValues.add(ga.abs.name());
            // add possible tuple to supporter constraints
            st.csp.bindings().addAllowedTupleToNAryConstraint(ga.abs.taskName(), argValues, ga.id);
        }

        // notify ourselves of the presence of any actions and tasks in the plan
        for (Action a : st.getAllActions())
            actionInserted(a, st, pl);
        for (Task t : st.getOpenTasks())
            taskInserted(t, st, pl);

        // trigger propagation of constraint networks
        st.checkConsistency();
        propagateNetwork(st, pl);
        st.checkConsistency();
    }

    @Override
    protected void apply(State st, StateLifeTime time, Planner planner) {
        if (time == StateLifeTime.SELECTION) {
            propagateNetwork(st, planner);
        }
    }

    @Override
    public void actionInserted(Action act, State st, Planner pl) {
        if(st.csp.bindings().isRecorded(act.instantiationVar()))
            return;
        assert !st.csp.bindings().isRecorded(act.instantiationVar()) : "The action already has a variable for its ground versions.";

        // all ground versions of this actions (represented by their ID)
        LVarRef[] vars = act.abs().allVars();
        List<VarRef> values = new ArrayList<>();
        for(LVarRef v : vars) {
            values.add(act.context().getDefinition(v));
        }

        // Variable representing the ground versions of this action
        st.csp.bindings().addIntVariable(act.instantiationVar(), new Domain(st.addableActions.toBitSet()));
        values.add(act.instantiationVar());
        st.addValuesSetConstraint(values, act.abs().name());

        assert st.csp.bindings().isRecorded(act.instantiationVar());
    }

    @Override
    public void taskInserted(Task task, State st, Planner planner) {
        if(st.csp.bindings().isRecorded(task.methodSupportersVar()))
            return;

        assert !st.csp.bindings().isRecorded(task.methodSupportersVar());
        assert !st.csp.bindings().isRecorded(task.groundSupportersVar());
        Collection<String> supportingMethods = planner.pb.getSupportersForTask(task.name()).stream()
                .map(aa -> aa.name()).collect(Collectors.toList());

        st.csp.bindings().addVariable(task.methodSupportersVar(), supportingMethods);
        st.csp.bindings().addIntVariable(task.groundSupportersVar());

        List<VarRef> variables = new LinkedList<>();
        variables.addAll(task.args());
        variables.add(task.methodSupportersVar());
        variables.add(task.groundSupportersVar());
        if(USE_DECOMPOSITION_VARIABLES)
            st.addValuesSetConstraint(variables, task.name());
    }

    @Override
    public void supportLinkAdded(Action act, Task task, State st) {
        st.addUnificationConstraint(task.groundSupportersVar(), act.instantiationVar());
    }

    private void propagateNetwork(State st, Planner pl) {
        final IntRep<GAction> gactsRep = pl.preprocessor.store.getIntRep(GAction.class);

        DepGraphCore.StateExt ext = st.getExtension(DepGraphCore.StateExt.class);

        final Preprocessor pp = st.pl.preprocessor;
        final GroundProblem gpb = pp.getGroundProblem();

        List<TempFluent> tempFluents = new ArrayList<>();
        // gather all fluents appearing in the partial plan
        // those fluents can not be used to support changes
        gpb.tempsFluents(st).stream()
                .flatMap(tfs -> tfs.fluents.stream().map(f -> new TempFluent(
                        st.getEarliestStartTime(tfs.timepoints.iterator().next()),
                        TempFluent.DGFluent.getBasicFluent(f, pp.store))))
                .forEach(tempFluents::add);

        // gather all fluents achieved and not involved in any causal link
        // those cn be used to support transitions
        gpb.tempsFluentsThatCanSupportATransition(st).stream()
                .flatMap(tfs -> tfs.fluents.stream().map(f -> new TempFluent(
                        st.getEarliestStartTime(tfs.timepoints.iterator().next()),
                        TempFluent.DGFluent.getFluentWithChange(f, pp.store))))
                .forEach(tempFluents::add);

        Set<TempFluent> tasks = new HashSet<>();
        for(Task t : st.getOpenTasks()) {
            int est = st.getEarliestStartTime(t.start());
            for(GAction ga : new EffSet<>(pp.groundActionIntRepresentation(), st.csp.bindings().rawDomain(t.groundSupportersVar()).toBitSet())) {
                tasks.add(new TempFluent(est, TempFluent.DGFluent.from(ga.task, st.pb, pp.store)));
            }
        }

        // all facts (fluents an open tasks) in the current state
        List<TempFluent> allFacts = new ArrayList<>();
        allFacts.addAll(tempFluents);
        allFacts.addAll(tasks);

        // create new graph from the core graph (actions) and the facts
        StateDepGraph graph = new StateDepGraph(ext.getCoreGraph(), allFacts, pl);
        ext.currentGraph = graph;
        graph.propagate(ext.prevGraph);

        if(!pp.fluentsInitialized()) {
            IRSet<Fluent> fluents = new IRSet<>(pp.store.getIntRep(Fluent.class));
            for(Fluent f : graph.fluentsEAs.keys())
                fluents.add(f);
            pp.setPossibleFluents(fluents);
        }

        // unsupporting actions // TODO: shouldn't those be in the graph as well
        IRSet<GAction> unsupporting = new IRSet<>(gactsRep);
        for(Action a : st.getAllActions())
            if(!st.taskNet.isSupporting(a))
                unsupporting.addAll(st.getGroundActions(a));

        // for all open tasks, restrict their set of possible supporters to
        // all (i) all non-supporting action in the plan; and (ii) all action that are addable
        IRSet<GAction> potentialTaskSupporters = unsupporting.clone();
        potentialTaskSupporters.addAll(graph.addableActs);
        Domain taskSupportersDom = new Domain(potentialTaskSupporters.toBitSet());
        for(Task t : st.getOpenTasks())
            st.csp.bindings().restrictDomain(t.groundSupportersVar(), taskSupportersDom);

        // all task that can be added to the plan
        Set<GTask> addableTasks = new HashSet<>(); // FIXME: use specialized implementation
        for(GAction ga : graph.addableActs) {
            addableTasks.addAll(ga.subTasks);
        }

        // all actions that might be attached: (i) those that are open;
        // and (ii) those that can be inserted
        IRSet<GAction> attachable = new IRSet<>(gactsRep);
        for(Task t : st.getOpenTasks()) {
            IRSet<GAction> supporters = new IRSet<>(gactsRep, st.csp.bindings().rawDomain(t.groundSupportersVar()).toBitSet());
            attachable.addAll(supporters);
        }
        for(GAction ga : graph.addableActs)
            if(addableTasks.contains(ga.task))
                attachable.add(ga);
        // task-dependent and unattached actions restricted to the set of attachable actions
        Domain unattachedDomain = new Domain(attachable.toBitSet());
        for(Action a : st.getUnmotivatedActions())
            st.csp.bindings().restrictDomain(a.instantiationVar(), unattachedDomain);

        // populate the addable actions information in the state. This info is used to filter out resolvers
        st.addableActions = new EffSet<>(pl.preprocessor.groundActionIntRepresentation());
        st.addableActions.addAll(graph.addableActs);
        st.addableTemplates = new HashSet<>();
        for(GAction ga : graph.addableActs)
            st.addableTemplates.add(ga.abs);


        int initialMakespan = Integer.MIN_VALUE;
        // declare state a dead end if an open goal is not feasible
        for(Timeline og : st.tdb.getConsumers()) {
            int latest = st.getLatestStartTime(og.getConsumeTimePoint());
            int earliest = st.getEarliestStartTime(og.getConsumeTimePoint());
            boolean doable = false;
            int optimisticEarliestTime = Integer.MAX_VALUE;
            for(Fluent f : DisjunctiveFluent.fluentsOf(og.stateVariable, og.getGlobalConsumeValue(), st, pl)) {
                TempFluent.DGFluent dgf;
                if(og.hasSinglePersistence())
                    dgf = TempFluent.DGFluent.getBasicFluent(f, graph.core.store);
                else
                    dgf = TempFluent.DGFluent.getFluentWithChange(f, graph.core.store);
                final int ea = graph.earliestAppearances.containsKey(dgf) ? graph.earliestAppearances.get(dgf) : Integer.MAX_VALUE;
                if (ea  <= latest)
                    doable = true;
                if(optimisticEarliestTime > ea)
                    optimisticEarliestTime = ea;
            }
            if(!doable)
                // at least one open goal is not achievable
                st.setDeadEnd();
            else if(earliest < optimisticEarliestTime)
                // push back in time we had a too optimistic value
                st.enforceDelay(st.pb.start(), og.getConsumeTimePoint(), optimisticEarliestTime);
            initialMakespan = Math.max(initialMakespan, optimisticEarliestTime);
        }
        if(!Planning.quiet && isFirstPass && GlobalOptions.getBooleanOption("reachability-instrumentation")) {
            System.out.println("Initial Makespan: "+initialMakespan);
        }

        st.checkConsistency();
        isFirstPass = false;
    }
    private boolean isFirstPass = true;
}