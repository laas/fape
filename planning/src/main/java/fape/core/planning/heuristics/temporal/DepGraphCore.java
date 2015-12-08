package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GroundProblem;
import fape.core.planning.heuristics.Preprocessor;
import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.states.StateExtension;
import fape.util.EffSet;
import fr.laas.fape.structures.*;

import java.util.*;
import java.util.stream.Collectors;

import fape.core.planning.heuristics.temporal.TempFluent.Fluent;
import planstack.anml.model.concrete.Task;

public class DepGraphCore implements DependencyGraph {

    protected GStore store;

    // graph structure
    Map<ActionNode, List<MinEdge>> actOut = new HashMap<>();
    Map<Fluent, List<MaxEdge>> fluentOut = new HashMap<>();
    Map<ActionNode, List<MaxEdge>> actIn = new HashMap<>();
    Map<Fluent, List<MinEdge>> fluentIn = new HashMap<>();

    IR2IntMap<Node> optimisticEST;
    int dmax = Integer.MIN_VALUE;

    public DepGraphCore(Collection<RAct> actions, GStore store) { // List<TempFluent> facts, State st) {
        this.store = store;

        for(RAct act : actions) {
            addAction(act);
        }
    }

    public IR2IntMap<Node> getDefaultEarliestApprearances() {
        IR2IntMap<Node> eas = new IR2IntMap<Node>(store.getIntRep(Node.class));
        for(Fluent f : fluentIn.keySet())
            eas.put(f.getID(), 0);
        for(ActionNode act : actIn.keySet())
            eas.put(act.getID(), 0);
        return eas;
    }

    public void addAction(RAct act) {
        actOut.put(act, new ArrayList<>());
        actIn.put(act, new ArrayList<>());
        for (TempFluent tf : act.getEffects()) {
            Fluent f = tf.fluent;
            assert !DependencyGraph.isInfty(tf.getTime()) : "Effect with infinite delay.";
            actOut.get(act).add(new MinEdge(act, f, tf.getTime()));
            fluentIn.putIfAbsent(f, new ArrayList<>());
            fluentOut.putIfAbsent(f, new ArrayList<>());
            fluentIn.get(f).add(new MinEdge(act, f, tf.getTime()));
            dmax = Math.max(dmax, tf.getTime());
        }
        for(TempFluent tf : act.getConditions()) {
            final Fluent f = tf.fluent;
            fluentOut.putIfAbsent(f, new ArrayList<>());
            fluentIn.putIfAbsent(f, new ArrayList<>());
            fluentOut.get(f).add(new MaxEdge(f, act, -tf.getTime()));
            actIn.get(act).add(new MaxEdge(f, act, -tf.getTime()));
            dmax = Math.max(dmax, -tf.getTime());
        }
    }


    @Override public Iterator<MaxEdge> inEdges(ActionNode n) { return actIn.get(n).iterator(); }

    @Override public Iterator<MinEdge> outEdges(ActionNode n) { return actOut.get(n).iterator(); }

    @Override public Iterator<MinEdge> inEdges(Fluent f) {
        if(fluentIn.containsKey(f))
            return fluentIn.get(f).iterator();
        else
            return Collections.emptyIterator();
    }

    @Override public Iterator<MaxEdge> outEdges(Fluent f) {
        return fluentOut.containsKey(f) ? fluentOut.get(f).iterator() : Collections.emptyIterator();
    }

    /**
     * An instance of this class is to be attached to
     */
    public static class StateExt implements StateExtension {

        public final DepGraphCore core;
        public final Optional<StateDepGraph> prevGraph;

        public StateDepGraph currentGraph = null;

        public StateExt(DepGraphCore core) {
            this.core = core;
            prevGraph = Optional.empty();
        }

        private StateExt(StateDepGraph prevGraph) {
            this.core = prevGraph.core;
            this.prevGraph = Optional.of(prevGraph);
        }

        @Override
        public StateExt clone() {
            if(currentGraph != null)
                return new StateExt(currentGraph);
            else
                return new StateExt(core);
        }
    }

    public static class Handler implements fape.core.planning.search.Handler {

        @Override
        public void apply(State st, StateLifeTime time, APlanner planner) {
            if(time == StateLifeTime.SELECTION) {
                reachabilityCheck(st, planner);
            }
        }

        private void reachabilityCheck(State st, APlanner pl) {
            if(!st.hasExtension(StateExt.class)) {
                // init the core of the dependency graph
                DepGraphCore core = new DepGraphCore(pl.preprocessor.getRelaxedActions(), pl.preprocessor.store);
                st.addExtension(new StateExt(core));
            }

            StateExt ext = st.getExtension(StateExt.class);

            final Preprocessor pp = st.pl.preprocessor;
            final GroundProblem gpb = pp.getGroundProblem();

            List<TempFluent> tempFluents = gpb.tempsFluents(st).stream()
                    .flatMap(tfs -> tfs.fluents.stream().map(f -> new TempFluent(
                            st.getEarliestStartTime(tfs.timepoints.iterator().next()),
                            TempFluent.Fluent.from(f, pp.store))))
                    .collect(Collectors.toList());

            Set<TempFluent> tasks = new HashSet<>();
            for(Task t : st.getOpenTasks()) {
                int est = st.getEarliestStartTime(t.start());
                for(GAction ga : new EffSet<>(pp.groundActionIntRepresentation(), st.csp.bindings().rawDomain(t.groundSupportersVar()).toBitSet())) {
                    tasks.add(new TempFluent(est, TempFluent.Fluent.from(ga.task, st.pb, pp.store)));
                }
            }

            List<TempFluent> allFacts = new LinkedList<>();
            allFacts.addAll(tempFluents);
            allFacts.addAll(tasks);

            StateDepGraph graph = new StateDepGraph(ext.core, allFacts);
            ext.currentGraph = graph;
            graph.propagate(ext.prevGraph);
        }

    }


}
