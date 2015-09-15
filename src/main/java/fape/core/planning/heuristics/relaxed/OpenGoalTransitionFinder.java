package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.planner.APlanner;
import fape.exceptions.FAPEException;
import fape.exceptions.NoSolutionException;
import planstack.anml.model.concrete.TPRef;

import java.util.*;

public class OpenGoalTransitionFinder {

    protected static boolean debugging = false;

    static public class DualNode {
        public final int dtgID;
        public final int nodeID;
        public DualNode(int dtgID, int nodeID) {
            this.dtgID = dtgID;
            this.nodeID = nodeID;
        }
        @Override public int hashCode() { return dtgID + nodeID * 64; }
        @Override public boolean equals(Object o) {
            return o instanceof DualNode && ((DualNode) o).dtgID == dtgID && ((DualNode) o).nodeID == nodeID;
        }
        @Override public String toString() { return "("+dtgID+", "+nodeID+")"; }
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
        public final int dtgOfStart;
        public Path(DualNode start, DualNode end, List<DualEdge> edges, int dtgOfStart) {
            this.start = start; this.end = end; this.edges = edges; this.dtgOfStart = dtgOfStart;
        }
    }
    final APlanner planner;

    public OpenGoalTransitionFinder(APlanner planner, DTGCollection dtgs, boolean printDebugInformation) {
        allDtgs = dtgs; this.printDebugInformation = printDebugInformation;
        this.planner = planner;
    }

    /** all dtgs of the system */
    final DTGCollection allDtgs;

    final boolean printDebugInformation;

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
        @Override public String toString() { return n +" "+cost(); }
    }

    public interface CostEvaluator {
        int cost(int liftedActionID, int gActionID, int fluentID);
        int distTo(int gActionID);
        boolean addable(int gActionID);
        boolean possibleInPlan(int gActionID);
    }

    public TPRef startTimePoint =null;

    public void addTransitionTargets(Collection<Fluent> fluents, TPRef startTimePoint) throws NoSolutionException {
        this.startTimePoint = startTimePoint;
        startNodes.addAll(allDtgs.entryPoints(dtgs, fluents));
        if(startNodes.isEmpty()) {
            for(int dtgID : dtgs) {
                System.out.println(dtgID+" "+allDtgs.fluentsInDTG(dtgID));
            }
            throw new FAPEException("No start point for transition fluents: " + fluents);
        }
    }

    public void addPersistenceTargets(Collection<Fluent> fluents, TPRef start, TPRef end) throws NoSolutionException {
        this.startTimePoint = start;
        startNodes.addAll(allDtgs.startPointForPersistence(dtgs, fluents, start, end));
        if(startNodes.isEmpty()) {
            for(int dtgID : dtgs) {
                System.out.println(dtgID+" "+allDtgs.fluentsInDTG(dtgID));
            }
            throw new FAPEException("No start point for persistence fluents: " + fluents);
        }
    }

    public Path bestPath(CostEvaluator ce) throws NoSolutionException {
        Queue<NodeCost> q = new PriorityQueue<>();
        Map<DualNode, NodeCost> costs = new HashMap<>();
        for (DualNode n : startNodes) {
            NodeCost nc = new NodeCost(n, 0, 0, null, null);
            costs.put(n, nc);
            q.add(nc);
        }

        if(printDebugInformation) {
            System.out.println("Looking for a path in DTGs: ");
            for (int dtgID : dtgs)
                System.out.println(dtgID + " " + allDtgs.fluentsByLevels(dtgID));
        }

        int dtgOfStart = -1;

        DualNode dest = null;
        while (dest == null && !q.isEmpty()) {

            NodeCost nc = q.poll();
            DTGImpl current = allDtgs.get(nc.n.dtgID);
            final int fluent = current.fluent(nc.n.nodeID);

            if(printDebugInformation)
                System.out.println(nc.n+" "+nc.cost());

            if (current.accepting(nc.n.nodeID)) {
                // best accepting node: finished!
                dest = nc.n;
                dtgOfStart = nc.n.dtgID;
            } else if(!current.isSink && allDtgs.dtgWithPathEnd(fluent, dtgs) != -1) {
                // this fluent represents the end of path taken for another open goal. We can stop here
                //TODO: this might benefit from a penalty if it was not the latest added
                dtgOfStart = allDtgs.dtgWithPathEnd(fluent, dtgs);
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

                    if(printDebugInformation)
                        System.out.print("  "+edgeDest);

                    if(lifted == -1 && ga != -1 && !ce.addable(ga)) {
                        if(printDebugInformation)
                            System.out.println("  not usable ground act: "+planner.preprocessor.getGroundAction(ga));
                        continue; // this action is not allowed
                    }
                    if(lifted != -1 && ga != -1 && !ce.possibleInPlan(ga)) {
                        if(printDebugInformation)
                            System.out.println("  not usable instantiation into :"+planner.preprocessor.getGroundAction(ga));
                        continue; // this action is not allowed
                    }

                    int accCost = nc.accumulatedCost + ce.cost(lifted, ga, current.fluent(edgeDest));
                    int actionCost = ga != -1 ? ce.distTo(ga) : 0;
                    int extCost = (actionCost > nc.externalCost) ? actionCost : nc.externalCost;

                    DualNode dualSource = new DualNode(nc.n.dtgID, src);
                    NodeCost ncNext = new NodeCost(dualSource, extCost, accCost, nc, new DualEdge(dualSource.dtgID, edgeID));

                    if(printDebugInformation)
                        System.out.println("  added: "+ncNext);

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
                        if(dtg == current || !dtg.isSink)
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
            throw new NoSolutionException("Could not find a path in the DTGs.");
        }

        NodeCost cur = costs.get(dest);
        assert startTimePoint != null;
        assert dtgOfStart != -1;
        return extractEdgeSequence(cur, dtgOfStart);
    }

    private Path extractEdgeSequence(NodeCost nc, int dtgOfStart) {
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
        return new Path(start, end, edges, dtgOfStart);
    }
}
