package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planninggraph.GroundDTGs;
import fape.core.planning.planninggraph.RelaxedPlanExtractor;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.FAPEException;
import fape.exceptions.NoSolutionException;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.TPRef;
import planstack.graph.GraphFactory;
import planstack.graph.core.LabeledEdge;
import planstack.graph.core.MultiLabeledDigraph;
import planstack.graph.printers.NodeEdgePrinter;
import planstack.graph.printers.NodeEdgePrinterInterface;

import javax.swing.text.html.HTMLDocument;
import java.util.*;
import java.util.stream.Collectors;

public class OpenGoalTransitionFinder {

    protected static boolean debugging = false;

    static public class DualNode {
        public final int dtgID;
        public final int nodeID;
        public DualNode(int dtgID, int nodeID) {
            this.dtgID = dtgID;
            this.nodeID = nodeID;
        }
        public int hashCode() { return dtgID + nodeID * 64; }
        public boolean equals(Object o) {
            return o instanceof DualNode && ((DualNode) o).dtgID == dtgID && ((DualNode) o).nodeID == nodeID;
        }
    }
    static public class DualEdge {
        public final int dtgID;
        public final int edgeID;
        public DualEdge(int dtgID, int edgeID) {
            this.dtgID = dtgID;
            this.edgeID = edgeID;
        }
    }

    static public class Path {
        public final DualNode start;
        public final DualNode end;
        public final List<DualEdge> edges;
        public Path(DualNode start, DualNode end, List<DualEdge> edges) {
            this.start = start; this.end = end; this.edges = edges;
        }
    }

    public OpenGoalTransitionFinder(DTGCollection dtgs) {
        allDtgs = dtgs;
    }

    /** all dtgs of the system */
    final DTGCollection allDtgs;

    /** IDs of the dtg I can use (built with setDTGUsable */
    final List<Integer> dtgs = new ArrayList<>();
    List<DualNode> startNodes = new ArrayList<>();

    public void setDTGUsable(int dtgID) {
        assert !dtgs.contains(dtgID);
        dtgs.add(dtgID);
    }

    private class NodeCost implements Comparable<NodeCost> {
        public final DualNode n;
        public final int externalCost;
        public final int accumulatedCost;
        public final NodeCost pred;
        public final DualEdge e;

        public NodeCost(DualNode n, int extCost, int accCost, NodeCost pred, DualEdge edge) {
            this.n = n;
            this.externalCost = extCost;
            this.accumulatedCost = accCost;
            this.pred = pred;
            this.e = edge;
        }

        public int cost() {
            return externalCost + accumulatedCost;
        }

        @Override
        public int compareTo(NodeCost o) {
            return cost() - o.cost();
        }
    }

    public interface CostEvaluator {
        int cost(int liftedActionID, int gActionID, int fluentID);
        int distTo(int gActionID);
        boolean usable(int gActionID);

//        public boolean usable(GAction ga);
    }

    public TPRef startTimePoint =null;

    public void addTransitionTargets(Collection<Fluent> fluents, TPRef startTimePoint) {
        this.startTimePoint = startTimePoint;
        startNodes.addAll(allDtgs.entryPoints(dtgs, fluents));
    }

    public void addPersistenceTargets(Collection<Fluent> fluents, TPRef start, TPRef end, State st) {
        this.startTimePoint = start;
        startNodes.addAll(allDtgs.startPointForPersistence(dtgs, fluents, start, end));
    }

    public Path bestPath(CostEvaluator ce) throws NoSolutionException {
        Queue<NodeCost> q = new PriorityQueue<>();
        Map<DualNode, NodeCost> costs = new HashMap<>();
        for (DualNode n : startNodes) {
            NodeCost nc = new NodeCost(n, 0, 0, null, null);
            costs.put(n, nc);
            q.add(nc);
        }

        DualNode dest = null;
        while (dest == null && !q.isEmpty()) {

            NodeCost nc = q.poll();
            DTGImpl current = allDtgs.get(nc.n.dtgID);
            final int fluent = current.fluent(nc.n.nodeID);

            if (current.accepting(nc.n.nodeID)) {
                // best accepting node: finished!
                dest = nc.n;
            } else {
                PrimitiveIterator.OfInt it = current.inEdgesIterator(nc.n.nodeID);
                while(it.hasNext()) {
                    final int edgeID = it.next();
                    final int ga = current.gaction(edgeID);
                    final int lifted = current.laction(edgeID);
                    final int src = current.source(edgeID);
                    final int edgeDest = current.dest(edgeID);
                    assert edgeDest == nc.n.nodeID;

                    if(ga != -1 && !ce.usable(ga))
                        continue; // this action is not allowed

                    int accCost = nc.accumulatedCost + ce.cost(lifted, ga, current.fluent(edgeDest));
                    int actionCost = ga != -1 ? ce.distTo(ga) : 0;
                    int extCost = (actionCost > nc.externalCost) ? actionCost : nc.externalCost;

                    DualNode dualSource = new DualNode(nc.n.dtgID, src);
                    NodeCost ncNext = new NodeCost(dualSource, extCost, accCost, nc, new DualEdge(dualSource.dtgID, edgeID));

                    if(!costs.containsKey(ncNext.n)) { // no existing label, put it in queue whatever
                        costs.put(ncNext.n, ncNext);
                        q.add(ncNext);
                    } else if(costs.get(ncNext.n).cost() > ncNext.cost()) { // existing label, update if better
                        q.remove(costs.get(ncNext.n));
                        costs.put(ncNext.n, ncNext);
                        q.add(ncNext);
                    }
                }
                if(!current.isSink) {
                    for(int i : dtgs) {
                        DTGImpl dtg = allDtgs.get(i);
                        if(dtg == current)
                            continue;
                        PrimitiveIterator.OfInt itEntryNodes =  dtg.entryNodes(fluent);
                        while(itEntryNodes.hasNext()) {
                            final DualNode entryPoint = new DualNode(i, itEntryNodes.nextInt());
                            // give a very high cost to any path going through an already extended path.
                            final NodeCost ncNext = new NodeCost(entryPoint, nc.externalCost, nc.accumulatedCost, nc, null);
                            if (!costs.containsKey(ncNext.n)) { // no existing label, put it in queue whatever
                                costs.put(ncNext.n, ncNext);
                                q.add(ncNext);
                            } else if (costs.get(ncNext.n).cost() > ncNext.cost()) { // existing label, update if better
                                q.remove(costs.get(ncNext.n));
                                costs.put(ncNext.n, ncNext);
                                q.add(ncNext);
                            }
                        }
                    }
                }
            }
        }

        if(dest == null) {
            throw new NoSolutionException();
        }

        NodeCost cur = costs.get(dest);
        assert startTimePoint != null;
        return extractEdgeSequence(cur);
//        DomainTransitionGraph containingDTG = dtgs.get(cur.n.containerID);
//
//        PartialPathDTG partialPathDTG = new PartialPathDTG(extractEdgeSequence(cur), containingDTG);
//        return partialPathDTG;
    }

    private Path extractEdgeSequence(NodeCost nc) {
        final DualNode start = nc.n;
        final List<DualEdge> edges = new ArrayList<>();
        DualNode end = null;
        NodeCost cur = nc;
        while(cur != null) {
            end = cur.n;
            if(cur.e != null){
                edges.add(cur.e);
            }
            cur = cur.pred;
        }
        assert end != null : "Error: no end to this path.";
        return new Path(start, end, edges);
    }
}
