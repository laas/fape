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

    Map<Integer, DomainTransitionGraph> dtgs = new HashMap<>();
    public List<DomainTransitionGraph.DTNode> startNodes = new LinkedList<>();

    public void addDTG(DomainTransitionGraph dtg) {
        assert !dtgs.containsKey(dtg.id());
        dtgs.put(dtg.id(), dtg);
    }

    private class NodeCost implements Comparable<NodeCost> {
        public final DomainTransitionGraph.DTNode n;
        public final int externalCost;
        public final int accumulatedCost;
        public final NodeCost pred;
        public final DomainTransitionGraph.DTEdge e;

        public NodeCost(DomainTransitionGraph.DTNode n, int extCost, int accCost, NodeCost pred, DomainTransitionGraph.DTEdge edge) {
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
        public int cost(Action a, GAction ga, GStateVariable sv);
        public int distTo(GAction ga);

//        public boolean usable(GAction ga);
    }

    public TPRef startTimePoint =null;

    public void addTransitionTargets(Collection<Fluent> fluents, TPRef startTimePoint) {
        this.startTimePoint = startTimePoint;
        for(DomainTransitionGraph dtg : dtgs.values()) {
            for (Fluent f : fluents) {
                DomainTransitionGraph.DTNode start = dtg.startNodeForFluent(f);
                if(start != null) {
                    assert dtg instanceof GroundDTGs.DTG;
                    startNodes.add(start);
                }
            }
        }
    }

    public void addPersistenceTargets(Collection<Fluent> fluents, TPRef start, TPRef end, State st) {
        this.startTimePoint = start;
        for(Fluent f : fluents) {
            for(DomainTransitionGraph dtg : dtgs.values()) {
                startNodes.addAll(dtg.unifiableNodes(f, start, end, st));
            }
        }
    }

    final class EdgeWithCost {
        public EdgeWithCost(DomainTransitionGraph.DTEdge e, int distCost, int directCost) {
            this.e = e; this.distanceCost = distCost; this.directCost = directCost;
        }
        public final DomainTransitionGraph.DTEdge e;
        public final int distanceCost;
        public final int directCost;

        @Override public String toString() { return distanceCost+" "+directCost; }
    }

    public Collection<EdgeWithCost> inEdges(DomainTransitionGraph.DTNode n, CostEvaluator ce) {
        List<EdgeWithCost> edges = new LinkedList<>();
        DomainTransitionGraph container = dtgs.get(n.containerID);
        Iterator<DomainTransitionGraph.DTEdge> it = container.inEdges(n);
        while(it.hasNext()) {
            DomainTransitionGraph.DTEdge e = it.next();
            edges.add(new EdgeWithCost(e, e.ga!=null ? ce.distTo(e.ga) : 0,  ce.cost(e.act, e.ga, e.sv())));
        }
        if(container instanceof GroundDTGs.DTG) {
            for (DomainTransitionGraph dtg : dtgs.values()) {
                if (dtg != container) {
                    DomainTransitionGraph.DTNode nNext = dtg.possibleEntryPointFrom(n);
                    if (nNext != null) {
                        int transitionCost = dtg.hasBeenExtended ? 999 : 0;
                        edges.add(new EdgeWithCost(new DomainTransitionGraph.DTEdge(nNext, n, null, null), 0, transitionCost));
                    }
                }
            }
        }
        return edges;
    }


    public PartialPathDTG bestPath(CostEvaluator ce) throws NoSolutionException {
        Queue<NodeCost> q = new PriorityQueue<>();
        Map<DomainTransitionGraph.DTNode, NodeCost> costs = new HashMap<>();
        for (DomainTransitionGraph.DTNode n : startNodes) {
            int baseCost;
            if(dtgs.get(n.containerID).hasBeenExtended)
                baseCost = 999;
            else
                baseCost = 0;
            NodeCost nc = new NodeCost(n, 0, baseCost, null, null);
            costs.put(n, nc);
            q.add(nc);
        }

        DomainTransitionGraph.DTNode dest = null;
        while (dest == null && !q.isEmpty()) {

            NodeCost nc = q.poll();
            DomainTransitionGraph current = dtgs.get(nc.n.containerID);

            if (current.isAccepting(nc.n)) {
                dest = nc.n;
            } else {
                Iterator<DomainTransitionGraph.DTEdge> it = current.inEdges(nc.n);
                while(it.hasNext()) {
                    DomainTransitionGraph.DTEdge e = it.next();

                    int accCost;
                    int extCost;
                    if(current.isFree(e)) {
                        accCost = nc.accumulatedCost;
                        extCost = nc.externalCost;
                    } else {
                        accCost = nc.accumulatedCost + ce.cost(e.act, e.ga, e.sv());
                        int actionCost = e.ga != null ? ce.distTo(e.ga) : 0;
                        extCost = (actionCost > nc.externalCost) ? actionCost : nc.externalCost;
                    }
                    NodeCost ncNext = new NodeCost(e.from, extCost, accCost, nc, e);

                    if(!costs.containsKey(ncNext.n)) { // no existing label, put it in queue whatever
                        costs.put(ncNext.n, ncNext);
                        q.add(ncNext);
                    } else if(costs.get(ncNext.n).cost() > ncNext.cost()) { // existing label, update if better
                        q.remove(costs.get(ncNext.n));
                        costs.put(ncNext.n, ncNext);
                        q.add(ncNext);
                    }
                }
                if(current instanceof GroundDTGs.DTG) {
                    for (DomainTransitionGraph dtg : dtgs.values()) {
                        if (dtg != current) {
                            DomainTransitionGraph.DTNode nNext = dtg.possibleEntryPointFrom(nc.n);
                            if (nNext != null) {
                                // give a very high cost to any path going through an already extended path.
                                int transitionCost = dtg.hasBeenExtended ? 999 : 0;
                                NodeCost ncNext = new NodeCost(nNext, nc.externalCost, nc.accumulatedCost + transitionCost, nc, null);
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
        }

        if(dest == null) {
//            print("no-path.dot", ce);
//            throw new FAPEException("aaaaaaaaaaaaaaaaaaa");
//            if(RelaxedPlanExtractor.debugging2)
//                System.out.println("qsdqsdqksl");
            throw new NoSolutionException();
        }
//            throw new FAPEException("NO SOLUTION! !!!!");

        NodeCost cur = costs.get(dest);
        assert startTimePoint != null;
        DomainTransitionGraph containingDTG = dtgs.get(cur.n.containerID);

        PartialPathDTG partialPathDTG = new PartialPathDTG(extractEdgeSequence(cur), containingDTG);
        if(partialPathDTG.id == 227) {
            print("coucou.dot", ce);
            System.out.println("dqsdqsd");
        }
        return partialPathDTG;
    }

    private List<DomainTransitionGraph.DTEdge> extractEdgeSequence(NodeCost nc) {
        List<DomainTransitionGraph.DTEdge> edges = new LinkedList<>();
        NodeCost cur = nc;
        while(cur != null) {
            if(cur.e == null && cur.pred != null) {
                edges.add(new DomainTransitionGraph.DTEdge(cur.n, cur.pred.n, null, null));
            } else if(cur.e != null){
                edges.add(cur.e);
            }
            cur = cur.pred;
        }
        return edges;
    }

    public void print(String filename, CostEvaluator ce) {
        MultiLabeledDigraph<DomainTransitionGraph.DTNode, EdgeWithCost> fullGraph = GraphFactory.getMultiLabeledDigraph();

        for(DomainTransitionGraph dtg : dtgs.values()) {
            for(DomainTransitionGraph.DTNode n : dtg.getAllNodes()) {
                if(!fullGraph.contains(n))
                    fullGraph.addVertex(n);
                for(EdgeWithCost e : inEdges(n, ce)) {
                    if(!fullGraph.contains(e.e.from))
                        fullGraph.addVertex(e.e.from);
                    assert n.equals(e.e.to);
                    fullGraph.addEdge(e.e.from, e.e.to, e);
                }
            }
        }
        fullGraph.exportToDotFile(filename, new NodeEdgePrinterInterface<DomainTransitionGraph.DTNode, EdgeWithCost, LabeledEdge<DomainTransitionGraph.DTNode, EdgeWithCost>>() {

            @Override public String printNode(DomainTransitionGraph.DTNode node) {
                return startNodes.contains(node) ? "start - "+node : node.toString();
            }

            @Override public String printEdge(EdgeWithCost edge) { return edge.toString(); }
            @Override public boolean excludeNode(DomainTransitionGraph.DTNode node) { return false; }
            @Override public boolean excludeEdge(LabeledEdge<DomainTransitionGraph.DTNode, EdgeWithCost> edge) { return false; }
        });
    }

    /*
    public class TransitionSequence {
        public Timeline supporter = null;
        public PathDTG extendedSolution;
        public List<NodeCost> nodes = new LinkedList<>();
        private List<DomainTransitionGraph.DTEdge> edges = null;

        public List<GAction> getActionsSequence() {
            return edges().stream()
                    .filter(e -> e != null && e.ga != null)
                    .map(e -> e.ga)
                    .collect(Collectors.toList());
//            List<GAction> solution = new LinkedList<>();
//            for(NodeCost nc : nodes) {
//                if(nc.e != null && nc.e.ga != null) {
//                    solution.add(nc.e.ga);
//                }
//            }
//            return solution;
        }

        public List<DomainTransitionGraph.DTEdge> edges() {
            if(edges == null) {
                edges = new LinkedList<>();
                for(NodeCost nc : nodes) {
                    if(nc.e == null && nc.pred != null) {
                        edges.add(new DomainTransitionGraph.DTEdge(nc.n, nc.pred.n, null, null));
                    } else {
                        edges.add(nc.e);
                    }
                }
            }
            return edges;
        }

        public PathDTG getDTG() {
            assert startTimePoint != null;
            PathDTG dtg = new PathDTG(supporter, extendedSolution, startTimePoint);
            boolean atLEastOneEdge = false;
            for(NodeCost nc : nodes) {
                if(nc.e != null) {
                    dtg.addNextEdge(nc.e.from, nc.e.to, nc.e.ga, nc.e.act);
                    atLEastOneEdge = true;
                }
            }
            int idOfExtended = nodes.get(0).n.containerID;
            DomainTransitionGraph base = dtgs.get(idOfExtended);
            if(dtg.isAcceptableSupporterForTransitions() && !(base instanceof GroundDTGs.DTG))
                base.hasBeenExtended = true;
            assert atLEastOneEdge;
            return dtg;
        }
    }
    */
}
