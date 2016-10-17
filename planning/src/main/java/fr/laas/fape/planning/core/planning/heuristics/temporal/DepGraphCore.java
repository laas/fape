package fr.laas.fape.planning.core.planning.heuristics.temporal;

import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.states.StateExtension;
import fr.laas.fape.structures.IR2IntMap;
import fr.laas.fape.structures.IRSet;

import java.util.*;

public class DepGraphCore implements DependencyGraph {

    /** True if this graph has already been reduced to reachable actions only */
    public final boolean wasReduced;

    protected GStore store;

    // graph structure
    Map<ActionNode, List<MinEdge>> actOut = new HashMap<>();
    Map<TempFluent.DGFluent, List<MaxEdge>> fluentOut = new HashMap<>();
    Map<ActionNode, List<MaxEdge>> actIn = new HashMap<>();
    Map<TempFluent.DGFluent, List<MinEdge>> fluentIn = new HashMap<>();

    int dmax = Integer.MIN_VALUE;
    /** All nodes with an incoming negative or null edge. This field is lazy */
    private IRSet<TempFluent.DGFluent> nodesWithIncomingNegEdge;

    /** All edges that should be ignored in the Dijkstra propagation.
     *  If they are ignored the graph will not contain any negative cycle. //TODO: double check that */
    private List<MaxEdge> toIgnoreInDijkstra;

    public DepGraphCore(Collection<RAct> actions, boolean isReduced, GStore store) { // List<TempFluent> facts, State st) {
        this.store = store;
        this.wasReduced = isReduced;

        for(RAct act : actions) {
            addAction(act);
        }
    }

    public IR2IntMap<Node> getDefaultEarliestApprearances() {
        IR2IntMap<Node> eas = new IR2IntMap<Node>(store.getIntRep(Node.class));
        for(TempFluent.DGFluent f : fluentIn.keySet())
            eas.put(f.getID(), 0);
        for(ActionNode act : actIn.keySet())
            eas.put(act.getID(), 0);
        return eas;
    }

    public void addAction(RAct act) {
        actOut.put(act, new ArrayList<>());
        actIn.put(act, new ArrayList<>());
        for (TempFluent tf : act.getEffects()) {
            TempFluent.DGFluent f = tf.fluent;
            assert !DependencyGraph.isInfty(tf.getTime()) : "Effect with infinite delay.";
            actOut.get(act).add(new MinEdge(act, f, tf.getTime()));
            fluentIn.putIfAbsent(f, new ArrayList<>());
            fluentOut.putIfAbsent(f, new ArrayList<>());
            fluentIn.get(f).add(new MinEdge(act, f, tf.getTime()));
            dmax = Math.max(dmax, tf.getTime());
        }
        for(TempFluent tf : act.getConditions()) {
            final TempFluent.DGFluent f = tf.fluent;
            fluentOut.putIfAbsent(f, new ArrayList<>());
            fluentIn.putIfAbsent(f, new ArrayList<>());
            fluentOut.get(f).add(new MaxEdge(f, act, -tf.getTime()));
            actIn.get(act).add(new MaxEdge(f, act, -tf.getTime()));
            dmax = Math.max(dmax, -tf.getTime());
        }
    }


    @Override public Iterator<MaxEdge> inEdgesIt(ActionNode n) { return actIn.get(n).iterator(); }

    @Override public Iterator<MinEdge> outEdgesIt(ActionNode n) { return actOut.get(n).iterator(); }

    @Override public Iterator<MinEdge> inEdgesIt(TempFluent.DGFluent f) {
        if(fluentIn.containsKey(f))
            return fluentIn.get(f).iterator();
        else
            return Collections.emptyIterator();
    }

    @Override public Iterator<MaxEdge> outEdgesIt(TempFluent.DGFluent f) {
        return fluentOut.containsKey(f) ? fluentOut.get(f).iterator() : Collections.emptyIterator();
    }

    protected IRSet<TempFluent.DGFluent> getFluentsWithIncomingNegEdge() {
        if(nodesWithIncomingNegEdge == null) {
            nodesWithIncomingNegEdge = new IRSet<>(store.getIntRep(TempFluent.DGFluent.class));

            for (TempFluent.DGFluent f : fluentIn.keySet()) {
                for (MinEdge e : inEdges(f)) {
                    if (e.delay <= 0) {
                        nodesWithIncomingNegEdge.add(f);
                    }
                }
            }
        }
        return nodesWithIncomingNegEdge;
    }

    protected List<MaxEdge> getEdgesToIgnoreInDijkstra() {
        if(toIgnoreInDijkstra == null) {
            toIgnoreInDijkstra = new ArrayList<>();
            for(List<MaxEdge> maxEdges : actIn.values()) {
                for(MaxEdge e : maxEdges) {
                    if(e.delay < 0 || getFluentsWithIncomingNegEdge().contains(e.fluent))
                        toIgnoreInDijkstra.add(e);
                }
            }
        }
        return toIgnoreInDijkstra;
    }

    /**
     * An instance of this class is to be attached to
     */
    public static class StateExt implements StateExtension {

        private final DepGraphCore core;
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

        public DepGraphCore getCoreGraph() {
            if(currentGraph != null)
                return currentGraph.core; // this version might be more recent
            else
                return core;
        }

        @Override
        public StateExt clone(State st) {
            if(currentGraph != null)
                return new StateExt(currentGraph);
            else
                return new StateExt(core);
        }
    }




}
