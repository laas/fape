package fape.core.planning.heuristics.temporal;


import fape.core.planning.grounding.*;
import fape.core.planning.heuristics.Preprocessor;
import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.util.EffSet;
import fr.laas.fape.structures.IRSet;
import fr.laas.fape.structures.IntRep;
import planstack.anml.model.LVarRef;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;
import planstack.anml.model.concrete.VarRef;
import planstack.constraints.bindings.Domain;

import java.util.*;
import java.util.stream.Collectors;

public class DGHandler implements fape.core.planning.search.Handler {

    private void initState(State st, APlanner pl) {
        if(!st.hasExtension(DepGraphCore.StateExt.class)) {
            // init the core of the dependency graph
            DepGraphCore core = new DepGraphCore(pl.preprocessor.getRelaxedActions(), pl.preprocessor.store);
            st.addExtension(new DepGraphCore.StateExt(core));

            for(Action a : st.getAllActions())
                actionInserted(a, st, pl);
            for(Task t : st.getOpenTasks())
                taskInserted(t, st, pl);
        }
    }

    private IRSet<GAction> allActions(State st, APlanner pl) {
        if(st.getExtension(DepGraphCore.StateExt.class).currentGraph != null)
            return st.getExtension(DepGraphCore.StateExt.class).currentGraph.addableActs;
        else
            return new IRSet<GAction>(pl.preprocessor.store.getIntRep(GAction.class), pl.preprocessor.getAllActions().toBitSet());

    }

    @Override
    public void apply(State st, StateLifeTime time, APlanner planner) {
        if(time == StateLifeTime.SELECTION) {
            propagateNetwork(st, planner);
        }
    }

    @Override
    public void actionInserted(Action act, State st, APlanner pl) {
        initState(st, pl);
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
        st.csp.bindings().addIntVariable(act.instantiationVar(), new Domain(allActions(st,pl).toBitSet()));
        values.add(act.instantiationVar());
        st.addValuesSetConstraint(values, act.abs().name());

        assert st.csp.bindings().isRecorded(act.instantiationVar());
    }

    @Override
    public void taskInserted(Task task, State st, APlanner planner) {
        initState(st, planner);
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
        initState(st, pl);

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
        StateDepGraph graph = new StateDepGraph(ext.core, allFacts);
        ext.currentGraph = graph;
        graph.propagate(ext.prevGraph);

        // unsupporting actions // TODO: shouldn't those be in the graph as well
        IRSet<GAction> unsupporting = new IRSet<GAction>(gactsRep);
        for(Action a : st.getAllActions())
            if(!st.taskNet.isSupporting(a))
                unsupporting.addAll(getGroundActions(a, st));

        // for all open tasks, restrict their set of possible supporters to
        // all (i) all non-supporting action in the plan; and (ii) all action that are addable
        IRSet<GAction> potentialTaskSupporters = unsupporting.clone();
        potentialTaskSupporters.addAll(graph.addableActs);
        Domain taskSupportersDom = new Domain(potentialTaskSupporters.toBitSet());
        for(Task t : st.getOpenTasks())
            st.csp.bindings().restrictDomain(t.groundSupportersVar(), taskSupportersDom);

        // all task that can be added to the plan
        Set<GTaskCond> addableTasks = new HashSet<>(); //FIXME: use specialized implementation
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

        st.addableActions = new EffSet<GAction>(pl.preprocessor.groundActionIntRepresentation());
        st.addableActions.addAll(graph.addableActs);
        st.addableTemplates = new HashSet<>();
        for(GAction ga : graph.addableActs)
            st.addableTemplates.add(ga.abs);

        for(Timeline og : st.tdb.getConsumers()) {
            boolean doable = false;
            for(Fluent f : DisjunctiveFluent.fluentsOf(og.stateVariable, og.getGlobalConsumeValue(), st, pl)) {
                TempFluent.DGFluent dgf = TempFluent.DGFluent.from(f, graph.core.store);
                if(graph.earliestAppearances.containsKey(dgf)) {
                    doable = true;
                    break;
                }
            }
            if(!doable)
                st.setDeadEnd();
        }

        st.isConsistent();
    }

    /** Returns a set of all ground actions this lifted action might be instantiated to */
    private static IRSet<GAction> getGroundActions(Action lifted, State st) {
        assert st.csp.bindings().isRecorded(lifted.instantiationVar());
        Domain dom = st.csp.bindings().rawDomain(lifted.instantiationVar());
        return new IRSet<GAction>(st.pl.preprocessor.store.getIntRep(GAction.class), dom.toBitSet());
    }
}