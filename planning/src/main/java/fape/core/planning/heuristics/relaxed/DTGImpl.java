package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.LStatementRef;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.statements.Assignment;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.Arrays;
import java.util.Collection;
import java.util.PrimitiveIterator;

public class DTGImpl {

    private static final int EDGE_SIZE = 4; /* src, dest, lifted act, ground act */
    private static final int NODE_SIZE = 6; /* fluent, level, start, end, accepting, entry-point */
    private static final int EMPTY_NODE = -5;

    private static final int ACCEPTING = 1, NON_ACCEPTING = 0;
    private static final int ENTRY_POINT =1, NOT_ENTRY_POINT = 0;

    private int nextNode = 1;
    private int nextEdge = 0;

    private int edgeIndex(int edge) { return edge * EDGE_SIZE; }
    private int nodeIndex(int node) { return node * NODE_SIZE; }
    private int id(int fluent, int lvl) { return fluentByLvl[(fluent * numLevels) + lvl]; }
    private int id(Fluent f, int lvl) { if(f != null) return id(f.getID(), lvl); else return 0; }
    private boolean hasNode(int fluent, int lvl) { return (fluent*numLevels) < fluentByLvl.length && id(fluent, lvl) != -1; }
    private void setId(int id, int fluent, int lvl) {
        if(fluentByLvl.length <= fluent*numLevels) {
            final int oldSize = fluentByLvl.length;
            fluentByLvl = Arrays.copyOf(fluentByLvl, (fluent+1)*numLevels);
            Arrays.fill(fluentByLvl, oldSize, fluentByLvl.length, -1);
        }
        fluentByLvl[(fluent * numLevels)+lvl] = id; }

    public final int source(int edge) { return edges[edgeIndex(edge)]; }
    public final int dest(int edge) { return edges[edgeIndex(edge)+1]; }
    public final int laction(int edge) { return edges[edgeIndex(edge)+2]; }
    public final int gaction(int edge) { return edges[edgeIndex(edge)+3]; }

    private void setEdge(int edge, int source, int dest, int lifted, int ground) {
        assert source != -1 && dest != -1;
        final int idx = edgeIndex(edge);
        edges[idx] = source;
        edges[idx+1] = dest;
        edges[idx+2] = lifted;
        edges[idx+3] = ground;

        if(numInEdge(dest) >= inEdges[dest].length-1)
            inEdges[dest] = Arrays.copyOf(inEdges[dest], inEdges[dest].length*2);
        inEdges[dest][0] += 1; // increment number of incoming edges
        inEdges[dest][inEdges[dest][0]] = edge;
    }

    public final int fluent(int node) { return nodes[nodeIndex(node)]; }
    final int lvl(int node) { return nodes[nodeIndex(node)+1]; }
    public final int start(int node) { return nodes[nodeIndex(node)+2]; }
    public final int end(int node) { return nodes[nodeIndex(node)+3]; }
    public final boolean accepting(int node) { return nodes[nodeIndex(node)+4] == ACCEPTING; }
    public final boolean entryPoint(int node) { return nodes[nodeIndex(node)+5] == ENTRY_POINT; }
    final int numInEdge(int node) { return inEdges[node][0]; }

    private void setAccepting(int node) { nodes[nodeIndex(node)+4] = ACCEPTING; }
    private void setEntryPoint(int node) { nodes[nodeIndex(node)+5] = ENTRY_POINT; }

    public int getNumNodes() {
        return nextNode;
    }

    public int getNumLevels() {
        return numLevels;
    }

    private void setNode(int node, int fluent, int lvl, int start, int end) {
        final int idx =nodeIndex(node);
        nodes[idx] = fluent;
        nodes[idx+1] = lvl;
        nodes[idx+2] = start;
        nodes[idx+3] = end;
        inEdges[node] = new int[5];
        inEdges[node][0] = 0; // first one is the number of edges in this list
        setId(node, fluent, lvl);
    }

    private int nextEdgeID() {
        if(edges.length <= nextEdge * EDGE_SIZE) // grow
            edges = Arrays.copyOf(edges, edges.length*2); //new int[tmp.length*2];
        return nextEdge++;
    }
    private int nextNodeID() {
        if(nodes.length <= nextNode * NODE_SIZE) {
            nodes = Arrays.copyOf(nodes, nodes.length*2);
            inEdges = Arrays.copyOf(inEdges, inEdges.length*2);
        }
        return nextNode++;
    }

    public int addNodeIfAbsent(Fluent f, int lvl, TPRef start, TPRef end) {
        if(hasNode(f, lvl))
            return id(f, lvl);
        else
            return addNode(f, lvl, start, end);
    }

    public int addNode(Fluent f, int lvl, TPRef start, TPRef end) {
        if(f == null) {
            assert lvl == 0 : "Null node can only exist at level 0.";
            return 0; // null fluent is always represented by node 0.
        }
        assert !hasNode(f.getID(), lvl) : "Node already recorded.";
        int id = nextNodeID();
        setNode(id, f.getID(), lvl, start != null ? start.id() : -1, end != null ?end.id() : -1);
        return id;
    }

    public int addEdge(Fluent src, int srcLvl, Action lifted, GAction ground, Fluent dest, int destLvl) {
        assert srcLvl < numLevels && destLvl < numLevels;
        assert hasNode(src, srcLvl) : "Source node not recorded.";
        assert hasNode(dest, destLvl) : "Dest node not recorded.";
        int id = nextEdgeID();
        setEdge(id, id(src, srcLvl), id(dest, destLvl), lifted!=null ? lifted.id().id() : -1, ground != null ? ground.id :-1);
        return id;
    }

