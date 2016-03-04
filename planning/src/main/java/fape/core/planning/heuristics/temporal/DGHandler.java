package fape.core.planning.heuristics.temporal;


import fape.core.planning.grounding.*;
import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.Preprocessor;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.util.EffSet;
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

public class DGHandler implements fape.core.planning.search.Handler {

    @Override
    public void stateBindedToPlanner(State st, APlanner pl) {
        assert !st.hasExtension(DepGraphCore.StateExt.class);

        // init the core of the dependency graph
        DepGraphCore core = new DepGraphCore(pl.preprocessor.getRelaxedActions(), pl.preprocessor.store);
        st.addExtension(new DepGraphCore.StateExt(core));

        // Record ground action ids as possible values for variables in the CSP
        for(GAction ga : pl.preprocessor.getAllActions()) {
            st.csp.bindings().addPossibleValue(ga.id);
        }

        // record all n ary constraints (action instantiations and task supporters)
        Set<String> recordedTask = new HashSet<>();
        for(AbstractAction aa : pl.pb.abstractActions()) {
            st.csp.bindings().recordEmptyNAryConstraint(aa.name(), true, aa.allVars().length+1);
            st.csp.bindings().addPossibleValue(aa.name());
            if(!recordedTask.contains(aa.taskName())) {
                st.csp.bindings().recordEmptyNAryConstraint(aa.taskName(), true, aa.args().size()+2);
                recordedTask.add(aa.taskName());
            }
        }

        for(GAction ga : pl.preprocessor.getAllActions()) {
            // values for all variables of this action
            List<String> values = new LinkedList<>();
            for(LVarRef var : ga.variables)
                values.add(ga.valueOf(var).instance());
            // add possible tuple to instantiation constraints
            st.csp.bindings().addAllowedTupleToNAryConstraint(ga.abs.name(), values, ga.id);

            // values for arguments of this action
            List<String> argValues = new LinkedList<>();
            for(LVarRef var : ga.abs.args())
                argValues.add(ga.valueOf(var).instance());
            argValues.add(ga.abs.name());
            // add possible tuple to supporter constraints
            st.csp.bindings().addAllowedTupleToNAryConstraint(ga.abs.taskName(), argValues, ga.id);
        }

        // notify ourself of the presence of any actions and tasks in the plan
        for(Action a : st.getAllActions())
            actionInserted(a, st, pl);
        for(Task t : st.getOpenTasks())
            taskInserted(t, st, pl);

        // trigger propagation of constraint networks
        st.isConsistent();
        propagateNetwork(st, pl);
        st.isConsistent();
    }

    @Override
    public void apply(State st, StateLifeTime time, APlanner planner) {
        if(time == StateLifeTime.SELECTION) {
            propagateNetwork(st, planner);
        }
    }

    @Override
    public void actionInserted(Action act, State st, APlanner pl) {
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
    public void taskInserted(Task task, State st, APlanner planner) {
        if(st.csp.bindings().isRecorded(task.methodSupportersVar()))
            return;

        assert !st.csp.bindings().isRecorded(task.methodSupportersVar());
        assert !st.csp.bindings().isRecorded(task.groundSupportersVar());
        Collection<String> supportingMethods = planner.pb.getSupportersForTask(task.name()).stream()
                .map(aa -> aa.name()).collect(Collectors.toList());

        st.csp.bindings().AddVariable(task.methodSupportersVar(), supportingMethods);
        st.csp.bindings().AddIntVariable(task.groundSupportersVar());

        List<VarRef> variables = new LinkedList<>();
        variables.addAll(task.args());
        variables.add(task.methodSupportersVar());
        variables.add(task.groundSupportersVar());
        st.addValuesSetConstraint(variables, task.name());
    }

    @Override
    public void supportLinkAdded(Action act, Task task, State st) {
        st.addUnificationConstraint(task.groundSupportersVar(), act.instantiationVar());
    }

    private void propagateNetwork(State st, APlanner pl) {
        final IntRep<GAction> gactsRep = pl.preprocessor.store.getIntRep(GAction.class);

        DepGraphCore.StateExt ext = st.getExtension(DepGraphCore.StateExt.class);

        final Preprocessor pp = st.pl.preprocessor;
        final GroundProblem gpb = pp.getGroundProblem();

        List<TempFluent> tempFluents = gpb.tempsFluents(st).stream()
                .flatMap(tfs -> tfs.fluents.stream().map(f -> new TempFluent(
                        st.getEarliestStartTime(tfs.timepoints.iterator().next()),
                        TempFluent.DGFluent.from(f, pp.store))))
                .collect(Collectors.toList());

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
        StateDepGraph graph = new StateDepGraph(ext.core, allFacts, pl);
        ext.currentGraph = graph;
        graph.propagate(ext.prevGraph);

        if(pp.allFluents == null) {
            pp.allFluents = new IRSet<>(pp.store.getIntRep(Fluent.class));
            for(Fluent f : graph.fluentsEAs.keys())
                pp.allFluents.add(f);
        }

        // unsupporting actions // TODO: shouldn't those be in the graph as well
        IRSet<GAction> unsupporting = new IRSet<GAction>(gactsRep);
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
        Set<GTask> addableTasks = new HashSet<>(); //FIXME: use specialized implementation
        for(GAction ga : graph.addableActs) {
            addableTasks.addAll(ga.subTasks);
        }

        // all actions that might be attached: (i) those that are open;
        // and (ii) those that can be inserted
        IRSet<GAction> attachable = new IRSet<GAction>(gactsRep);
        for(Task t : st.getOpenTasks()) {
            IRSet<GAction> supporters = new IRSet<GAction>(gactsRep, st.csp.bindings().rawDomain(t.groundSupportersVar()).toBitSet());
            attachable.addAll(supporters);
        }
        for(GAction ga : graph.addableActs)
            if(addableTasks.contains(ga.task))
                attachable.add(ga);
        // retricted task-dependent and unattached actions to the set of attachable actions
        Domain unattachedDomain = new Domain(attachable.toBitSet());
        for(Action a : st.getUnmotivatedActions())
            st.csp.bindings().restrictDomain(a.instantiationVar(), unattachedDomain);

        // populate the addable actions information in the state. This info is used to filter out resolvers
        st.addableActions = new EffSet<GAction>(pl.preprocessor.groundActionIntRepresentation());
        st.addableActions.addAll(graph.addableActs);
        st.addableTemplates = new HashSet<>();
        for(GAction ga : graph.addableActs)
            st.addableTemplates.add(ga.abs);



        // declare state a dead end if an open goal is not feasible
        for(Timeline og : st.tdb.getConsumers()) {
            int latest = st.getLatestStartTime(og.getConsumeTimePoint());
            boolean doable = false;
            for(Fluent f : DisjunctiveFluent.fluentsOf(og.stateVariable, og.getGlobalConsumeValue(), st, pl)) {
                TempFluent.DGFluent dgf = TempFluent.DGFluent.from(f, graph.core.store);
                if(graph.earliestAppearances.containsKey(dgf) && graph.earliestAppearances.get(dgf) <= latest) {
                    doable = true;
                    break;
                }
            }
            if(!doable)
                st.setDeadEnd();
        }

        st.isConsistent();
    }
}