package fape.core.planning.heuristics.temporal;

import fape.core.planning.states.StateExtension;
import fr.laas.fape.structures.*;

import java.util.*;

import fape.core.planning.heuristics.temporal.TempFluent.DGFluent;

public class DepGraphCore implements DependencyGraph {

    protected GStore store;

    // graph structure
    Map<ActionNode, List<MinEdge>> actOut = new HashMap<>();
    Map<DGFluent, List<MaxEdge>> fluentOut = new HashMap<>();
    Map<ActionNode, List<MaxEdge>> actIn = new HashMap<>();
    Map<DGFluent, List<MinEdge>> fluentIn = new HashMap<>();

    int dmax = Integer.MIN_VALUE;

    public DepGraphCore(Collection<RAct> actions, GStore store) { // List<TempFluent> facts, State st) {
        this.store = store;

        for(RAct act : actions) {
            addAction(act);
        }
    }

    public IR2IntMap<Node> getDefaultEarliestApprearances() {
        IR2IntMap<Node> eas = new IR2IntMap<Node>(store.getIntRep(Node.class));
        for(DGFluent f : fluentIn.keySet())
            eas.put(f.getID(), 0);
        for(ActionNode act : actIn.keySet())
            eas.put(act.getID(), 0);
        return eas;
    }

    public void addAction(RAct act) {
        actOut.put(act, new ArrayList<>());
        actIn.put(act, new ArrayList<>());
        for (TempFluent tf : act.getEffects()) {
            DGFluent f = tf.fluent;
            assert !DependencyGraph.isInfty(tf.getTime()) : "Effect with infinite delay.";
            actOut.get(act).add(new MinEdge(act, f, tf.getTime()));
            fluentIn.putIfAbsent(f, new ArrayList<>());
            fluentOut.putIfAbsent(f, new ArrayList<>());
            fluentIn.get(f).add(new MinEdge(act, f, tf.getTime()));
            dmax = Math.max(dmax, tf.getTime());
        }
        for(TempFluent tf : act.getConditions()) {
            final DGFluent f = tf.fluent;
            fluentOut.putIfAbsent(f, new ArrayList<>());
            fluentIn.putIfAbsent(f, new ArrayList<>());
            fluentOut.get(f).add(new MaxEdge(f, act, -tf.getTime()));
            actIn.get(act).add(new MaxEdge(f, act, -tf.getTime()));
            dmax = Math.max(dmax, -tf.getTime());
        }
    }


    @Override public Iterator<MaxEdge> inEdgesIt(ActionNode n) { return actIn.get(n).iterator(); }

    @Override public Iterator<MinEdge> outEdgesIt(ActionNode n) { return actOut.get(n).iterator(); }

    @Override public Iterator<MinEdge> inEdgesIt(DGFluent f) {
        if(fluentIn.containsKey(f))
            return fluentIn.get(f).iterator();
        else
            return Collections.emptyIterator();
    }

    @Override public Iterator<MaxEdge> outEdgesIt(DGFluent f) {
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




}