    int[] edges = new int[10*EDGE_SIZE];
    int[] nodes = new int[10*NODE_SIZE];
    final int numLevels;
    public final boolean isSink;
    int[] fluentByLvl;
    int[][] inEdges = new int[10][];

    public DTGImpl(int numLevels, boolean isSink) {
        this.numLevels = numLevels;
        this.isSink = isSink;
        fluentByLvl = new int[10*numLevels];
        Arrays.fill(fluentByLvl, -1);
    }

    public PrimitiveIterator.OfInt inEdgesIterator(final int node) {
        return new PrimitiveIterator.OfInt() {
            final int[] edges = inEdges[node];
            int current = 0;
            @Override public int nextInt() {
                assert edges[current+1] != -1;
                return edges[1+current++];
            }

            @Override
            public boolean hasNext() {
                return edges != null && current < edges[0];
            }
        };
    }

    public void setAccepting(Fluent f, int lvl) {
        setAccepting(id(f, lvl));
    }

    public void setEntryPoint(Fluent f, int lvl) {
        setEntryPoint(id(f, lvl));
    }

    public boolean hasNode(Fluent f, int level) {
        if(f != null)
            return hasNode(f.getID(), level);
        assert level == 0;
        return true;
    }

    private int nextNodeOfFluent(int fluent, int from) {
        int i = from;
        while(fluent(from) != fluent && i < nextNode)
            i++;
        if(i == nextNode)
            return -1;
        assert(fluent(i) == fluent);
        return i;
    }

    public PrimitiveIterator.OfInt entryNodes(Fluent f) {
        assert f != null : "Cannot enter with null fluent";
        return entryNodes(f.getID());
    }

    public PrimitiveIterator.OfInt entryNodes(final int fluent) {
        return new PrimitiveIterator.OfInt() {
            private int nextFrom(int from) { // returns next entry point with same fluent
                int i = from;
                while(i < nextNode && (fluent(i) != fluent || !entryPoint(i)))
                    i++;
                if(i == nextNode)
                    return -1;
                assert(fluent(i) == fluent);
                return i;
            }
            int curNode = nextFrom(0);
            @Override
            public int nextInt() {
                int ret = curNode;
                curNode = nextFrom(curNode+1);
                return ret;
            }

            @Override
            public boolean hasNext() {
                return curNode != -1;
            }
        };
    }



    public static DTGImpl buildFromTimeline(Timeline tl, APlanner planner, State st) {
        DTGImpl dtg = new DTGImpl(tl.numChanges()+1, true);

        FeasibilityReasoner reas = planner.preprocessor.getFeasibilityReasoner();

        for(int i=0 ; i<tl.numChanges() ; i++) {
            ChainComponent cc = tl.getChangeNumber(i);

            LogStatement s = cc.getFirst();
            if(i == 0) { // first statement
                if(s instanceof Assignment) {
                    dtg.addNode(null, 0, null, s.start());
                    dtg.setAccepting(null, 0);
                } else {
                    Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.startValue(), st, planner);
                    for(Fluent f : fluents) {
                        dtg.addNode(f, 0, null, s.start());
                        dtg.setAccepting(f, 0);
                    }
                }
            }

            // action by which this statement was introduced (null if no action)
            Action containingAction = st.getActionContaining(s);
            TPRef start = s.end();
            TPRef end = i+1 < tl.numChanges() ? tl.getChangeNumber(i+1).getConsumeTimePoint() : null;

            if(containingAction == null) { // statement was not added as part of an action
                assert s instanceof Assignment;
                assert s.endValue() instanceof InstanceRef;
                assert i == 0;
                Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.endValue(), st, planner); //todo why false

                for(Fluent f : fluents) {
                    assert(dtg.hasNode(null, i)); // FROM
                    if(!dtg.hasNode(f, i + 1)) // TO
                        dtg.addNode(f, i + 1, start, end);
                    dtg.addEdge(null, i, null, null, f, i+1);

                    if(i == tl.numChanges() -1) { // this is the last transition of this timeline, link to the DTG
                        dtg.setEntryPoint(f, i + 1);
                    }
                }
            } else { // statement was added as part of an action or a decomposition
                Collection<GAction> acts = st.getGroundActions(containingAction);

                // local reference of the statement, used to extract the corresponding ground statement from the GAction
                assert containingAction.context().contains(s);
                LStatementRef statementRef = containingAction.context().getRefOfStatement(s);

                for(GAction ga : acts) {
                    GAction.GLogStatement gs = ga.statementWithRef(statementRef);
                    Fluent fromFluent;
                    Fluent toFluent;

                    if(gs instanceof GAction.GTransition) {
                        fromFluent = planner.preprocessor.getFluent(gs.sv, ((GAction.GTransition) gs).from);
                        toFluent = planner.preprocessor.getFluent(gs.sv, ((GAction.GTransition) gs).to);

                    } else {
                        assert gs instanceof GAction.GAssignment;
                        fromFluent = null;
                        toFluent = planner.preprocessor.getFluent(gs.sv, ((GAction.GAssignment) gs).to);
                    }

                    if(dtg.hasNode(fromFluent, i)) {
                        if(!dtg.hasNode(toFluent, i+1))
                            dtg.addNode(toFluent, i+1, start, end);

                        dtg.addEdge(fromFluent, i, containingAction, ga, toFluent, i+1);
                        if(i == tl.numChanges()-1) {
                            dtg.setEntryPoint(toFluent, i+1);
                        }
                    }
                }
            }
        }
        return dtg;
    }
}
